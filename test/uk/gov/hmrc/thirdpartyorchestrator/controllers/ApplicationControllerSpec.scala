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

import play.api.http.Status
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker
import uk.gov.hmrc.thirdpartyorchestrator.mocks.services.ApplicationServiceMock
import uk.gov.hmrc.thirdpartyorchestrator.utils.ApplicationBuilder

class ApplicationControllerSpec extends BaseControllerSpec with Matchers {

  trait Setup
      extends ApplicationServiceMock with UserBuilder with ApplicationBuilder with LocalUserIdTracker {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val applicationId = ApplicationId.random
    val clientId      = ClientId.random
    val userId1       = UserId.random
    val userId2       = UserId.random
    val email         = LaxEmailAddress("bob@example.com")
    val developer     = buildUser(email, "Bob", "Fleming").copy(userId = userId1, verified = true)
    val application   = buildApplication(applicationId, clientId, userId1, userId2)
    val controller    = new ApplicationController(applicationServiceMock, Helpers.stubControllerComponents())
  }

  "getApplication" should {
    "return 200 if successful" in new Setup {
      val appRequest = FakeRequest("GET", s"/applications/${applicationId}")
      fetchApplicationByIdReturns(applicationId, application)
      val result     = controller.getApplication(applicationId)(appRequest)
      status(result) shouldBe Status.OK
    }

    "return 404 if application not found" in new Setup {
      val appRequest = FakeRequest("GET", s"/applications/${applicationId}")
      fetchApplicationByIdNotFound(applicationId)
      val result     = controller.getApplication(applicationId)(appRequest)
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "queryDispatcher" should {
    "return 200 if get application by client id is successful" in new Setup {
      val appRequest = FakeRequest("GET", s"/applications?clientId=${clientId}")
      fetchApplicationByClientIdReturns(clientId, application)
      val result     = controller.queryDispatcher()(appRequest)
      status(result) shouldBe Status.OK
    }

    "return 404 if get application by client id is not found" in new Setup {
      val appRequest = FakeRequest("GET", s"/applications?clientId=${clientId}")
      fetchApplicationByClientIdNotFound(clientId)
      val result     = controller.queryDispatcher()(appRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "return 400 if query params not found" in new Setup {
      val appRequest = FakeRequest("GET", s"/applications")
      val result     = controller.queryDispatcher()(appRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }

  "getVerifiedCollaboratorsForApplication" should {
    "return 200 if successful" in new Setup {
      val devsRequest = FakeRequest("GET", s"/applications/${applicationId}/developers")
      fetchVerifiedCollaboratorsForApplicationReturns(applicationId, Set(developer))
      val result      = controller.getVerifiedDevelopersForApplication(applicationId)(devsRequest)
      status(result) shouldBe Status.OK
    }

    "return 200 if application found but no verified developers" in new Setup {
      val devsRequest = FakeRequest("GET", s"/applications/${applicationId}/developers")
      fetchVerifiedCollaboratorsForApplicationReturns(applicationId, Set.empty)
      val result      = controller.getVerifiedDevelopersForApplication(applicationId)(devsRequest)
      status(result) shouldBe Status.OK
    }

    "return 404 if application not found" in new Setup {
      val devsRequest = FakeRequest("GET", s"/applications/${applicationId}/developers")
      fetchVerifiedCollaboratorsForApplicationNotFound(applicationId)
      val result      = controller.getVerifiedDevelopersForApplication(applicationId)(devsRequest)
      status(result) shouldBe Status.NOT_FOUND
    }
  }
}
