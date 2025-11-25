/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api._
import play.api.http.Status.BAD_REQUEST
import play.api.http.{ContentTypes, HeaderNames}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.thirdpartyorchestrator.utils.WireMockExtensions

class ReadEitherWithNoExceptionSpec
    extends BaseConnectorIntegrationSpec
    with GuiceOneAppPerSuite
    with WireMockExtensions {

  private val stubConfig = Configuration(
    "microservice.services.third-party-application-principal.port" -> stubPort
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .in(Mode.Test)
      .build()

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val applicationId: ApplicationId = ApplicationId.random
    val underTest: QueryConnector    = app.injector.instanceOf[PrincipalQueryConnector]
  }

  "ReadEitherWithNoExceptionSpec" should {
    "handle bad request with Either" in new Setup with ReadEitherWithNoException {
      stubFor(
        get(urlPathEqualTo("/query"))
          .withQueryParam("applicationId", equalTo(applicationId.toString))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
              .withBody("""{"code":"BANG", "message": "boom"}""")
          )
      )

      private val result = await(underTest.query[Either[HttpResponse, Option[ApplicationWithCollaborators]]](ApplicationQuery.ById(applicationId, Nil, false)))

      inside(result.swap.value) {
        case HttpResponse(statusCode, message, _) =>
          statusCode shouldBe BAD_REQUEST
          message shouldBe """{"code":"BANG", "message": "boom"}"""
        case _                                    => fail("Unexpected HttpResponse")
      }
    }
  }
}
