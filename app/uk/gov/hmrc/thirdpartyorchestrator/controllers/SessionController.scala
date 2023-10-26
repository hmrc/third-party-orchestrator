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
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.thirdpartyorchestrator.domain.models.developers.{SessionId, SessionResponse}
import uk.gov.hmrc.thirdpartyorchestrator.services.SessionService

object SessionController {
  case class SessionRequest(sessionId: String)
  implicit val formatSession = Json.format[SessionRequest]
}

@Singleton()
class SessionController @Inject() (
    sessionService: SessionService,
    cc: ControllerComponents
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc) with JsonUtils {

  import SessionController._

  def getDeveloperForSession(): Action[AnyContent] = Action.async { implicit request =>
    withJsonBodyFromAnyContent[SessionRequest] { sessionRequest =>
      Try(SessionId.unsafeApply(sessionRequest.sessionId)) match {
        case Failure(_)         => Future.successful(BadRequest("Invalid session id"))
        case Success(sessionId) => sessionService.fetch(sessionId).map { maybeSession =>
            maybeSession match {
              case Some(session) => Ok(Json.toJson(SessionResponse.from(session)))
              case _             => NotFound("Unknown session id")
            }
          }
      }
    }
  }
}
