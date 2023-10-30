/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.thirdpartyorchestrator.domain.models.developers.{Developer, DeveloperResponse}
import uk.gov.hmrc.thirdpartyorchestrator.services.ApplicationService

@Singleton()
class ApplicationController @Inject() (
    applicationService: ApplicationService,
    cc: ControllerComponents
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc) with JsonUtils {

  def getVerifiedCollaboratorsForApplication(applicationId: ApplicationId): Action[AnyContent] = Action.async { implicit request =>
    lazy val failed = (msg: String) => NotFound(msg)
    val success     = (developers: Set[Developer]) => Ok(Json.toJson(DeveloperResponse.from(developers)))
    applicationService.fetchVerifiedCollaboratorsForApplication(applicationId).map(_.fold(failed, success))
  }
}
