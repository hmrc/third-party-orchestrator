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

import scala.concurrent.Future.{failed, successful}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationResponse
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, UserId}
import uk.gov.hmrc.thirdpartyorchestrator.connectors.{EnvironmentAwareThirdPartyApplicationConnector, PrincipalThirdPartyApplicationConnector, SubordinateThirdPartyApplicationConnector, ThirdPartyApplicationConnector}

trait ThirdPartyApplicationConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait AbstractThirdPartyApplicationMock {
    def aMock: ThirdPartyApplicationConnector

    object FetchApplicationById {

      def thenReturn(applicationId: ApplicationId)(application: Option[ApplicationResponse]) =
        when(aMock.fetchApplication(eqTo(applicationId))(*)).thenReturn(successful(application))

      def thenReturnNone(applicationId: ApplicationId) =
        when(aMock.fetchApplication(eqTo(applicationId))(*)).thenReturn(successful(None))

      def thenThrowException(applicationId: ApplicationId)(exception: Exception) =
        when(aMock.fetchApplication(eqTo(applicationId))(*)).thenReturn(failed(exception))
    }

    object FetchApplicationByClientId {

      def thenReturn(clientId: ClientId)(application: Option[ApplicationResponse]) =
        when(aMock.fetchApplication(eqTo(clientId))(*)).thenReturn(successful(application))

      def thenReturnNone(clientId: ClientId) =
        when(aMock.fetchApplication(eqTo(clientId))(*)).thenReturn(successful(None))

      def thenThrowException(clientId: ClientId)(exception: Exception) =
        when(aMock.fetchApplication(eqTo(clientId))(*)).thenReturn(failed(exception))
    }

    object FetchApplicationsByUserIds {

      def thenReturn(userIds: List[UserId])(applications: List[ApplicationResponse]) =
        when(aMock.fetchApplicationsByUserIds(eqTo(userIds))(*)).thenReturn(successful(applications))

      def thenReturnEmptyList(userIds: List[UserId]) =
        when(aMock.fetchApplicationsByUserIds(eqTo(userIds))(*)).thenReturn(successful(List.empty))

      def thenThrowException(userIds: List[UserId])(exception: Exception) =
        when(aMock.fetchApplicationsByUserIds(eqTo(userIds))(*)).thenReturn(failed(exception))
    }
  }

  object ThirdPartyApplicationConnectorMock extends AbstractThirdPartyApplicationMock {
    val aMock = mock[ThirdPartyApplicationConnector]
  }

  object SubordinateThirdPartyApplicationConnectorMock extends AbstractThirdPartyApplicationMock {
    override val aMock: ThirdPartyApplicationConnector = mock[SubordinateThirdPartyApplicationConnector]
  }

  object PrincipalThirdPartyApplicationConnectorMock extends AbstractThirdPartyApplicationMock {
    override val aMock: PrincipalThirdPartyApplicationConnector = mock[PrincipalThirdPartyApplicationConnector]
  }

  object EnvironmentAwareThirdPartyApplicationConnectorMock {
    private val subordinateConnector = SubordinateThirdPartyApplicationConnectorMock
    private val principalConnector   = PrincipalThirdPartyApplicationConnectorMock

    lazy val instance = {
      new EnvironmentAwareThirdPartyApplicationConnector(subordinateConnector.aMock, principalConnector.aMock)
    }

    lazy val Principal   = principalConnector
    lazy val Subordinate = subordinateConnector
  }
}
