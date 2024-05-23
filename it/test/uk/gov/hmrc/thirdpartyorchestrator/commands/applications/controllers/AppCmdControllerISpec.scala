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

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.NonEmptyList
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, InternalServerException, UnauthorizedException}

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, ApplicationId, ClientId, Environment, UserId}
import uk.gov.hmrc.apiplatform.modules.common.utils
import uk.gov.hmrc.thirdpartyorchestrator.commands.applications.domain.models.{AppCmdHandlerTypes, DispatchSuccessResult}
import uk.gov.hmrc.thirdpartyorchestrator.utils._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.thirdpartyorchestrator.commands.applications.connectors.PrincipalAppCmdConnector
import uk.gov.hmrc.thirdpartyorchestrator.commands.applications.connectors.AppCmdConnector
import play.api.Configuration
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Mode
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

class AppCmdControllerISpec
    extends AsyncHmrcSpec
    with WireMockExtensions
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with PrincipalAndSubordinateWireMockSetup
    with ApplicationBuilder
    with utils.FixedClock {

  val stubPort       = sys.env.getOrElse("WIREMOCK", "22222").toInt
  val stubHost       = "localhost"

  private val stubConfig = Configuration(
    "microservice.services.third-party-application-principal.host" -> stubHost,
    "microservice.services.third-party-application-principal.port" -> stubPort
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig)
      .in(Mode.Test)
      .build()

  trait Setup {
    val applicationId = ApplicationId.random
    implicit val hc: HeaderCarrier      = HeaderCarrier()
    lazy val baseUrl = s"http://localhost:$port"

    val wsClient = app.injector.instanceOf[WSClient]
    val requestorEmail     = "requestor@example.com".toLaxEmail
    val newTeamMemberEmail = "newTeamMember@example.com".toLaxEmail
    val newCollaborator = Collaborators.Administrator(UserId.random, newTeamMemberEmail)
    val cmd             = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(requestorEmail), newCollaborator, instant)
    val request         = DispatchRequest(cmd, Set.empty[LaxEmailAddress])
  }


 
  "AppCmdController" should {
    "return 401 when Unauthorised is returned from connector" in new Setup {
       
      stubFor(Environment.PRODUCTION)(
        patch(urlMatching(s"/application/${applicationId.value}/dispatch"))
          .willReturn(
            aResponse()
              .withStatus(UNAUTHORIZED)
          )
      )
     val body = Json.toJson(request).toString()
     println(s"**** $body ****")
      val response: WSResponse = await(wsClient.url(s"${baseUrl}/applications/${applicationId.value}/dispatch").withHttpHeaders(("content-type", "application/json")).patch(body))  
      response.status shouldBe UNAUTHORIZED   
    }
  }
}
