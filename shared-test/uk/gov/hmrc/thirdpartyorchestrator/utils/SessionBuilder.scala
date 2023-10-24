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

package uk.gov.hmrc.thirdpartyorchestrator.utils

import java.time.LocalDateTime

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.thirdpartyorchestrator.domain.models._

trait SessionBuilder {

  def buildSession(sessionId: SessionId, userId: UserId, authMfaId: MfaId, smsMfaId: MfaId, firstName: String, lastName: String, userEmail: LaxEmailAddress): Session = {
    Session(
      sessionId,
      LoggedInState.LOGGED_IN,
      buildDeveloper(userId, userEmail, firstName, lastName, authMfaId, smsMfaId)
    )
  }

  def buildDeveloper(
      userId: UserId,
      emailAddress: LaxEmailAddress,
      firstName: String,
      lastName: String,
      authMfaId: MfaId,
      smsMfaId: MfaId
    ): User = {
    User(
      emailAddress,
      firstName,
      lastName,
      LocalDateTime.parse("2022-12-23T12:24:31.123"),
      LocalDateTime.parse("2023-10-23T12:24:32.543"),
      true,
      Some(AccountSetup(List("ROLE1"), None, List("SERVICE1"), None, List("TARGET1"), None, false)),
      Some("Example Corp"),
      true,
      List(
        AuthenticatorAppMfaDetailSummary(
          authMfaId,
          "Petes App",
          LocalDateTime.parse("2022-12-28T11:21:31.123"),
          true
        ),
        SmsMfaDetailSummary(
          smsMfaId,
          "Petes Phone",
          LocalDateTime.parse("2023-01-21T11:21:31.123"),
          "07123456789",
          true
        )
      ),
      Some("2435364598347653405635324543645634575"),
      EmailPreferences(List(TaxRegimeInterests("REGIME1", Set("SERVICE1", "SERVICE2"))), Set(EmailTopic.TECHNICAL)),
      userId
    )
  }

  def buildMinimalSession(sessionId: SessionId, userId: UserId, firstName: String, lastName: String, userEmail: LaxEmailAddress): Session = {
    Session(
      sessionId,
      LoggedInState.LOGGED_IN,
      buildMinimalDeveloper(userId, userEmail, firstName, lastName)
    )
  }

  def buildMinimalDeveloper(
      userId: UserId,
      emailAddress: LaxEmailAddress,
      firstName: String,
      lastName: String
    ): User = {
    User(
      emailAddress,
      firstName,
      lastName,
      LocalDateTime.parse("2022-12-23T12:24:31.123"),
      LocalDateTime.parse("2023-10-23T12:24:32.543"),
      true,
      None,
      None,
      false,
      List.empty,
      None,
      EmailPreferences.noPreferences,
      userId
    )
  }
}
