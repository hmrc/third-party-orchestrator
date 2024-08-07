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

package uk.gov.hmrc.thirdpartyorchestrator.services

import scala.concurrent.ExecutionContext.Implicits.global

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.tpd.session.domain.models.{LoggedInState, UserSession, UserSessionId}
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker
import uk.gov.hmrc.thirdpartyorchestrator.mocks.connectors.ThirdPartyDeveloperConnectorMockModule
import uk.gov.hmrc.thirdpartyorchestrator.utils.AsyncHmrcSpec

class SessionServiceSpec extends AsyncHmrcSpec {

  trait Setup extends ThirdPartyDeveloperConnectorMockModule with UserBuilder
      with LocalUserIdTracker {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val underTest = new SessionService(ThirdPartyDeveloperConnectorMock.aMock)

    val email     = "thirdpartydeveloper@example.com".toLaxEmail
    val userId    = UserId.random
    val sessionId = UserSessionId.random
    val session   = UserSession(sessionId, LoggedInState.LOGGED_IN, buildUser(email, "Bob", "Fleming").copy(userId = userId))
  }

  "fetchSession" should {
    "return the session when it exists" in new Setup {
      ThirdPartyDeveloperConnectorMock.FetchSession.thenReturn(sessionId)(Some(session))
      await(underTest.fetch(sessionId)) shouldBe Some(session)
    }

    "return None when its does not exist" in new Setup {
      ThirdPartyDeveloperConnectorMock.FetchSession.thenReturn(sessionId)(None)
      await(underTest.fetch(sessionId)) shouldBe None
    }
  }
}
