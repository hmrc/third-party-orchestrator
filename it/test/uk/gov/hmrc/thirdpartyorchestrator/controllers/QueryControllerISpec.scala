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

package uk.gov.hmrc.thirdpartyorchestrator.controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.{Application, Configuration, Mode}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ParamNames
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils
import uk.gov.hmrc.thirdpartyorchestrator.utils._

class QueryControllerISpec
    extends AsyncHmrcSpec
    with WireMockExtensions
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with PrincipalAndSubordinateWireMockSetup
    with ApplicationWithCollaboratorsFixtures
    with utils.FixedClock {

  val stubConfig = Configuration(
    "microservice.services.third-party-application-principal.host"   -> WireMockHost,
    "microservice.services.third-party-application-principal.port"   -> WireMockPrincipalPort,
    "microservice.services.third-party-application-subordinate.host" -> WireMockHost,
    "microservice.services.third-party-application-subordinate.port" -> WireMockSubordinatePort,
    "inPairedEnvironment"                                            -> false
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .in(Mode.Test)
      .build()

  trait Setup {
    val applicationId              = standardApp.id
    implicit val hc: HeaderCarrier = HeaderCarrier()
    lazy val baseUrl               = s"http://localhost:$port"

    val wsClient           = app.injector.instanceOf[WSClient]
  }

  "QueryController" should {
    "return result passing environment down as query" in new Setup {
      stubFor(Environment.SANDBOX)(
        get(urlPathEqualTo(s"/query"))
          .withQueryParam(ParamNames.ApplicationId, equalTo(s"$applicationId"))
          .withQueryParam(ParamNames.Environment, equalTo("SANDBOX"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.toJson(standardApp.inSandbox().withId(applicationId)).toString())
          )
      )

      val response: WSResponse = await(
        wsClient
          .url(s"$baseUrl/environment/SANDBOX/query")
          .withHttpHeaders(("content-type", "application/json"))
          .withQueryStringParameters((ParamNames.ApplicationId -> s"$applicationId"))
          .get()
      )
      response.status shouldBe OK
    }
  }
}
