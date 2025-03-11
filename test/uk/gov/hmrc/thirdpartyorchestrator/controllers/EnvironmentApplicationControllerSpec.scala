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

package uk.gov.hmrc.thirdpartyorchestrator.controllers

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.matchers.should.Matchers

import play.api.http.{ContentTypes, HeaderNames, Status}
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaboratorsFixtures, Collaborators, PaginatedApplications}
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.{ApplicationNameValidationRequest, ApplicationNameValidationResult, ChangeApplicationNameValidationRequest}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker
import uk.gov.hmrc.thirdpartyorchestrator.mocks.connectors.ThirdPartyApplicationConnectorMockModule

class EnvironmentApplicationControllerSpec extends BaseControllerSpec with Matchers {

  trait Setup
      extends UserBuilder with LocalUserIdTracker with ApplicationWithCollaboratorsFixtures with ThirdPartyApplicationConnectorMockModule {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val applicationId = ApplicationId.random
    val clientId      = ClientId.random
    val developer     = buildUser(emailOne, "Bob", "Fleming").copy(verified = true)
    val application   = standardApp.withCollaborators(Collaborators.Administrator(userIdOne, emailOne))
    val controller    = new EnvironmentApplicationController(EnvironmentAwareThirdPartyApplicationConnectorMock.instance, Helpers.stubControllerComponents())
  }

  "searchApplications" should {
    "return 200 if successful" in new Setup {
      val appRequest = FakeRequest("GET", s"/applications?clientId=12233455")
      EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.SearchApplications.thenReturns(PaginatedApplications(List(application), 0, 0, 0, 0))
      val result     = controller.searchApplications(Environment.SANDBOX)(appRequest)
      status(result) shouldBe Status.OK
    }
  }

  "validateName" should {
    "return 200 if successful" in new Setup {
      val request: ApplicationNameValidationRequest = ChangeApplicationNameValidationRequest("MyAppName", applicationId)
      val fakeRequest                               = FakeRequest("POST", "/environment/SANDBOX/application/name/validate")
        .withBody(Json.toJson(request))
        .withHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))
      val response: ApplicationNameValidationResult = ApplicationNameValidationResult.Valid
      EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.ValidateName.thenReturns(request)(response)

      val result = controller.validateName(Environment.SANDBOX)(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(response)
    }

    "return 404 if application is not found" in new Setup {
      val request: ApplicationNameValidationRequest = ChangeApplicationNameValidationRequest("MyAppName", applicationId)
      val fakeRequest                               = FakeRequest("POST", "/environment/SANDBOX/application/name/validate")
        .withBody(Json.toJson(request))
        .withHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))
      EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.ValidateName.thenReturnsNone(request)

      val result = controller.validateName(Environment.SANDBOX)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }
  }
}
