/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.thirdpartyorchestrator.commands.applications.controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.{Application, Configuration, Mode}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils
import uk.gov.hmrc.thirdpartyorchestrator.utils._

class AppCmdControllerISpec
    extends AsyncHmrcSpec
    with WireMockExtensions
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with PrincipalAndSubordinateWireMockSetup
    with ApplicationWithCollaboratorsFixtures
    with utils.FixedClock {

  private val stubConfig = Configuration(
    "microservice.services.third-party-application-principal.host"   -> WireMockHost,
    "microservice.services.third-party-application-principal.port"   -> WireMockPrincipalPort,
    "microservice.services.third-party-application-subordinate.host" -> WireMockHost,
    "microservice.services.third-party-application-subordinate.port" -> WireMockSubordinatePort
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .in(Mode.Test)
      .build()

  trait Setup {
    // val applicationId              = ApplicationId.random
    implicit val hc: HeaderCarrier = HeaderCarrier()
    lazy val baseUrl               = s"http://localhost:$port"

    val wsClient           = app.injector.instanceOf[WSClient]
    val requestorEmail     = "requestor@example.com".toLaxEmail
    val newTeamMemberEmail = "newTeamMember@example.com".toLaxEmail
    val newCollaborator    = Collaborators.Administrator(UserId.random, newTeamMemberEmail)
    val cmd                = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(requestorEmail), newCollaborator, instant)
    val request            = DispatchRequest(cmd, Set.empty[LaxEmailAddress])
  }

  "AppCmdController" should {
    // "return 400 when payload is valid json but not valid object" in new Setup {
    //   // command: ApplicationCommand, verifiedCollaboratorsToNotify: Set[LaxEmailAddress])
    //   val body                 = Json.toJson("""{"command":"somecommand", "verifiedCollaboratorsToNotify":[]  }""").toString()
    //   val response: WSResponse = await(wsClient.url(s"${baseUrl}/applications/${applicationId.value}/dispatch").withHttpHeaders(("content-type", "application/json")).patch(body))
    //   response.status shouldBe BAD_REQUEST

    //   // check stub not called.????
    // }

    "return 401 when Unauthorised is returned from connector" in new Setup {
      stubForApplication(applicationIdOne, ClientId.random)

      stubFor(Environment.SANDBOX)(
        patch(urlMatching(s"/application/$applicationIdOne/dispatch"))
          .willReturn(
            aResponse()
              .withStatus(UNAUTHORIZED)
          )
      )

      val body                 = Json.toJson(request).toString()
      val response: WSResponse = await(wsClient.url(s"${baseUrl}/applications/$applicationIdOne/dispatch").withHttpHeaders(("content-type", "application/json")).patch(body))
      response.status shouldBe UNAUTHORIZED
    }
  }

  private def stubForApplication(applicationId: ApplicationId, clientId: ClientId, userId1: UserId = UserId.random, userId2: UserId = UserId.random) = {
    stubFor(Environment.PRODUCTION)(
      get(urlPathEqualTo(s"/application/$applicationId"))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withHeader("Content-Type", "application/json")
        )
    )
    stubFor(Environment.SANDBOX)(
      get(urlPathEqualTo(s"/application/$applicationId"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(getBody(applicationId, clientId, userId1, userId2))
        )
    )
  }

  private def getBody(applicationId: ApplicationId, clientId: ClientId, userId1: UserId, userId2: UserId) = {
    val app = standardApp.withId(applicationId).modify(_.copy(clientId = clientId, deployedTo = Environment.SANDBOX))
    Json.toJson(app).toString()
  }
}
