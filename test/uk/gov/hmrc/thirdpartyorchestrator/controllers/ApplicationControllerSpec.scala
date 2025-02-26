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
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationName, ApplicationWithCollaboratorsFixtures, Collaborators}
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.CreateApplicationRequestV1
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker
import uk.gov.hmrc.thirdpartyorchestrator.mocks.services.ApplicationServiceMock

class ApplicationControllerSpec extends BaseControllerSpec with Matchers {

  trait Setup
      extends ApplicationServiceMock with UserBuilder with LocalUserIdTracker with ApplicationWithCollaboratorsFixtures {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val applicationId = ApplicationId.random
    val clientId      = ClientId.random
    val developer     = buildUser(emailOne, "Bob", "Fleming").copy(verified = true)
    val application   = standardApp.withCollaborators(Collaborators.Administrator(userIdOne, emailOne))
    val controller    = new ApplicationController(applicationServiceMock, Helpers.stubControllerComponents())
  }

  "create" should {
    "return 201 if successful in creating an application" in new Setup {
      val admin = Collaborators.Administrator(userIdThree, LaxEmailAddress("jim@example.com"))

      val createApplicationRequest =
        CreateApplicationRequestV1.create(
          name = ApplicationName("Test V1 Application"),
          access = Access.Standard(),
          description = None,
          environment = Environment.SANDBOX,
          collaborators = Set(admin),
          subscriptions = None
        )

      createApplicationReturns(createApplicationRequest, application)

      val request = FakeRequest("POST", s"/application").withBody(Json.toJson(createApplicationRequest)).withHeaders(("content-type", "application/json"))

      val result = controller.create()(request)
      status(result) shouldBe Status.CREATED
    }
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

  "getApplicationsByEmail" should {
    "return the applications if successful" in new Setup {
      val appRequest = FakeRequest("POST", "/developer/applications")
        .withBody(Json.toJson(ApplicationsByRequest(List(emailOne, emailTwo))))

      fetchApplicationsForEmailReturns(List(emailOne, emailTwo), application)

      val result = controller.getApplicationsByEmail()(appRequest)

      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(List(application))
    }

    "return 500 if service call failed" in new Setup {
      val appRequest = FakeRequest("POST", "/developer/applications")
        .withBody(Json.toJson(ApplicationsByRequest(List(emailOne, emailTwo))))

      fetchApplicationsForEmailFails()

      val result = controller.getApplicationsByEmail()(appRequest)

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

  }

  "getApplicationsByCollaborator" should {
    "return the applications if successful" in new Setup {
      val appRequest = FakeRequest("GET", s"/developer/$userIdOne/applications")

      fetchApplicationsForUserIdReturns(userIdOne, application)

      val result = controller.getApplicationsByCollaborator(userIdOne)(appRequest)

      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(List(application))
    }

    "return 500 if service call failed" in new Setup {
      val appRequest = FakeRequest("GET", s"/developer/$userIdOne/applications")

      fetchApplicationsForUserIdFails()

      val result = controller.getApplicationsByCollaborator(userIdOne)(appRequest)

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

  }
}
