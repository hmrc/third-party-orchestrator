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
import play.api.libs.json.Json
import play.api.{Application, Configuration, Mode}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, ApplicationWithCollaboratorsFixtures, PaginatedApplications}
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.{ApplicationNameValidationRequest, ApplicationNameValidationResult, ChangeApplicationNameValidationRequest}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, UserId}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.thirdpartyorchestrator.utils.{TestData, WireMockExtensions}

class ThirdPartyApplicationConnectorIntegrationSpec extends BaseConnectorIntegrationSpec
    with GuiceOneAppPerSuite with WireMockExtensions with FixedClock with ApplicationWithCollaboratorsFixtures {

  private val stubConfig = Configuration(
    "microservice.services.third-party-application-principal.port" -> stubPort
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .in(Mode.Test)
      .build()

  trait Setup extends TestData {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val userId1                      = userIdOne
    val userId2                      = userIdTwo
    val expectedApplication          = standardApp
    val clientId: ClientId           = expectedApplication.clientId
    val applicationId: ApplicationId = expectedApplication.id

    val underTest: PrincipalThirdPartyApplicationConnector = app.injector.instanceOf[PrincipalThirdPartyApplicationConnector]
  }

  "create" should {
    "return the newly created" in new Setup {
      stubFor(
        post(urlPathEqualTo(s"/application"))
          .withJsonRequestBody(createSandboxApplicationRequest)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader(HeaderNames.CONTENT_TYPE, "application/json")
              .withBody(Json.toJson(standardApp).toString())
          )
      )

      private val result = await(underTest.create(createSandboxApplicationRequest))
      result shouldBe standardApp
    }

    "handle 404 from downstream" in new Setup {
      stubFor(
        post(urlPathEqualTo(s"/application"))
          .withJsonRequestBody(createSandboxApplicationRequest)
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      val exception = intercept[UpstreamErrorResponse] {
        await(underTest.create(createSandboxApplicationRequest))
      }
      exception.statusCode shouldBe NOT_FOUND
    }
  }

  "searchApplications" should {
    "return applications" in new Setup {
      stubFor(
        get(urlEqualTo(s"/applications?accessType=STANDARD"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader(HeaderNames.CONTENT_TYPE, "application/json")
              .withBody(paginatedBody(standardApp))
          )
      )

      private val result = await(underTest.searchApplications(Map("accessType" -> Seq("STANDARD"))))

      result shouldBe PaginatedApplications(List(standardApp), 1, Int.MaxValue, 1, 1)
    }

    "return errors" in new Setup {
      stubFor(
        get(urlEqualTo(s"/applications?accessType=STANDARD"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
          )
      )

      val e = intercept[UpstreamErrorResponse] {
        await(underTest.searchApplications(Map("accessType" -> Seq("STANDARD"))))
      }

      e.statusCode shouldBe BAD_REQUEST
    }
  }

  "fetchApplication" should {
    "return the application" in new Setup {

      stubFor(
        get(urlPathEqualTo(s"/application/$applicationId"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader(HeaderNames.CONTENT_TYPE, "application/json")
              .withBody(getBody(applicationId, clientId, userId1, userId2))
          )
      )

      private val result = await(underTest.fetchApplication(applicationId))

      result shouldBe Some(expectedApplication)
    }
  }

  "validateName" should {
    "pass for ChangeApplicationNameValidationRequest" in new Setup {
      val request: ApplicationNameValidationRequest       = ChangeApplicationNameValidationRequest("MyAppName", applicationId)
      val expectedResult: ApplicationNameValidationResult = ApplicationNameValidationResult.Valid

      stubFor(
        post(urlPathEqualTo(s"/application/name/validate"))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader(HeaderNames.CONTENT_TYPE, "application/json")
              .withJsonBody(expectedResult)
          )
      )

      val result = await(underTest.validateName(request))

      result shouldBe Some(expectedResult)
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
              .withHeader(HeaderNames.CONTENT_TYPE, "application/json")
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
              .withHeader(HeaderNames.CONTENT_TYPE, "application/json")
              .withBody(getBody(applicationId, clientId, userId1, userId2))
          )
      )

      private val result = await(underTest.fetchApplication(clientId))

      result shouldBe Some(expectedApplication)
    }
  }

  "verify" should {
    val verificationCode = "123456"

    "" in new Setup {
      stubFor(
        post(urlPathEqualTo(s"/verify-uplift/$verificationCode"))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
              .withHeader(HeaderNames.CONTENT_TYPE, "application/json")
          )
      )

      val result = await(underTest.verify(verificationCode))

      result.status shouldBe NO_CONTENT
    }
  }

  private def paginatedBody(apps: ApplicationWithCollaborators*) = {
    Json.toJson(PaginatedApplications(apps.toList, 1, Int.MaxValue, apps.size, apps.size)).toString
  }

  private def getBody(applicationId: ApplicationId, clientId: ClientId, userId1: UserId, userId2: UserId) = {
    Json.toJson(standardApp).toString
  }
}
