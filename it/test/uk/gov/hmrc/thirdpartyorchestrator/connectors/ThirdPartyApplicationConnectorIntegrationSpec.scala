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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, UserId}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.thirdpartyorchestrator.utils.{ApplicationBuilder, WireMockExtensions}

class ThirdPartyApplicationConnectorIntegrationSpec extends BaseConnectorIntegrationSpec
    with GuiceOneAppPerSuite with WireMockExtensions with FixedClock {

  private val stubConfig = Configuration(
    "microservice.services.third-party-application-principal.port" -> stubPort
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .in(Mode.Test)
      .build()

  trait Setup extends ApplicationBuilder {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val applicationId       = ApplicationId.random
    val clientId            = ClientId.random
    val userId1             = UserId.random
    val userId2             = UserId.random
    val expectedApplication = buildApplication(applicationId, clientId, userId1, userId2)

    val underTest: ThirdPartyApplicationConnector = app.injector.instanceOf[PrincipalThirdPartyApplicationConnector]
  }

  "fetchApplication" should {
    "return the application" in new Setup {

      stubFor(
        get(urlPathEqualTo(s"/application/$applicationId"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(getBody(applicationId, clientId, userId1, userId2))
          )
      )

      private val result = await(underTest.fetchApplication(applicationId))

      result shouldBe Some(expectedApplication)
    }
  }

  "fetchApplicationsByUserIds" should {
    "return the applications" in new Setup {

      stubFor(
        post(urlPathEqualTo(s"/developer/applications"))
          .withJsonRequestBody(CollaboratorUserIds(List(userId1, userId2)))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(s"[${getBody(applicationId, clientId, userId1, userId2)}]")
          )
      )

      private val result = await(underTest.fetchApplicationsByUserIds(List(userId1, userId2)))

      result shouldBe List(expectedApplication)
    }
  }

  "fetchApplicationByClientId" should {
    "return the application" in new Setup {

      stubFor(
        get(urlPathEqualTo("/application"))
          .withQueryParam("clientId", equalTo(clientId.value))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(getBody(applicationId, clientId, userId1, userId2))
          )
      )

      private val result = await(underTest.fetchApplication(clientId))

      result shouldBe Some(expectedApplication)
    }
  }

  private def getBody(applicationId: ApplicationId, clientId: ClientId, userId1: UserId, userId2: UserId) = {
    s"""{
       |  "id": "$applicationId",
       |  "clientId": "$clientId",
       |  "gatewayId": "gateway-id",
       |  "name": "Petes test application",
       |  "deployedTo": "PRODUCTION",
       |  "description": "Petes test application description",
       |  "collaborators": [
       |    {
       |      "userId": "$userId1",
       |      "emailAddress": "bob@example.com",
       |      "role": "ADMINISTRATOR"
       |    },
       |    {
       |      "userId": "$userId2",
       |      "emailAddress": "bob@example.com",
       |      "role": "ADMINISTRATOR"
       |    }
       |  ],
       |  "createdOn": "$nowAsText",
       |  "lastAccess": "$nowAsText",
       |  "grantLength": 547,
       |  "redirectUris": [],
       |  "access": {
       |    "redirectUris": [],
       |    "overrides": [],
       |    "importantSubmissionData": {
       |      "organisationUrl": "https://www.example.com",
       |      "responsibleIndividual": {
       |        "fullName": "Bob Fleming",
       |        "emailAddress": "bob@example.com"
       |      },
       |      "serverLocations": [
       |        {
       |          "serverLocation": "inUK"
       |        }
       |      ],
       |      "termsAndConditionsLocation": {
       |        "termsAndConditionsType": "inDesktop"
       |      },
       |      "privacyPolicyLocation": {
       |        "privacyPolicyType": "inDesktop"
       |      },
       |      "termsOfUseAcceptances": [
       |        {
       |          "responsibleIndividual": {
       |            "fullName": "Bob Fleming",
       |            "emailAddress": "bob@example.com"
       |          },
       |          "dateTime": "$nowAsText",
       |          "submissionId": "4e62811a-7ab3-4421-a89e-65a8bad9b6ae",
       |          "submissionInstance": 0
       |        }
       |      ]
       |    },
       |    "accessType": "STANDARD"
       |  },
       |  "state": {
       |    "name": "TESTING",
       |    "updatedOn": "$nowAsText"
       |  },
       |  "rateLimitTier": "BRONZE",
       |  "blocked": false,
       |  "trusted": false,
       |  "ipAllowlist": {
       |    "required": false,
       |    "allowlist": []
       |  },
       |  "moreApplication": {
       |    "allowAutoDelete": false
       |  }
       |}""".stripMargin
  }
}
