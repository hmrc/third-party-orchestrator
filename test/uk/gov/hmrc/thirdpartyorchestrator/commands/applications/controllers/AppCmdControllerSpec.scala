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

package uk.gov.hmrc.thirdpartyorchestrator.commands.applications.controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import cats.data.NonEmptyList

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.Collaborators.Developer
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, ApplicationWithCollaboratorsFixtures}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.thirdpartyorchestrator.commands.applications.connectors.EnvironmentAwareAppCmdConnector
import uk.gov.hmrc.thirdpartyorchestrator.commands.applications.mocks.CommandConnectorMockModule
import uk.gov.hmrc.thirdpartyorchestrator.mocks.services.ApplicationFetcherMockModule
import uk.gov.hmrc.thirdpartyorchestrator.utils.AsyncHmrcSpec

class AppCmdControllerSpec extends AsyncHmrcSpec with FixedClock with ApplicationWithCollaboratorsFixtures {

  trait Setup
      extends ApplicationFetcherMockModule
      with CommandConnectorMockModule {

    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    val clientId: ClientId                    = clientIdTwo
    val sandboxApplicationId: ApplicationId   = applicationIdTwo

    val sandboxApplication: ApplicationWithCollaborators = standardApp.withEnvironment(Environment.SANDBOX)
    val productionApplicationId: ApplicationId           = applicationIdOne

    val productionApplication: ApplicationWithCollaborators = standardApp

    val adminEmail: LaxEmailAddress        = "admin@example.com".toLaxEmail
    val developerAsCollaborator: Developer = Developer(UserId.random, "dev@example.com".toLaxEmail)
    val verifiedEmails                     = Set.empty[LaxEmailAddress]

    val envAwareCmdConnector = new EnvironmentAwareAppCmdConnector(CommandConnectorMocks.Sandbox.aMock, CommandConnectorMocks.Prod.aMock)

    val controller: AppCmdController =
      new AppCmdController(ApplicationFetcherMock.aMock, envAwareCmdConnector, Helpers.stubControllerComponents())
  }

  "AppCmdController" when {
    "calling dispatch" should {
      import cats.syntax.option._

      "dont preprocess or dispatch command when the app does not exist" in new Setup {
        ApplicationFetcherMock.FetchApplication.thenReturn(productionApplicationId)(None)

        val cmd: ApplicationCommands.AddCollaborator = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, instant)
        val request: FakeRequest[JsValue]            =
          FakeRequest("PATCH", s"/applications/${productionApplicationId}/dispatch").withBody(Json.toJson(DispatchRequest(cmd, verifiedEmails)))

        status(controller.dispatch(productionApplicationId)(request)) shouldBe BAD_REQUEST

        CommandConnectorMocks.Sandbox.IssueCommand.verifyNoCommandsIssued()
        CommandConnectorMocks.Prod.IssueCommand.verifyNoCommandsIssued()
      }

      "dispatch a command when the app exists in sandbox" in new Setup {
        ApplicationFetcherMock.FetchApplication.thenReturn(sandboxApplicationId)(sandboxApplication.some)

        CommandConnectorMocks.Sandbox.IssueCommand.Dispatch.succeedsWith(sandboxApplication)

        val cmd: ApplicationCommands.AddCollaborator = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, instant)
        println(Json.toJson[ApplicationCommand](cmd))
        val inboundDispatchRequest: DispatchRequest  = DispatchRequest(cmd, verifiedEmails)
        val request: FakeRequest[JsValue]            = FakeRequest("PATCH", s"/applications/${sandboxApplicationId}/dispatch").withBody(Json.toJson(inboundDispatchRequest))

        status(controller.dispatch(sandboxApplicationId)(request)) shouldBe OK

        CommandConnectorMocks.Prod.IssueCommand.verifyNoCommandsIssued()
        CommandConnectorMocks.Sandbox.IssueCommand.verifyCalledWith(cmd, verifiedEmails)
      }

      "dispatch a command when the app exists in production" in new Setup {
        ApplicationFetcherMock.FetchApplication.thenReturn(productionApplicationId)(productionApplication.some)

        CommandConnectorMocks.Prod.IssueCommand.Dispatch.succeedsWith(productionApplication)

        val cmd: ApplicationCommands.AddCollaborator = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, instant)
        val request: FakeRequest[JsValue]            =
          FakeRequest("PATCH", s"/applications/${productionApplicationId}/dispatch").withBody(Json.toJson(DispatchRequest(cmd, verifiedEmails)))

        status(controller.dispatch(productionApplicationId)(request)) shouldBe OK

        CommandConnectorMocks.Sandbox.IssueCommand.verifyNoCommandsIssued()
        CommandConnectorMocks.Prod.IssueCommand.verifyCalledWith(cmd, verifiedEmails)
      }

      "dispatch a command and handle command failure" in new Setup {
        ApplicationFetcherMock.FetchApplication.thenReturn(productionApplicationId)(productionApplication.some)

        CommandConnectorMocks.Prod.IssueCommand.Dispatch.failsWith(CommandFailures.ActorIsNotACollaboratorOnApp)

        val cmd: ApplicationCommands.AddCollaborator = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, instant)
        val request: FakeRequest[JsValue]            =
          FakeRequest("PATCH", s"/applications/${productionApplicationId}/dispatch").withBody(Json.toJson(DispatchRequest(cmd, verifiedEmails)))

        val result: Future[Result] = controller.dispatch(productionApplicationId)(request)
        status(result) shouldBe BAD_REQUEST

        import uk.gov.hmrc.apiplatform.modules.common.domain.services.NonEmptyListFormatters._
        Json.fromJson[NonEmptyList[CommandFailure]](contentAsJson(result)).get shouldBe NonEmptyList.one(CommandFailures.ActorIsNotACollaboratorOnApp)

        CommandConnectorMocks.Prod.IssueCommand.verifyCalledWith(cmd, verifiedEmails)
        CommandConnectorMocks.Sandbox.IssueCommand.verifyNoCommandsIssued()
      }
    }

    "calling dispatchToEnvironment" should {
      "dispatch a command when the app exists in the environment specified" in new Setup {
        CommandConnectorMocks.Sandbox.IssueCommand.Dispatch.succeedsWith(sandboxApplication)

        val cmd: ApplicationCommands.AddCollaborator = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, instant)
        val inboundDispatchRequest: DispatchRequest  = DispatchRequest(cmd, verifiedEmails)
        val request: FakeRequest[JsValue]            = FakeRequest("PATCH", s"/environment/SANDBOX/application/$sandboxApplicationId").withBody(Json.toJson(inboundDispatchRequest))

        status(controller.dispatchToEnvironment(Environment.SANDBOX, sandboxApplicationId)(request)) shouldBe OK

        CommandConnectorMocks.Prod.IssueCommand.verifyNoCommandsIssued()
        CommandConnectorMocks.Sandbox.IssueCommand.verifyCalledWith(cmd, verifiedEmails)
      }
    }
  }
}
