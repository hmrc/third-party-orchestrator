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
import uk.gov.hmrc.http.HttpReads.Implicits._
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

  private def getParam(queryMap: Map[String, Seq[String]])(paramName: String): Option[String] = {
    queryMap.get(paramName).flatMap(_.headOption)
  }

  private def hasEnvParameter(params: Map[String, Seq[String]]) = {
    getParam(params)(ParamNames.Environment).isDefined
  }

  def queryEnv(environment: Environment): Action[AnyContent] = Action.async { implicit request =>
    //
    // Check there isn't an environment query param
    // And add one if we're not in a bridged deployment
    //
    if (hasEnvParameter(request.queryString)) {
      successful(BadRequest(Json.toJson(JsErrorResponse("UNEXPECTED_PARAMETER", "Cannot provide an environment query parameter when using environment path parameter"))))
    } else {
      val effectiveQueryMap: Map[String, Seq[String]] =
        if (appConfig.inPairedEnvironment) {
          request.queryString
        } else {
          request.queryString + (ParamNames.Environment -> Seq(s"$environment"))
        }
      queryConnector(environment).query[HttpResponse](effectiveQueryMap).map(convertToResult)
    }
  }

  private def convertToResult(resp: HttpResponse): Result = {
    Result(ResponseHeader(resp.status), Strict(ByteString(resp.body), Some(ContentTypes.JSON)))
      .withHeaders(resp.headers.toSeq.map(a => (a._1, a._2.head)): _*)
  }
}
