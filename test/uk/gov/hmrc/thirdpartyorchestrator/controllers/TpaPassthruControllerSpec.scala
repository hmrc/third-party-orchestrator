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
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.Collaborators
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker
import uk.gov.hmrc.thirdpartyorchestrator.mocks.connectors.ThirdPartyApplicationConnectorMockModule
import uk.gov.hmrc.thirdpartyorchestrator.utils.TestData

class TpaPassthruControllerSpec extends BaseControllerSpec with Matchers {

  trait Setup
      extends ThirdPartyApplicationConnectorMockModule
      with UserBuilder
      with LocalUserIdTracker
      with TestData {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val applicationId = ApplicationId.random
    val clientId      = ClientId.random
    val developer     = buildUser(emailOne, "Bob", "Fleming").copy(verified = true)
    val application   = standardApp.withCollaborators(Collaborators.Administrator(userIdOne, emailOne))
    val controller    = new TpaPassthruController(PrincipalThirdPartyApplicationConnectorMock.aMock, Helpers.stubControllerComponents())
  }

  "verifyUplift" should {
    "return 201 if successful in verifying uplift" in new Setup {
      PrincipalThirdPartyApplicationConnectorMock.VerifyUplift.succeedsWith("12345")

      val request = FakeRequest("POST", s"/verify-uplift")
        .withHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))

      val result = controller.verifyUplift("12345")(request)
      status(result) shouldBe Status.NO_CONTENT
    }

    "return 400 if bad request returned from connector" in new Setup {
      PrincipalThirdPartyApplicationConnectorMock.VerifyUplift.failsWithStatus("12345", BAD_REQUEST)

      val request = FakeRequest("POST", s"/verify-uplift")
        .withHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))

      val result = controller.verifyUplift("12345")(request)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 500 if internal server error returned from connector" in new Setup {
      PrincipalThirdPartyApplicationConnectorMock.VerifyUplift.failsWithStatus("12345", INTERNAL_SERVER_ERROR)

      val request = FakeRequest("POST", s"/verify-uplift")
        .withHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))

      val result = controller.verifyUplift("12345")(request)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

}
