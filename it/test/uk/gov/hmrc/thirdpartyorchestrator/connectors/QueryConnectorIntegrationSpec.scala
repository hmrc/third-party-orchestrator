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

import play.api.http.ContentTypes
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.{Application, Configuration, Mode}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, ApplicationWithCollaboratorsFixtures}
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.thirdpartyorchestrator.utils.{TestData, WireMockExtensions}

class QueryConnectorIntegrationSpec extends BaseConnectorIntegrationSpec
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

    val underTest: QueryConnector = app.injector.instanceOf[PrincipalQueryConnector]
  }

  "query" should {
    "return the application by id" in new Setup {
      stubFor(
        get(urlPathEqualTo("/query"))
          .withQueryParam("applicationId", equalTo(applicationId.toString))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
              .withBody(getBody())
          )
      )

      private val result = await(underTest.query[Option[ApplicationWithCollaborators]](ApplicationQuery.ById(applicationId, Nil, false)))

      result shouldBe Some(expectedApplication)
    }

    "handle not found" in new Setup {
      stubFor(
        get(urlPathEqualTo("/query"))
          .withQueryParam("applicationId", equalTo(applicationId.toString))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
          )
      )

      private val result = await(underTest.query[Option[ApplicationWithCollaborators]](ApplicationQuery.ById(applicationId, Nil, false)))

      result shouldBe None
    }

    "handle bad request" in new Setup {
      stubFor(
        get(urlPathEqualTo("/query"))
          .withQueryParam("applicationId", equalTo(applicationId.toString))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(underTest.query[Option[ApplicationWithCollaborators]](ApplicationQuery.ById(applicationId, Nil, false)))
      }
    }

    "return the application by clientId" in new Setup {
      stubFor(
        get(urlPathEqualTo("/query"))
          .withQueryParam("clientId", equalTo(clientId.value))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
              .withBody(getBody())
          )
      )

      private val result = await(underTest.query[Option[ApplicationWithCollaborators]](ApplicationQuery.ByClientId(clientId, false, Nil, false)))

      result shouldBe Some(expectedApplication)
    }
  }

  "postQuery" should {
    "return the application by id" in new Setup {
      stubFor(
        post(urlPathEqualTo("/query"))
          .withJsonRequestBody(Map("applicationId" -> List(applicationId.toString)))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
              .withBody(getBody())
          )
      )

      private val result = await(underTest.postQuery[Option[ApplicationWithCollaborators]](ApplicationQuery.ById(applicationId, Nil, false)))

      result shouldBe Some(expectedApplication)
    }

    "handle not found" in new Setup {
      stubFor(
        post(urlPathEqualTo("/query"))
          .withJsonRequestBody(Map("applicationId" -> List(applicationId.toString)))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
          )
      )

      private val result = await(underTest.postQuery[Option[ApplicationWithCollaborators]](ApplicationQuery.ById(applicationId, Nil, false)))

      result shouldBe None
    }

    "handle bad request" in new Setup {
      stubFor(
        post(urlPathEqualTo("/query"))
          .withJsonRequestBody(Map("applicationId" -> List(applicationId.toString)))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
          )
      )

      intercept[UpstreamErrorResponse] {
        await(underTest.postQuery[Option[ApplicationWithCollaborators]](ApplicationQuery.ById(applicationId, Nil, false)))
      }
    }

    "return the application by clientId" in new Setup {
      stubFor(
        post(urlPathEqualTo("/query"))
          .withJsonRequestBody(Map("clientId" -> List(clientId.toString)))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo(ContentTypes.JSON))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
              .withBody(getBody())
          )
      )

      private val result = await(underTest.postQuery[Option[ApplicationWithCollaborators]](ApplicationQuery.ByClientId(clientId, false, Nil, false)))

      result shouldBe Some(expectedApplication)
    }
  }

  private def getBody() = {
    Json.toJson(standardApp).toString
  }
}
