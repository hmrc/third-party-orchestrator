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
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, UserId}
import uk.gov.hmrc.thirdpartyorchestrator.mocks.connectors.ThirdPartyDeveloperConnectorMockModule
import uk.gov.hmrc.thirdpartyorchestrator.mocks.services.ApplicationByIdFetcherMockModule
import uk.gov.hmrc.thirdpartyorchestrator.services.ApplicationService.GetApplicationResult
import uk.gov.hmrc.thirdpartyorchestrator.utils.{ApplicationBuilder, AsyncHmrcSpec, DeveloperBuilder}

class ApplicationServiceSpec extends AsyncHmrcSpec {

  trait Setup extends ThirdPartyDeveloperConnectorMockModule with ApplicationByIdFetcherMockModule with DeveloperBuilder with ApplicationBuilder {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val underTest = new ApplicationService(ThirdPartyDeveloperConnectorMock.aMock, ApplicationByIdFetcherMock.aMock)

    val applicationId = ApplicationId.random
    val email         = "thirdpartydeveloper@example.com".toLaxEmail
    val userId1       = UserId.random
    val userId2       = UserId.random
    val clientId      = ClientId.random
    val developer1    = buildDeveloper(userId1, email, "Bob", "Fleming", true)
    val developer2    = buildDeveloper(userId2, email, "Bob", "Fleming", false)
    val application   = buildApplication(applicationId, clientId, userId1, userId2)
  }

  "fetchVerifiedCollaboratorsForApplication" should {
    "return the collaborators when the application exists" in new Setup {
      ApplicationByIdFetcherMock.FetchApplication.thenReturn(applicationId)(Some(application))
      ThirdPartyDeveloperConnectorMock.FetchDeveloper.thenReturn(userId1)(Some(developer1))
      ThirdPartyDeveloperConnectorMock.FetchDeveloper.thenReturn(userId2)(Some(developer2))
      val result = await(underTest.fetchVerifiedCollaboratorsForApplication(applicationId))
      result shouldBe Right(GetApplicationResult(application, Set(developer1)))
    }

    "return None when application does not exist" in new Setup {
      ApplicationByIdFetcherMock.FetchApplication.thenReturn(applicationId)(None)
      val result = await(underTest.fetchVerifiedCollaboratorsForApplication(applicationId))
      result.left.value shouldBe "Application not found"
    }
  }
}
