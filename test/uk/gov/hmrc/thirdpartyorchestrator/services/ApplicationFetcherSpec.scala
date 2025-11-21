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

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, ApplicationWithCollaboratorsFixtures}
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.GetAppsForAdminOrRIRequest
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.Param.{ExcludeDeletedQP, UserIdsQP}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, LaxEmailAddress, UserId}
import uk.gov.hmrc.thirdpartyorchestrator.mocks.connectors._
import uk.gov.hmrc.thirdpartyorchestrator.utils.AsyncHmrcSpec

class ApplicationFetcherSpec extends AsyncHmrcSpec with ApplicationWithCollaboratorsFixtures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup extends ThirdPartyApplicationConnectorMockModule with QueryConnectorMockModule with MockitoSugar
      with ArgumentMatchersSugar {

    val userIds: List[UserId]                      = List(userIdOne, userIdTwo)
    val application: ApplicationWithCollaborators  = standardApp
    val clientId: ClientId                         = application.clientId
    val applicationId: ApplicationId               = application.id
    val application2: ApplicationWithCollaborators = standardApp.withId(applicationIdTwo)
    val exception                                  = new RuntimeException("error")

    val fetcher = new ApplicationFetcher(
      EnvironmentAwareThirdPartyApplicationConnectorMock.instance,
      EnvironmentAwareQueryConnectorMock.instance
    )

    val appIdQuery    = ApplicationQuery.ById(applicationId, Nil, false)
    val clientIdQuery = ApplicationQuery.ByClientId(clientId, false, Nil, false)
    val userIdQuery   = ApplicationQuery.GeneralOpenEndedApplicationQuery(List(UserIdsQP(userIds), ExcludeDeletedQP))
  }

  "ApplicationFetcher" when {
    "fetchApplication is called" should {
      "return None if absent from principal and subordinate" in new Setup {
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](appIdQuery, None)
        EnvironmentAwareQueryConnectorMock.Subordinate.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](appIdQuery, None)

        await(fetcher.fetchApplication(applicationId)) shouldBe None
      }

      "return an application from subordinate if present" in new Setup {
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](appIdQuery, None)
        EnvironmentAwareQueryConnectorMock.Subordinate.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](appIdQuery, Some(application))

        await(fetcher.fetchApplication(applicationId)) shouldBe Some(application)
      }

      "return an application from principal and dont then call subordinate" in new Setup {
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](appIdQuery, Some(application))

        await(fetcher.fetchApplication(applicationId)) shouldBe Some(application)
      }

      "return None if absent in principal and subordinate throws" in new Setup {
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](appIdQuery, None)
        EnvironmentAwareQueryConnectorMock.Subordinate.ByQuery.failsFor[Option[ApplicationWithCollaborators]](appIdQuery, exception)

        await(fetcher.fetchApplication(applicationId)) shouldBe None
      }

      "return an exception if principal throws regardless of subordinate" in new Setup {
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.failsFor[Option[ApplicationWithCollaborators]](appIdQuery, exception)

        intercept[Exception] {
          await(fetcher.fetchApplication(applicationId))
        }.shouldBe(exception)
      }
    }

    "fetchApplicationByClientIdId is called" should {
      "return None if absent from principal and subordinate" in new Setup {
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](clientIdQuery, None)
        EnvironmentAwareQueryConnectorMock.Subordinate.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](clientIdQuery, None)

        await(fetcher.fetchApplication(clientId)) shouldBe None
      }

      "return an application from subordinate if present" in new Setup {
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](clientIdQuery, None)
        EnvironmentAwareQueryConnectorMock.Subordinate.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](clientIdQuery, Some(application))

        await(fetcher.fetchApplication(clientId)) shouldBe Some(application)
      }

      "return an application from principal and dont then call subordinate" in new Setup {
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](clientIdQuery, Some(application))

        await(fetcher.fetchApplication(clientId)) shouldBe Some(application)
      }

      "return None if absent in principal and subordinate throws" in new Setup {
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.returnsFor[Option[ApplicationWithCollaborators]](clientIdQuery, None)
        EnvironmentAwareQueryConnectorMock.Subordinate.ByQuery.failsFor[Option[ApplicationWithCollaborators]](clientIdQuery, exception)

        await(fetcher.fetchApplication(clientId)) shouldBe None
      }

      "return an exception if principal throws regardless of subordinate" in new Setup {
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.failsFor[Option[ApplicationWithCollaborators]](clientIdQuery, exception)

        intercept[Exception] {
          await(fetcher.fetchApplication(clientId))
        }.shouldBe(exception)
      }
    }

    "fetchApplicationsByUserIds is called" should {

      "return Empty List if given an empty list of user ids" in new Setup {
        await(fetcher.fetchApplicationsByUserIds(List.empty)) shouldBe List.empty
      }

      "return Empty List if absent from principal and subordinate" in new Setup {
        EnvironmentAwareQueryConnectorMock.Subordinate.ByQuery.returnsFor(userIdQuery, Nil)
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.returnsFor(userIdQuery, Nil)

        await(fetcher.fetchApplicationsByUserIds(userIds)) shouldBe List.empty
      }

      "return an application from subordinate if present" in new Setup {
        EnvironmentAwareQueryConnectorMock.Subordinate.ByQuery.returnsFor(userIdQuery, List(application))
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.returnsFor(userIdQuery, Nil)

        await(fetcher.fetchApplicationsByUserIds(userIds)) shouldBe List(application)
      }

      "return an application from principal if present" in new Setup {
        EnvironmentAwareQueryConnectorMock.Subordinate.ByQuery.returnsFor(userIdQuery, Nil)
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.returnsFor(userIdQuery, List(application))

        await(fetcher.fetchApplicationsByUserIds(userIds)) shouldBe List(application)
      }

      "return a combined list of applications from both envs" in new Setup {
        EnvironmentAwareQueryConnectorMock.Subordinate.ByQuery.returnsFor(userIdQuery, List(application2))
        EnvironmentAwareQueryConnectorMock.Principal.ByQuery.returnsFor(userIdQuery, List(application))

        await(fetcher.fetchApplicationsByUserIds(userIds)) should contain theSameElementsAs List(application, application2)
      }
    }

    "getAppsForResponsibleIndividualOrAdmin is called" should {
      val request: GetAppsForAdminOrRIRequest = GetAppsForAdminOrRIRequest(LaxEmailAddress("a@example.com"))

      "return Empty List if given a request with no email" in new Setup {
        val emptyRequest: GetAppsForAdminOrRIRequest = GetAppsForAdminOrRIRequest(LaxEmailAddress(""))
        await(fetcher.getAppsForResponsibleIndividualOrAdmin(emptyRequest)) shouldBe List.empty
      }

      "return Empty List if absent from principal and subordinate" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.GetAppsForResponsibleIndividualOrAdmin.thenReturnEmptyList(request)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.GetAppsForResponsibleIndividualOrAdmin.thenReturnEmptyList(request)
        await(fetcher.getAppsForResponsibleIndividualOrAdmin(request)) shouldBe List.empty
      }

      "return an application from subordinate if present" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.GetAppsForResponsibleIndividualOrAdmin.thenReturn(request)(List(application))
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.GetAppsForResponsibleIndividualOrAdmin.thenReturnEmptyList(request)
        await(fetcher.getAppsForResponsibleIndividualOrAdmin(request)) shouldBe List(application)
      }

      "return an application from principal if present" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.GetAppsForResponsibleIndividualOrAdmin.thenReturnEmptyList(request)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.GetAppsForResponsibleIndividualOrAdmin.thenReturn(request)(List(application))
        await(fetcher.getAppsForResponsibleIndividualOrAdmin(request)) shouldBe List(application)
      }

      "return a combined list of applications from both envs" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.GetAppsForResponsibleIndividualOrAdmin.thenReturn(request)(List(application2))
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.GetAppsForResponsibleIndividualOrAdmin.thenReturn(request)(List(application))
        await(fetcher.getAppsForResponsibleIndividualOrAdmin(request)) should contain theSameElementsAs List(application, application2)
      }
    }
  }
}
