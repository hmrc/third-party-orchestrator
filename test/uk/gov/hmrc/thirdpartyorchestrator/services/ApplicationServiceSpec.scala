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
import uk.gov.hmrc.thirdpartyorchestrator.mocks.connectors.{ThirdPartyApplicationConnectorMockModule, ThirdPartyDeveloperConnectorMockModule}
import uk.gov.hmrc.thirdpartyorchestrator.services.ApplicationService.GetApplicationResult
import uk.gov.hmrc.thirdpartyorchestrator.utils.{ApplicationBuilder, AsyncHmrcSpec, DeveloperBuilder}

class ApplicationServiceSpec extends AsyncHmrcSpec {

  trait Setup extends ThirdPartyDeveloperConnectorMockModule with ThirdPartyApplicationConnectorMockModule with DeveloperBuilder with ApplicationBuilder {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val underTest = new ApplicationService(ThirdPartyDeveloperConnectorMock.aMock, ThirdPartyApplicationConnectorMock.aMock)

    val applicationId = ApplicationId.random
    val email         = "thirdpartydeveloper@example.com".toLaxEmail
    val userId        = UserId.random
    val clientId      = ClientId.random
    val developer     = buildDeveloper(userId, email, "Bob", "Fleming")
    val application   = buildApplication(applicationId, clientId, userId)
  }

  "fetchVerifiedCollaboratorsForApplication" should {
    "return the collaborators when the application exists" in new Setup {
      ThirdPartyApplicationConnectorMock.FetchApplicationById.thenReturn(applicationId)(Some(application))
      ThirdPartyDeveloperConnectorMock.FetchDeveloper.thenReturn(userId)(Some(developer))
      val result = await(underTest.fetchVerifiedCollaboratorsForApplication(applicationId))
      result shouldBe Right(GetApplicationResult(application, Set(developer)))
    }

    "return None when application does not exist" in new Setup {
      ThirdPartyApplicationConnectorMock.FetchApplicationById.thenReturn(applicationId)(None)
      val result = await(underTest.fetchVerifiedCollaboratorsForApplication(applicationId))
      result.left.value shouldBe "Application not found"
    }
  }
}
