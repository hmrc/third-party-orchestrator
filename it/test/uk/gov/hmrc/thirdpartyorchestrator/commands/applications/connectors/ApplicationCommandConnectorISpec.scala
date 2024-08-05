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

package uk.gov.hmrc.thirdpartyorchestrator.commands.applications.connectors

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.NonEmptyList
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.http.Status._
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, UnauthorizedException}

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, ApplicationId, ClientId, Environment, UserId}
import uk.gov.hmrc.apiplatform.modules.common.utils
import uk.gov.hmrc.thirdpartyorchestrator.commands.applications.domain.models.{AppCmdHandlerTypes, DispatchSuccessResult}
import uk.gov.hmrc.thirdpartyorchestrator.utils._

class ApplicationCommandConnectorISpec
    extends AsyncHmrcSpec
    with WireMockExtensions
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with PrincipalAndSubordinateWireMockSetup
    with ApplicationBuilder
    with utils.FixedClock
    with HttpClientV2Support {

  trait Setup {

    implicit val hc: HeaderCarrier = HeaderCarrier()
    val apiKeyTest                 = "5bb51bca-8f97-4f2b-aee4-81a4a70a42d3"
    val bearer                     = "TestBearerToken"

    val applicationId = ApplicationId.random
    val clientId      = ClientId.random

    def anApplicationResponse(createdOn: Instant = instant, lastAccess: Instant = instant): ApplicationResponse = {
      ApplicationResponse(
        id = applicationId,
        clientId = clientId,
        gatewayId = "gatewayId",
        name = ApplicationName("appName"),
        deployedTo = Environment.PRODUCTION,
        description = Some("random description"),
        collaborators = Set.empty,
        createdOn = createdOn,
        lastAccess = Some(lastAccess),
        grantLength = GrantLength.EIGHTEEN_MONTHS,
        lastAccessTokenUsage = None,
        termsAndConditionsUrl = None,
        privacyPolicyUrl = None,
        access = Access.Standard(),
        state = ApplicationState(State.TESTING, None, None, None, updatedOn = instant),
        rateLimitTier = RateLimitTier.BRONZE,
        checkInformation = None,
        blocked = false,
        trusted = false,
        ipAllowlist = IpAllowlist(),
        moreApplication = MoreApplication(true)
      )
    }
  }

  trait PrincipalSetup extends Setup {
    self: Setup =>

    val config = PrincipalAppCmdConnector.Config(
      baseUrl = s"http://$WireMockHost:$WireMockPrincipalPort"
    )

    val connector: AppCmdConnector = new PrincipalAppCmdConnector(config, httpClientV2)
    val url                        = s"${config.baseUrl}/application/${applicationId.value}/dispatch"
  }

  trait SubordinateSetup {
    self: Setup =>

    val config    = SubordinateAppCmdConnector.Config(
      baseUrl = s"http://$WireMockHost:$WireMockSubordinatePort",
      useProxy = false,
      bearerToken = bearer,
      apiKey = apiKeyTest
    )
    val connector = new SubordinateAppCmdConnector(config, httpClientV2)
    val url       = s"${config.baseUrl}/application/${applicationId.value}/dispatch"
  }

  trait CollaboratorSetup extends Setup {
    val requestorEmail     = "requestor@example.com".toLaxEmail
    val newTeamMemberEmail = "newTeamMember@example.com".toLaxEmail
    val adminsToEmail      = Set("bobby@example.com".toLaxEmail, "daisy@example.com".toLaxEmail)

    val newCollaborator = Collaborators.Administrator(UserId.random, newTeamMemberEmail)
    val cmd             = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(requestorEmail), newCollaborator, instant)
    val request         = DispatchRequest(cmd, adminsToEmail)
  }

  "addCollaborator" should {
    "return success" in new CollaboratorSetup with PrincipalSetup {
      val response = anApplicationResponse()

      stubFor(Environment.PRODUCTION)(
        patch(urlMatching(s".*/application/${applicationId.value}/dispatch"))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withJsonBody(DispatchSuccessResult(response))
              .withStatus(OK)
          )
      )

      val result: Either[AppCmdHandlerTypes.Failures, AppCmdHandlerTypes.Success] = await(connector.dispatch(applicationId, request))

      result shouldBe Right(DispatchSuccessResult(response))
    }

    "return teamMember already exists response" in new CollaboratorSetup with PrincipalSetup {
      import uk.gov.hmrc.apiplatform.modules.common.domain.services.NonEmptyListFormatters._
      val response = NonEmptyList.one[CommandFailure](CommandFailures.CollaboratorAlreadyExistsOnApp)

      stubFor(Environment.PRODUCTION)(
        patch(urlMatching(s".*/application/${applicationId.value}/dispatch"))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withJsonBody(response)
              .withStatus(BAD_REQUEST)
          )
      )

      val result = await(connector.dispatch(applicationId, request))

      result.left.value shouldBe NonEmptyList.one(CommandFailures.CollaboratorAlreadyExistsOnApp)
    }

    "return unauthorised" in new CollaboratorSetup with PrincipalSetup {
      stubFor(Environment.PRODUCTION)(
        patch(urlMatching(s".*/application/${applicationId.value}/dispatch"))
          .willReturn(
            aResponse()
              .withStatus(UNAUTHORIZED)
          )
      )

      intercept[UnauthorizedException] {
        await(connector.dispatch(applicationId, request))
      }.message shouldBe (s"Command unauthorised")
    }

    "return for generic error" in new CollaboratorSetup with PrincipalSetup {

      stubFor(Environment.PRODUCTION)(
        patch(urlMatching(s".*/application/${applicationId.value}/dispatch"))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withStatus(IM_A_TEAPOT)
          )
      )

      intercept[InternalServerException] {
        await(connector.dispatch(applicationId, request))
      }.message shouldBe (s"Failed calling dispatch 418")
    }

    "handle TPA returning something we don't understand" in new CollaboratorSetup with PrincipalSetup {

      stubFor(Environment.PRODUCTION)(
        patch(urlMatching(s".*/application/${applicationId.value}/dispatch"))
          .withJsonRequestBody(request)
          .willReturn(
            aResponse()
              .withBody("""{ "bobbins": "true" }""")
              .withStatus(OK)
          )
      )

      intercept[InternalServerException] {
        await(connector.dispatch(applicationId, request))
      }.message shouldBe (s"Failed parsing response to dispatch")
    }
  }
}
