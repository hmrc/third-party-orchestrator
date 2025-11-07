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

import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.ApplicationNameValidationRequest
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment
import uk.gov.hmrc.thirdpartyorchestrator.connectors.EnvironmentAwareThirdPartyApplicationConnector
import uk.gov.hmrc.thirdpartyorchestrator.utils.ApplicationLogger

@Singleton()
class EnvironmentApplicationController @Inject() (
    connector: EnvironmentAwareThirdPartyApplicationConnector,
    cc: ControllerComponents
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc) with JsonUtils with ApplicationLogger with WarnStillInUse {

  def searchApplications(environment: Environment): Action[AnyContent] = warnStillInUse("searchApplications") {
    Action.async { implicit request =>
      connector(environment).searchApplications(request.queryString).map(response => Ok(Json.toJson(response)))
    }
  }

  def validateName(environment: Environment): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[ApplicationNameValidationRequest] { req =>
      connector(environment).validateName(req)
        .map(response => response.fold[Result](NotFound(JsNull))(r => Ok(Json.toJson(r))))
    }
  }
}
