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

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationResponse
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.Collaborators.Developer
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.thirdpartyorchestrator.commands.applications.connectors.EnvironmentAwareAppCmdConnector
import uk.gov.hmrc.thirdpartyorchestrator.commands.applications.mocks.CommandConnectorMockModule
import uk.gov.hmrc.thirdpartyorchestrator.mocks.services.ApplicationFetcherMockModule
import uk.gov.hmrc.thirdpartyorchestrator.utils.{ApplicationBuilder, AsyncHmrcSpec}

class AppCmdControllerSpec extends AsyncHmrcSpec with FixedClock {

  trait Setup
      extends ApplicationFetcherMockModule
      with ApplicationBuilder
      with CommandConnectorMockModule {

    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    val clientId: ClientId                    = ClientId("Some ID")
    val sandboxApplicationId: ApplicationId   = ApplicationId.random

    val sandboxApplication: ApplicationResponse =
      buildApplication(applicationId = sandboxApplicationId, clientId, UserId.random, UserId.random).copy(deployedTo = Environment.SANDBOX)
    val productionApplicationId: ApplicationId  = ApplicationId.random

    val productionApplication: ApplicationResponse =
      buildApplication(applicationId = productionApplicationId, clientId, UserId.random, UserId.random).copy(deployedTo = Environment.PRODUCTION)

    val adminEmail: LaxEmailAddress        = "admin@example.com".toLaxEmail
    val developerAsCollaborator: Developer = Developer(UserId.random, "dev@example.com".toLaxEmail)
    val verifiedEmails                     = Set.empty[LaxEmailAddress]

    val envAwareCmdConnector = new EnvironmentAwareAppCmdConnector(CommandConnectorMocks.Sandbox.aMock, CommandConnectorMocks.Prod.aMock)

    val controller: AppCmdController =
      new AppCmdController(ApplicationFetcherMock.aMock, envAwareCmdConnector, Helpers.stubControllerComponents())
  }

  "AppCmdController" should {
    import cats.syntax.option._

    "dont preprocess or dispatch command when the app does not exist" in new Setup {

      ApplicationFetcherMock.FetchApplication.thenReturn(productionApplicationId)(None)

      val cmd: ApplicationCommands.AddCollaborator = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, instant)
      val request: FakeRequest[JsValue]            =
        FakeRequest("PATCH", s"/applications/${productionApplicationId.value}/dispatch").withBody(Json.toJson(DispatchRequest(cmd, verifiedEmails)))

      status(controller.dispatch(productionApplicationId)(request)) shouldBe BAD_REQUEST

      CommandConnectorMocks.Sandbox.IssueCommand.verifyNoCommandsIssued()
      CommandConnectorMocks.Prod.IssueCommand.verifyNoCommandsIssued()
    }

    "dispatch a command when the app exists in sandbox" in new Setup {
      ApplicationFetcherMock.FetchApplication.thenReturn(sandboxApplicationId)(sandboxApplication.some)

      CommandConnectorMocks.Sandbox.IssueCommand.Dispatch.succeedsWith(sandboxApplication)

      val cmd: ApplicationCommands.AddCollaborator = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, instant)
      val inboundDispatchRequest: DispatchRequest  = DispatchRequest(cmd, verifiedEmails)
      val request: FakeRequest[JsValue]            = FakeRequest("PATCH", s"/applications/${sandboxApplicationId.value}/dispatch").withBody(Json.toJson(inboundDispatchRequest))

      status(controller.dispatch(sandboxApplicationId)(request)) shouldBe OK

      CommandConnectorMocks.Prod.IssueCommand.verifyNoCommandsIssued()
      CommandConnectorMocks.Sandbox.IssueCommand.verifyCalledWith(cmd, verifiedEmails)
    }

    "dispatch a command when the app exists in production" in new Setup {
      ApplicationFetcherMock.FetchApplication.thenReturn(productionApplicationId)(productionApplication.some)

      CommandConnectorMocks.Prod.IssueCommand.Dispatch.succeedsWith(productionApplication)

      val cmd: ApplicationCommands.AddCollaborator = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, instant)
      val request: FakeRequest[JsValue]            =
        FakeRequest("PATCH", s"/applications/${productionApplicationId.value}/dispatch").withBody(Json.toJson(DispatchRequest(cmd, verifiedEmails)))

      status(controller.dispatch(productionApplicationId)(request)) shouldBe OK

      CommandConnectorMocks.Sandbox.IssueCommand.verifyNoCommandsIssued()
      CommandConnectorMocks.Prod.IssueCommand.verifyCalledWith(cmd, verifiedEmails)
    }

    "dispatch a command and handle command failure" in new Setup {
      ApplicationFetcherMock.FetchApplication.thenReturn(productionApplicationId)(productionApplication.some)

      CommandConnectorMocks.Prod.IssueCommand.Dispatch.failsWith(CommandFailures.ActorIsNotACollaboratorOnApp)

      val cmd: ApplicationCommands.AddCollaborator = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, instant)
      val request: FakeRequest[JsValue]            =
        FakeRequest("PATCH", s"/applications/${productionApplicationId.value}/dispatch").withBody(Json.toJson(DispatchRequest(cmd, verifiedEmails)))

      val result: Future[Result] = controller.dispatch(productionApplicationId)(request)
      status(result) shouldBe BAD_REQUEST

      import uk.gov.hmrc.apiplatform.modules.common.domain.services.NonEmptyListFormatters._
      Json.fromJson[NonEmptyList[CommandFailure]](contentAsJson(result)).get shouldBe NonEmptyList.one(CommandFailures.ActorIsNotACollaboratorOnApp)

      CommandConnectorMocks.Prod.IssueCommand.verifyCalledWith(cmd, verifiedEmails)
      CommandConnectorMocks.Sandbox.IssueCommand.verifyNoCommandsIssued()
    }

    "dispatch a command and handle unauthorised command" in new Setup {
      ApplicationFetcherMock.FetchApplication.thenReturn(productionApplicationId)(productionApplication.some)

      CommandConnectorMocks.Prod.IssueCommand.Dispatch.throwsUnauthorised()

      val cmd: ApplicationCommands.AddCollaborator = ApplicationCommands.AddCollaborator(Actors.AppCollaborator(adminEmail), developerAsCollaborator, instant)
      val request: FakeRequest[JsValue]            =
        FakeRequest("PATCH", s"/applications/${productionApplicationId.value}/dispatch").withBody(Json.toJson(DispatchRequest(cmd, verifiedEmails)))

      val result: Future[Result] = controller.dispatch(productionApplicationId)(request)
      status(result) shouldBe UNAUTHORIZED
    }
  }
}
