/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.thirdpartyorchestrator.controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

import cats.data.EitherT
import org.apache.pekko.util.ByteString

import play.api.http.ContentTypes
import play.api.http.HttpEntity.Strict
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, ResponseHeader, Result}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.QueriedApplication
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ParamNames
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.thirdpartyorchestrator.config.AppConfig
import uk.gov.hmrc.thirdpartyorchestrator.connectors.{EnvironmentAwareQueryConnector, ReadEitherWithNoException}
import uk.gov.hmrc.thirdpartyorchestrator.utils.ApplicationLogger

@Singleton()
class QueryController @Inject() (
    queryConnector: EnvironmentAwareQueryConnector,
    appConfig: AppConfig,
    cc: ControllerComponents
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc) with JsonUtils with ApplicationLogger with ReadEitherWithNoException {

  def queryEnv(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
    queryEnv(environment, request.queryString)
  }

  private def buildEffectiveParams(inPairedEnvironment: Boolean, params: Map[String, Seq[String]], environment: Environment): Map[String, Seq[String]] =
    if (inPairedEnvironment)
      params
    else
      params + (ParamNames.Environment -> Seq(s"$environment"))

  private def queryEnv(environment: Environment, params: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[Result] = {
    /*
     * Check there isn't an environment query param
     * And add one if we're not in a bridged deployment
     */
    if (hasEnvParameter(params)) {
      successful(BadRequest(Json.toJson(JsErrorResponse("UNEXPECTED_PARAMETER", "Cannot provide an environment query parameter when using environment path parameter"))))
    } else {
      queryConnector(environment).query[HttpResponse](buildEffectiveParams(appConfig.inPairedEnvironment, params, environment)).map(convertToResult)
    }
  }

  private def recoverWithDefault[T](default: T): PartialFunction[Throwable, T] = {
    case NonFatal(e) =>
      logger.warn(s"Error occurred fetching application: ${e.getMessage}", e)
      default
  }

  private def queryBothEnvironments(params: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[Result] = {
    def hasParam: String => Boolean = hasParameter(params) _
    def isPaginatedQuery: Boolean   = hasParam(ParamNames.PageNbr) || hasParam(ParamNames.PageSize)
    def isSingleAppQuery: Boolean   = hasParam(ParamNames.ApplicationId) || hasParam(ParamNames.ClientId) || hasParam(ParamNames.ServerToken)

    def handleFailure(response: HttpResponse): Result = {
      logger.warn(s"Error occurred in Third Party Orchestrater calling one query endpoint: ${response.status} ${response.body}")
      Result(ResponseHeader(response.status), Strict(ByteString(response.body), Some(ContentTypes.JSON)))
        .withHeaders(response.headers.toSeq.map(a => (a._1, a._2.head)): _*)
    }

    def handleOption(oapp: Option[QueriedApplication]): Result = {
      oapp.fold(applicationNotFound)(app => Ok(Json.toJson(app)))
    }

    if (isPaginatedQuery) {
      successful(BadRequest(Json.toJson(JsErrorResponse("UNEXPECTED_PARAMETER", "Cannot request paginated queries across both environments"))))

    } else if (isSingleAppQuery) {
      EitherT(queryConnector.principal.query[Either[HttpResponse, Option[QueriedApplication]]](buildEffectiveParams(appConfig.inPairedEnvironment, params, Environment.PRODUCTION)))
        .flatMap {
          _.fold(
            EitherT(queryConnector.subordinate.query[Either[HttpResponse, Option[QueriedApplication]]](buildEffectiveParams(
              appConfig.inPairedEnvironment,
              params,
              Environment.SANDBOX
            )))
          )(app => EitherT.rightT(Some(app)))
        }
        .fold(handleFailure, handleOption)

    } else { // not singleApp
      val principalET   =
        EitherT(queryConnector.principal.query[Either[HttpResponse, List[QueriedApplication]]](buildEffectiveParams(appConfig.inPairedEnvironment, params, Environment.PRODUCTION)))
      val subordinateET =
        EitherT(queryConnector.subordinate.query[Either[HttpResponse, List[QueriedApplication]]](
          buildEffectiveParams(appConfig.inPairedEnvironment, params, Environment.SANDBOX)
        ) recover recoverWithDefault(Right(Nil)))

      val appsET = for {
        principalApps   <- principalET
        subordinateApps <- subordinateET
      } yield principalApps ++ subordinateApps

      appsET.fold(handleFailure, apps => Ok(Json.toJson(apps)))
    }
  }

  def query(): Action[AnyContent] = Action.async { implicit request =>
    val envParam = getParam(request.queryString)(ParamNames.Environment)

    if (envParam.isDefined) {
      Environment(envParam.get).fold(
        successful(BadRequest(Json.toJson(JsErrorResponse("INVALID_QUERY", s"${envParam.get} is not a valid environment"))))
      )(env => queryEnv(env, request.queryString.-(ParamNames.Environment)))
    } else {
      queryBothEnvironments(request.queryString)
    }
  }

  private def convertToResult(resp: HttpResponse): Result = {
    Result(ResponseHeader(resp.status), Strict(ByteString(resp.body), Some(ContentTypes.JSON)))
      .withHeaders(resp.headers.toSeq.map(a => (a._1, a._2.head)): _*)
  }

  private def getParam(queryMap: Map[String, Seq[String]])(paramName: String): Option[String] = {
    queryMap.get(paramName).flatMap(_.headOption)
  }

  private def hasParameter(params: Map[String, Seq[String]])(paramName: String) = {
    getParam(params)(paramName).isDefined
  }

  private def hasEnvParameter(params: Map[String, Seq[String]]) = hasParameter(params)(ParamNames.Environment)

  private def asBody(errorCode: String, message: Json.JsValueWrapper): JsObject =
    Json.obj(
      "code"    -> errorCode.toString,
      "message" -> message
    )

  private val applicationNotFound = NotFound(asBody("APPLICATION_NOT_FOUND", "No application found for query"))
}
