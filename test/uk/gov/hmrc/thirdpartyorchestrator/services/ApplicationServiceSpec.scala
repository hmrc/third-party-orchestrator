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

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaboratorsFixtures
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker
import uk.gov.hmrc.thirdpartyorchestrator.mocks.connectors.ThirdPartyDeveloperConnectorMockModule
import uk.gov.hmrc.thirdpartyorchestrator.mocks.services.ApplicationFetcherMockModule
import uk.gov.hmrc.thirdpartyorchestrator.utils.AsyncHmrcSpec

class ApplicationServiceSpec extends AsyncHmrcSpec {

  trait Setup extends ThirdPartyDeveloperConnectorMockModule with ApplicationFetcherMockModule with UserBuilder with LocalUserIdTracker
      with ApplicationWithCollaboratorsFixtures {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val underTest = new ApplicationService(ThirdPartyDeveloperConnectorMock.aMock, ApplicationFetcherMock.aMock)

    val developer1    = buildUser(emailOne, "Bob", "Fleming").copy(userId = userIdOne, verified = true)
    val developer2    = buildUser(emailTwo, "Bob", "Fleming").copy(userId = userIdTwo, verified = false)
    val application   = standardApp.withCollaborators(standardApp.collaborators.take(2))
    val clientId      = application.clientId
    val applicationId = application.id
  }

  "fetchApplication" should {
    "return the application" in new Setup {
      ApplicationFetcherMock.FetchApplication.thenReturn(applicationId)(Some(application))
      val result = await(underTest.fetchApplication(applicationId))
      result shouldBe Some(application)
    }

    "return None when application does not exist" in new Setup {
      ApplicationFetcherMock.FetchApplication.thenReturn(applicationId)(None)
      val result = await(underTest.fetchApplication(applicationId))
      result shouldBe None
    }
  }

  "fetchApplicationByClientId" should {
    "return the application" in new Setup {
      ApplicationFetcherMock.FetchApplicationByClientId.thenReturn(clientId)(Some(application))
      val result = await(underTest.fetchApplication(clientId))
      result shouldBe Some(application)
    }

    "return None when application does not exist" in new Setup {
      ApplicationFetcherMock.FetchApplicationByClientId.thenReturn(clientId)(None)
      val result = await(underTest.fetchApplication(clientId))
      result shouldBe None
    }
  }

  "fetchVerifiedCollaboratorsForApplication" should {
    "return the collaborators when the application exists" in new Setup {
      ApplicationFetcherMock.FetchApplication.thenReturn(applicationId)(Some(application))
      ThirdPartyDeveloperConnectorMock.FetchDeveloper.thenReturn(userIdOne)(Some(developer1))
      ThirdPartyDeveloperConnectorMock.FetchDeveloper.thenReturn(userIdTwo)(Some(developer2))
      val result = await(underTest.fetchVerifiedCollaboratorsForApplication(applicationId))
      result shouldBe Right(Set(developer1))
    }

    "return None when application does not exist" in new Setup {
      ApplicationFetcherMock.FetchApplication.thenReturn(applicationId)(None)
      val result = await(underTest.fetchVerifiedCollaboratorsForApplication(applicationId))
      result.left.value shouldBe "Application not found"
    }
  }

  "fetchApplicationsForEmails" should {
    "return the applications" in new Setup {
      ThirdPartyDeveloperConnectorMock.FetchDevelopers.thenReturn(List(emailOne, emailTwo))(List(developer1, developer2.copy(verified = true)))
      ApplicationFetcherMock.FetchApplicationsByUserId.thenReturn(List(userIdOne, userIdTwo))(List(application))

      val result = await(underTest.fetchApplicationsForEmails(List(emailOne, emailTwo)))

      result shouldBe List(application)
    }

    "return the applications filtering out unverified developers" in new Setup {
      ThirdPartyDeveloperConnectorMock.FetchDevelopers.thenReturn(List(emailOne, emailTwo))(List(developer1, developer2))
      ApplicationFetcherMock.FetchApplicationsByUserId.thenReturn(List(userIdOne))(List(application))

      val result = await(underTest.fetchApplicationsForEmails(List(emailOne, emailTwo)))

      result shouldBe List(application)
    }

    "return empty when no users found" in new Setup {
      ThirdPartyDeveloperConnectorMock.FetchDevelopers.thenReturn(List(emailOne, emailTwo))(List.empty)
      ApplicationFetcherMock.FetchApplicationsByUserId.thenReturn(List.empty)(List.empty)

      val result = await(underTest.fetchApplicationsForEmails(List(emailOne, emailTwo)))

      result shouldBe List.empty
    }

    "return empty when no applications found" in new Setup {
      ThirdPartyDeveloperConnectorMock.FetchDevelopers.thenReturn(List(emailOne, emailTwo))(List(developer1, developer2.copy(verified = true)))
      ApplicationFetcherMock.FetchApplicationsByUserId.thenReturn(List(userIdOne, userIdTwo))(List.empty)

      val result = await(underTest.fetchApplicationsForEmails(List(emailOne, emailTwo)))

      result shouldBe List.empty
    }
  }

}
