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

package uk.gov.hmrc.thirdpartyorchestrator.mocks.services

import scala.concurrent.Future.{failed, successful}

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.GetAppsForAdminOrRIRequest
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, UserId}
import uk.gov.hmrc.thirdpartyorchestrator.services.ApplicationFetcher

trait ApplicationFetcherMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait AbstractApplicationFetcherMock {
    def aMock: ApplicationFetcher

    object FetchApplication {

      def thenReturn(applicationId: ApplicationId)(application: Option[ApplicationWithCollaborators]) =
        when(aMock.fetchApplication(eqTo(applicationId))(*)).thenReturn(successful(application))
    }

    object FetchApplicationsByUserId {

      def thenReturn(userIds: List[UserId])(applications: List[ApplicationWithCollaborators]) =
        when(aMock.fetchApplicationsByUserIds(eqTo(userIds))(*)).thenReturn(successful(applications))
    }

    object GetAppsForResponsibleIndividualOrAdmin {

      def thenReturn(request: GetAppsForAdminOrRIRequest)(applications: List[ApplicationWithCollaborators]) =
        when(aMock.getAppsForResponsibleIndividualOrAdmin(eqTo(request))(*)).thenReturn(successful(applications))

      def thenReturnEmptyList(request: GetAppsForAdminOrRIRequest) =
        when(aMock.getAppsForResponsibleIndividualOrAdmin(eqTo(request))(*)).thenReturn(successful(List.empty))

      def thenThrowException(request: GetAppsForAdminOrRIRequest)(exception: Exception) =
        when(aMock.getAppsForResponsibleIndividualOrAdmin(eqTo(request))(*)).thenReturn(failed(exception))
    }

    object FetchApplicationByClientId {

      def thenReturn(clientId: ClientId)(application: Option[ApplicationWithCollaborators]) =
        when(aMock.fetchApplication(eqTo(clientId))(*)).thenReturn(successful(application))
    }
  }

  object ApplicationFetcherMock extends AbstractApplicationFetcherMock {
    val aMock = mock[ApplicationFetcher]
  }
}
