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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, ResponseHeader, Result}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ParamNames
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.thirdpartyorchestrator.config.AppConfig
import uk.gov.hmrc.thirdpartyorchestrator.connectors.EnvironmentAwareQueryConnector
import uk.gov.hmrc.thirdpartyorchestrator.utils.ApplicationLogger

@Singleton()
class QueryController @Inject() (
    queryConnector: EnvironmentAwareQueryConnector,
    appConfig: AppConfig,
    cc: ControllerComponents
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc) with JsonUtils with ApplicationLogger {

  private def getEnvironmentParameter(queryMap: Map[String, Seq[String]]): Option[String] = {
    queryMap.get(ParamNames.Environment).flatMap(_.headOption)
  }

  def queryEnv(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
    //
    // Check there isn't an environment query param
    // And add one if we're not in a bridged deployment
    //
    if (getEnvironmentParameter(request.queryString).isDefined) {
      successful(BadRequest(Json.toJson(JsErrorResponse("UNEXPECTED_PARAMETER", "Cannot provide an environment query parameter when using environment path parameter"))))
    } else {
      val effectiveQueryMap: Map[String, Seq[String]] =
        if (appConfig.inPairedEnvironment) {
          request.queryString
        } else {
          request.queryString + (ParamNames.Environment -> Seq(s"$environment"))
        }
      queryConnector(environment).query(effectiveQueryMap).map(convertToResult)
    }
  }

  // def query(): Action[AnyContent] = Action.async { implicit request =>
  //   // 1 . If paginated query, we must have an environment (but we won't pass it down except in single environments (LOCAL/INT/STAGING))

  //   // 2. If there is an environment query param this is a simple pass through (but we need to remove the environment param except in single environments (LOCAL/INT/STAGING))

  //   // 3 .If there is no environment query param, should we query both environments and stitch the data together - for single app queries this is easier (slightly)
  //   // but we cannot do paginated query

  //   val envQuery: Option[String] = request.queryString.get("environment").flatMap(_.headOption)
  //   envQuery.map(Environment(_)) match {
  //     case None            => ??? // Stitch together
  //     case Some(None)      => successful(BadRequest(s"${envQuery.get} is not a valid environment"))
  //     case Some(Some(env)) => queryConnector(env).query(request.target.queryMap).map(convertToResult)
  //   }
  // }

  private def convertToResult(resp: HttpResponse) = {
    Result(ResponseHeader(resp.status), Strict(ByteString(resp.body), Some(ContentTypes.JSON)))
      .withHeaders(resp.headers.toSeq.map(a => (a._1, a._2.head)): _*)
  }
}
