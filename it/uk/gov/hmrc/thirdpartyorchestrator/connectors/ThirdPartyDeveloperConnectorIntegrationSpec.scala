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
import uk.gov.hmrc.thirdpartyorchestrator.utils.WireMockExtensions
import uk.gov.hmrc.thirdpartyorchestrator.utils.DeveloperBuilder
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, UserId}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.{MfaId, SessionId}

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

  trait Setup extends DeveloperBuilder {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val userEmail         = "thirdpartydeveloper@example.com".toLaxEmail
    val applicationId     = ApplicationId.random
    val userId            = UserId.random
    val authMfaId         = MfaId.random
    val smsMfaId          = MfaId.random
    val sessionId         = SessionId.random
    val expectedSession   = buildSession(sessionId, userId, authMfaId, smsMfaId, "John", "Doe", userEmail)
    val expectedDeveloper = buildDeveloper(userId, userEmail, "John", "Doe", authMfaId, smsMfaId)

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
                           |    "registrationTime": "2022-12-23T12:24:31.123",
                           |    "lastModified": "2023-10-23T12:24:32.543",
                           |    "verified": true,
                           |    "organisation": "Example Corp",
                           |    "accountSetup": {
                           |      "roles": [
                           |        "ROLE1"
                           |      ],
                           |      "services": [
                           |        "SERVICE1"
                           |      ],
                           |      "targets": [
                           |        "TARGET1"
                           |      ],
                           |      "incomplete": false
                           |    },
                           |    "mfaEnabled": true,
                           |    "mfaDetails": [ 
                           |       {
                           |         "id": "$authMfaId",
                           |         "name": "Petes App",
                           |         "createdOn": "2022-12-28T11:21:31.123",
                           |         "verified": true,
                           |         "mfaType": "AUTHENTICATOR_APP"
                           |       },
                           |       {
                           |         "id": "$smsMfaId",
                           |         "name": "Petes Phone",
                           |         "createdOn": "2023-01-21T11:21:31.123",
                           |         "mobileNumber": "07123456789",
                           |         "verified": true,
                           |         "mfaType": "SMS"
                           |       }
                           |    ],
                           |    "nonce": "2435364598347653405635324543645634575",
                           |    "emailPreferences": { 
                           |      "interests": [
                           |        {
                           |          "regime": "REGIME1",
                           |          "services": [
                           |            "SERVICE1",
                           |            "SERVICE2"
                           |          ]
                           |        }
                           |      ], 
                           |      "topics": [
                           |        "TECHNICAL"
                           |      ] 
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
                           |  "registrationTime": "2022-12-23T12:24:31.123",
                           |  "lastModified": "2023-10-23T12:24:32.543",
                           |  "verified": true,
                           |  "organisation": "Example Corp",
                           |  "accountSetup": {
                           |    "roles": [
                           |      "ROLE1"
                           |    ],
                           |    "services": [
                           |      "SERVICE1"
                           |    ],
                           |    "targets": [
                           |      "TARGET1"
                           |    ],
                           |    "incomplete": false
                           |  },
                           |  "mfaEnabled": true,
                           |  "mfaDetails": [ 
                           |     {
                           |       "id": "$authMfaId",
                           |       "name": "Petes App",
                           |       "createdOn": "2022-12-28T11:21:31.123",
                           |       "verified": true,
                           |       "mfaType": "AUTHENTICATOR_APP"
                           |     },
                           |     {
                           |       "id": "$smsMfaId",
                           |       "name": "Petes Phone",
                           |       "createdOn": "2023-01-21T11:21:31.123",
                           |       "mobileNumber": "07123456789",
                           |       "verified": true,
                           |       "mfaType": "SMS"
                           |     }
                           |  ],
                           |  "nonce": "2435364598347653405635324543645634575",
                           |  "emailPreferences": { 
                           |    "interests": [
                           |      {
                           |        "regime": "REGIME1",
                           |        "services": [
                           |          "SERVICE1",
                           |          "SERVICE2"
                           |        ]
                           |      }
                           |    ], 
                           |    "topics": [
                           |      "TECHNICAL"
                           |    ] 
                           |  }
                           |}""".stripMargin)
          )
      )

      private val result = await(underTest.fetchDeveloper(userId))

      result shouldBe Some(expectedDeveloper)
    }
  }
}
