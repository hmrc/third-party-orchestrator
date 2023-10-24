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

package uk.gov.hmrc.thirdpartyorchestrator.domain.models

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}

case class Session(sessionId: SessionId, loggedInState: LoggedInState, developer: User)

object Session {
  implicit val formatSession = Json.format[Session]
}

case class SessionRequest(sessionId: String)

object SessionRequest {
  implicit val formatSession = Json.format[SessionRequest]
}

case class SessionResponse(
    userId: UserId,
    email: LaxEmailAddress,
    firstName: String,
    lastName: String,
    organisation: Option[String] = None
  )

object SessionResponse {

  def from(session: Session) =
    SessionResponse(
      session.developer.userId,
      session.developer.email,
      session.developer.firstName,
      session.developer.lastName,
      session.developer.organisation
    )

  implicit val format = Json.format[SessionResponse]
}
