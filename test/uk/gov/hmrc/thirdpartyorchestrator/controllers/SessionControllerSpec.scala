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

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.tpd.session.domain.models.{LoggedInState, UserSession, UserSessionId}
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker
import uk.gov.hmrc.thirdpartyorchestrator.controllers.SessionController._
import uk.gov.hmrc.thirdpartyorchestrator.mocks.services.SessionServiceMock

class SessionControllerSpec extends BaseControllerSpec with Matchers {

  trait Setup
      extends SessionServiceMock with UserBuilder with LocalUserIdTracker {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val userId     = UserId.random
    val sessionId  = UserSessionId.random
    val session    = UserSession(sessionId, LoggedInState.LOGGED_IN, buildUser(LaxEmailAddress("bob@example.com"), "Bob", "Fleming").copy(userId = userId))
    val controller = new SessionController(sessionServiceMock, Helpers.stubControllerComponents())
  }

  "getDeveloperForSession" should {
    "return 200 if successful" in new Setup {
      val request = FakeRequest().withJsonBody(Json.toJson(SessionRequest(sessionId)))

      fetchSessionByIdReturns(sessionId, session)
      val result = controller.getDeveloperForSession()(request)
      status(result) shouldBe Status.OK
    }

    "return 400 if invalid session id" in new Setup {
      val request = FakeRequest().withBody("""{ "sessionId": "1234576547" }""")

      val result = controller.getDeveloperForSession()(request)
      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) shouldBe "Invalid payload"
    }

    "return 400 if invalid json" in new Setup {
      val request = FakeRequest().withBody(s"""{ "sessionId": "$sessionId" }""")

      val result = controller.getDeveloperForSession()(request)
      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) shouldBe "Invalid payload"
    }

    "return 400 if incorrect json" in new Setup {
      val request = FakeRequest().withBody(s"""{ }""")

      val result = controller.getDeveloperForSession()(request)
      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) should include("Invalid payload")
    }

    "return 404 if session not found" in new Setup {
      val request = FakeRequest().withJsonBody(Json.toJson(SessionRequest(sessionId)))

      fetchSessionByIdReturnsNone(sessionId)
      val result = controller.getDeveloperForSession()(request)
      status(result) shouldBe Status.NOT_FOUND
    }
  }
}
