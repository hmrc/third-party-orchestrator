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

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.developers.domain.models._

trait DeveloperBuilder {

  def buildSession(sessionId: SessionId, userId: UserId, firstName: String, lastName: String, userEmail: LaxEmailAddress): Session = {
    Session(
      sessionId,
      LoggedInState.LOGGED_IN,
      buildDeveloper(userId, userEmail, firstName, lastName)
    )
  }

  def buildDeveloper(
      userId: UserId,
      emailAddress: LaxEmailAddress,
      firstName: String,
      lastName: String
    ): Developer = {
    Developer(
      emailAddress,
      firstName,
      lastName,
      true,
      userId
    )
  }
}
