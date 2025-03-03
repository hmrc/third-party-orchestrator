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

import play.api.http.Status
import uk.gov.hmrc.http.HttpResponse

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, PaginatedApplications}
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.CreateApplicationRequest
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, UserId}
import uk.gov.hmrc.thirdpartyorchestrator.connectors.{
  EnvironmentAwareThirdPartyApplicationConnector,
  PrincipalThirdPartyApplicationConnector,
  SubordinateThirdPartyApplicationConnector,
  ThirdPartyApplicationConnector
}

trait ThirdPartyApplicationConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait AbstractThirdPartyApplicationMock {
    def aMock: ThirdPartyApplicationConnector

    object FetchApplicationById {

      def thenReturn(applicationId: ApplicationId)(application: Option[ApplicationWithCollaborators]) =
        when(aMock.fetchApplication(eqTo(applicationId))(*)).thenReturn(successful(application))

      def thenReturnNone(applicationId: ApplicationId) =
        when(aMock.fetchApplication(eqTo(applicationId))(*)).thenReturn(successful(None))

      def thenThrowException(applicationId: ApplicationId)(exception: Exception) =
        when(aMock.fetchApplication(eqTo(applicationId))(*)).thenReturn(failed(exception))
    }

    object FetchApplicationByClientId {

      def thenReturn(clientId: ClientId)(application: Option[ApplicationWithCollaborators]) =
        when(aMock.fetchApplication(eqTo(clientId))(*)).thenReturn(successful(application))

      def thenReturnNone(clientId: ClientId) =
        when(aMock.fetchApplication(eqTo(clientId))(*)).thenReturn(successful(None))

      def thenThrowException(clientId: ClientId)(exception: Exception) =
        when(aMock.fetchApplication(eqTo(clientId))(*)).thenReturn(failed(exception))
    }

    object FetchApplicationsByUserIds {

      def thenReturn(userIds: List[UserId])(applications: List[ApplicationWithCollaborators]) =
        when(aMock.fetchApplicationsByUserIds(eqTo(userIds))(*)).thenReturn(successful(applications))

      def thenReturnEmptyList(userIds: List[UserId]) =
        when(aMock.fetchApplicationsByUserIds(eqTo(userIds))(*)).thenReturn(successful(List.empty))

      def thenThrowException(userIds: List[UserId])(exception: Exception) =
        when(aMock.fetchApplicationsByUserIds(eqTo(userIds))(*)).thenReturn(failed(exception))
    }

    object SearchApplications {

      def thenReturns(applications: PaginatedApplications) = {
        when(aMock.searchApplications(*)(*)).thenReturn(successful(applications))
      }

      def thenThrowException(exception: Exception) =
        when(aMock.searchApplications(*)(*)).thenReturn(failed(exception))
    }

    object Create {

      def thenReturns(request: CreateApplicationRequest)(response: ApplicationWithCollaborators) = {
        when(aMock.create(eqTo(request))(*)).thenReturn(successful(response))
      }
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

    object VerifyUplift {

      def succeedsWith(verifyCode: String) = {
        when(aMock.verify(eqTo(verifyCode))(*)).thenReturn(successful(HttpResponse(Status.NO_CONTENT, "", Map.empty)))
      }

      def failsWithStatus(verifyCode: String, status: Int) = {
        when(aMock.verify(eqTo(verifyCode))(*)).thenReturn(successful(HttpResponse(status, "", Map.empty)))
      }
    }
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
