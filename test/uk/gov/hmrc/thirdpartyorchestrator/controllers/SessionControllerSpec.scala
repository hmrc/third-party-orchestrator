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
import uk.gov.hmrc.thirdpartyorchestrator.domain.models.{MfaId, SessionId}
import uk.gov.hmrc.thirdpartyorchestrator.mocks.services.SessionServiceMock
import uk.gov.hmrc.thirdpartyorchestrator.utils.SessionBuilder

class SessionControllerSpec extends BaseControllerSpec with Matchers {

  trait Setup
      extends SessionServiceMock with SessionBuilder {

    implicit val hc = HeaderCarrier()

    val userId      = UserId.random
    val sessionId   = SessionId.random
    val session     = buildSession(sessionId, userId, MfaId.random, MfaId.random, "Bob", "Fleming", LaxEmailAddress("bob@example.com"))
    val fakeRequest = FakeRequest("GET", "/")
    val controller  = new SessionController(sessionServiceMock, Helpers.stubControllerComponents())
  }

  "getDeveloperForSession" should {
    "return 200 if successful" in new Setup {
      fetchSessionByIdReturns(sessionId, session)
      val result = controller.getDeveloperForSession(sessionId.toString)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 400 if invalid session id" in new Setup {
      val invalidSessionId = "1234567890"
      val result           = controller.getDeveloperForSession(invalidSessionId)(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 404 if session not found" in new Setup {
      fetchSessionByIdReturnsNone(sessionId)
      val result = controller.getDeveloperForSession(sessionId.toString)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }
  }
}
