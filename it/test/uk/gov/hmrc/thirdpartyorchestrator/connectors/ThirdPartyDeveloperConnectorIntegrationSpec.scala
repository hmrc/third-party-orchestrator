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

package uk.gov.hmrc.thirdpartyorchestrator.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration, Mode}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, UserId}
import uk.gov.hmrc.apiplatform.modules.tpd.session.domain.models.{LoggedInState, UserSession, UserSessionId}
import uk.gov.hmrc.apiplatform.modules.tpd.test.builders.UserBuilder
import uk.gov.hmrc.apiplatform.modules.tpd.test.utils.LocalUserIdTracker
import uk.gov.hmrc.thirdpartyorchestrator.utils.WireMockExtensions

class ThirdPartyDeveloperConnectorIntegrationSpec extends BaseConnectorIntegrationSpec
    with GuiceOneAppPerSuite with WireMockExtensions {

  private val stubConfig = Configuration(
    "microservice.services.third-party-developer.port" -> stubPort
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .in(Mode.Test)
      .build()

  trait Setup extends UserBuilder with LocalUserIdTracker {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val userEmail         = "thirdpartydeveloper@example.com".toLaxEmail
    val applicationId     = ApplicationId.random
    val userId            = UserId.random
    val sessionId         = UserSessionId.random
    val expectedSession   = UserSession(sessionId, LoggedInState.LOGGED_IN, buildUser(userEmail, "John", "Doe").copy(userId = userId))
    val expectedDeveloper = buildUser(userEmail, "John", "Doe").copy(userId = userId, verified = true)

    val underTest: ThirdPartyDeveloperConnector = app.injector.instanceOf[ThirdPartyDeveloperConnector]
  }

  "fetchSession" should {
    "return the session" in new Setup {

      stubFor(
        get(urlPathEqualTo(s"/session/$sessionId"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(s"""{
                           |  "sessionId": "$sessionId",
                           |  "loggedInState": "LOGGED_IN",
                           |  "developer": {
                           |    "userId": "$userId",
                           |    "email": "${userEmail.text}",
                           |    "firstName": "John",
                           |    "lastName": "Doe",
                           |    "registrationTime": "$nowAsText",
                           |    "lastModified": "$nowAsText",
                           |    "verified": true,
                           |    "mfaEnabled": true,
                           |    "mfaDetails": [],
                           |    "emailPreferences": { 
                           |      "interests": [],
                           |      "topics": []
                           |    }
                           |  }
                           |}""".stripMargin)
          )
      )

      private val result = await(underTest.fetchSession(sessionId))

      result shouldBe Some(expectedSession)
    }
  }

  "fetchDeveloper" should {
    "return the developer" in new Setup {

      stubFor(
        get(urlPathEqualTo(s"/developer"))
          .withQueryParam("developerId", equalTo(userId.toString()))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(s"""{
                           |  "userId": "$userId",
                           |  "email": "${userEmail.text}",
                           |  "firstName": "John",
                           |  "lastName": "Doe",
                           |  "registrationTime": "$nowAsText",
                           |  "lastModified": "$nowAsText",
                           |  "verified": true,
                           |  "mfaDetails": [],
                           |  "emailPreferences": { 
                           |    "interests": [],

                           |    "topics": []

                           |  }
                           |}""".stripMargin)
          )
      )

      private val result = await(underTest.fetchDeveloper(userId))

      result shouldBe Some(expectedDeveloper)
    }
  }
}
