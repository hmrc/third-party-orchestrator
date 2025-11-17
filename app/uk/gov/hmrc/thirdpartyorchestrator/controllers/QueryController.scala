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
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

import org.apache.pekko.util.ByteString

import play.api.http.ContentTypes
import play.api.http.HttpEntity.Strict
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, ResponseHeader, Result}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.http.UpstreamErrorResponse

import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ParamNames
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.thirdpartyorchestrator.config.AppConfig
import uk.gov.hmrc.thirdpartyorchestrator.connectors.EnvironmentAwareQueryConnector
import uk.gov.hmrc.thirdpartyorchestrator.utils.ApplicationLogger
import scala.concurrent.Future
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.QueriedApplication
import uk.gov.hmrc.http.HeaderCarrier
import cats.data.EitherT
import uk.gov.hmrc.http.HttpReads

@Singleton()
class QueryController @Inject() (
    queryConnector: EnvironmentAwareQueryConnector,
    appConfig: AppConfig,
    cc: ControllerComponents
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc) with JsonUtils with ApplicationLogger {

  private def getParam(queryMap: Map[String, Seq[String]])(paramName: String): Option[String] = {
    queryMap.get(paramName).flatMap(_.headOption)
  }

  private def hasParameter(params: Map[String, Seq[String]])(paramName: String) = {
    getParam(params)(paramName).isDefined
  }

  private def hasEnvParameter(params: Map[String, Seq[String]]) = hasParameter(params)(ParamNames.Environment)

  def queryEnv(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
    queryEnv(environment, request.queryString)
  }

  private def queryEnv(environment: Environment, params: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[Result] = {
    //
    // Check there isn't an environment query param
    // And add one if we're not in a bridged deployment
    //
    if (hasEnvParameter(params)) {
      successful(BadRequest(Json.toJson(JsErrorResponse("UNEXPECTED_PARAMETER", "Cannot provide an environment query parameter when using environment path parameter"))))
    } else {
      val effectiveParams: Map[String, Seq[String]] =
        if (appConfig.inPairedEnvironment) {
          params
        } else {
          params + (ParamNames.Environment -> Seq(s"$environment"))
        }
      queryConnector(environment).query[HttpResponse](effectiveParams).map(convertToResult)
    }
  }

  private def asBody(errorCode: String, message: Json.JsValueWrapper): JsObject =
    Json.obj(
      "code"    -> errorCode.toString,
      "message" -> message
    )

  private val applicationNotFound = NotFound(asBody("APPLICATION_NOT_FOUND", "No application found for query"))

  implicit def readEitherOfOption[A : HttpReads]: HttpReads[Either[HttpResponse, Option[A]]] = 
     HttpReads[HttpResponse]
      .flatMap(x => x.status match {
        case 200 | 201 => HttpReads[A].map(y => Right(Some(y))) // this delegates error handling to HttpReads[A]
        case 404 => HttpReads.pure(Right(None))
        case 400 => HttpReads[HttpResponse].map(Left.apply)
      })

  implicit def readEitherOfL[A : HttpReads]: HttpReads[Either[HttpResponse, A]] = 
     HttpReads[HttpResponse]
      .flatMap(x => x.status match {
        case 200 | 201 => HttpReads[A].map(y => Right(y)) // this delegates error handling to HttpReads[A]
        case 400 => HttpReads[HttpResponse].map(Left.apply)
      })

  private def queryBothEnvironments(params: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[Result] = {
    def hasParam: String => Boolean = hasParameter(params) _
    def isPaginatedQuery: Boolean = hasParam(ParamNames.PageNbr) || hasParam(ParamNames.PageSize)
    def isSingleAppQuery: Boolean = hasParam(ParamNames.ApplicationId) || hasParam(ParamNames.ClientId) || hasParam(ParamNames.ServerToken)

    def handleFailure(response: HttpResponse): Result = {
      logger.warn(s"Error occurred performing tpo query: ${response.status} ${response.body}")
      Result(ResponseHeader(response.status), Strict(ByteString(response.body), Some(ContentTypes.JSON)))
      .withHeaders(response.headers.toSeq.map(a => (a._1, a._2.head)): _*)
    }

    def handleOption(oapp: Option[QueriedApplication]): Result = {
      oapp.fold(applicationNotFound)(app => Ok(Json.toJson(app)))
    }

    def effectiveParams(environment: Environment): Map[String, Seq[String]] = if (appConfig.inPairedEnvironment) params else params + (ParamNames.Environment -> Seq(s"$environment"))

    if (isPaginatedQuery) {
      successful(BadRequest(Json.toJson(JsErrorResponse("UNEXPECTED_PARAMETER", "Cannot request paginated queries across both environments"))))
    } else if (isSingleAppQuery) {
      val etMaybeApp = for {
        principal <- EitherT(queryConnector.principal.query[Either[HttpResponse, Option[QueriedApplication]]](effectiveParams(Environment.PRODUCTION)))
        subordinate <- EitherT(queryConnector.subordinate.query[Either[HttpResponse, Option[QueriedApplication]]](effectiveParams(Environment.SANDBOX)))
      } yield principal.orElse(subordinate)

      etMaybeApp.fold(handleFailure, handleOption)

    } else { // not singleApp
      val etApps = for {
        principal <- EitherT(queryConnector.principal.query[Either[HttpResponse, List[QueriedApplication]]](effectiveParams(Environment.PRODUCTION)))
        subordinate <- EitherT(queryConnector.subordinate.query[Either[HttpResponse, List[QueriedApplication]]](effectiveParams(Environment.SANDBOX)))
      } yield principal ++ subordinate

      etApps.fold(handleFailure, apps => Ok(Json.toJson(apps)))
    }
  }

  def query(): Action[AnyContent] = Action.async { implicit request =>
    val envParam = getParam(request.queryString)(ParamNames.Environment)

    if (envParam.isDefined) {
      Environment(envParam.get).fold(
        successful(BadRequest(Json.toJson(JsErrorResponse("INVALID_QUERY", s"${envParam.get} is not a valid environment"))))
      )(
        env => queryEnv(env, request.queryString.-(ParamNames.Environment))
      )
    } else {
      queryBothEnvironments(request.queryString)
      .recover {
        case UpstreamErrorResponse(message, BAD_REQUEST, _, _) => 
          BadRequest(message).as(ContentTypes.JSON)
      }
    }
  }

  private def convertToResult(resp: HttpResponse): Result = {
    Result(ResponseHeader(resp.status), Strict(ByteString(resp.body), Some(ContentTypes.JSON)))
      .withHeaders(resp.headers.toSeq.map(a => (a._1, a._2.head)): _*)
  }    
}
