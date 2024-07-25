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

package uk.gov.hmrc.thirdpartyorchestrator.mocks.connectors

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.tpd.core.domain.models.User
import uk.gov.hmrc.apiplatform.modules.tpd.session.domain.models.{UserSession, UserSessionId}
import uk.gov.hmrc.thirdpartyorchestrator.connectors.ThirdPartyDeveloperConnector

trait ThirdPartyDeveloperConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait AbstractThirdPartyDeveloperMock {
    def aMock: ThirdPartyDeveloperConnector

    object FetchSession {

      def thenReturn(sessionId: UserSessionId)(session: Option[UserSession]) =
        when(aMock.fetchSession(eqTo(sessionId))(*)).thenReturn(successful(session))
    }

    object FetchDeveloper {

      def thenReturn(developerId: UserId)(developer: Option[User]) =
        when(aMock.fetchDeveloper(eqTo(developerId))(*)).thenReturn(successful(developer))
    }
  }

  object ThirdPartyDeveloperConnectorMock extends AbstractThirdPartyDeveloperMock {
    val aMock = mock[ThirdPartyDeveloperConnector]
  }
}
