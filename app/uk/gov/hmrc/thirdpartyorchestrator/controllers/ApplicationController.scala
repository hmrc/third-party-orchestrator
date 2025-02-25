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
import scala.concurrent.Future.successful

import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.tpd.core.domain.models.User
import uk.gov.hmrc.thirdpartyorchestrator.services.ApplicationService
import uk.gov.hmrc.thirdpartyorchestrator.utils.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.CreateApplicationRequestV2


case class ApplicationsByRequest(emails: List[LaxEmailAddress])

object ApplicationsByRequest {
  implicit val format: OFormat[ApplicationsByRequest] = Json.format[ApplicationsByRequest]
}

@Singleton()
class ApplicationController @Inject() (
    applicationService: ApplicationService,
    cc: ControllerComponents
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc) with JsonUtils with ApplicationLogger {

  def create() = Action.async(parse.json) { implicit request =>
    withJsonBody[CreateApplicationRequestV2] { createApplicationRequest =>
      applicationService.createSandboxApplication(createApplicationRequest)
      .map(app => Ok(Json.toJson(app)))
    }
  }

  def getApplication(applicationId: ApplicationId): Action[AnyContent] = Action.async { implicit request =>
    applicationService.fetchApplication(applicationId).map {
      case Some(response) => Ok(Json.toJson(response))
      case None           => NotFound
    }
  }

  def getApplicationsByEmail(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[ApplicationsByRequest] {
      emailsRequest =>
        applicationService.fetchApplicationsForEmails(emailsRequest.emails)
          .map(response => Ok(Json.toJson(response))) recover recovery
    }
  }

  def getApplicationsByCollaborator(userId: UserId): Action[AnyContent] = Action.async { implicit request =>
    applicationService.fetchApplicationsByUserIds(List(userId))
      .map(response => Ok(Json.toJson(response))) recover recovery
  }

  def queryDispatcher(): Action[AnyContent] = Action.async { implicit request =>
    val queryBy = request.queryString.keys.toList.sorted
    queryBy match {
      case ("clientId" :: _) =>
        val clientId = ClientId(request.queryString("clientId").head)
        applicationService.fetchApplication(clientId).map {
          case Some(response) => Ok(Json.toJson(response))
          case None           => NotFound
        }
      case _                 =>
        successful(BadRequest("Unknown query parameters"))
    }
  }

  def getVerifiedDevelopersForApplication(applicationId: ApplicationId): Action[AnyContent] = Action.async { implicit request =>
    lazy val failed = (msg: String) => NotFound(msg)
    val success     = (users: Set[User]) => Ok(Json.toJson(users))
    applicationService.fetchVerifiedCollaboratorsForApplication(applicationId).map(_.fold(failed, success))
  }

  def recovery: PartialFunction[Throwable, Result] = {
    case e: Throwable =>
      logger.error(s"Error occurred: ${e.getMessage}", e)
      handleException(e)
  }

  private[controllers] def handleException(e: Throwable) = {
    logger.error(s"An unexpected error occurred: ${e.getMessage}", e)
    InternalServerError(Json.obj(
      "code"    -> "UNKNOWN_ERROR",
      "message" -> "Unknown error occurred"
    ))
  }
}
