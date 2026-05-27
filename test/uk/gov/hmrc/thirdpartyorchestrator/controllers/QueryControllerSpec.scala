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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.libs.json.{JsArray, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HttpResponse

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, ApplicationWithCollaboratorsFixtures}
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.QueriedApplication
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ParamNames
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Environment
import uk.gov.hmrc.thirdpartyorchestrator.mocks.connectors.QueryConnectorMockModule

class QueryControllerSpec extends BaseControllerSpec with ApplicationWithCollaboratorsFixtures {

  trait SetupPairedEnvironment extends QueryConnectorMockModule {
    val connectorMock = EnvironmentAwareQueryConnectorMock.instance
    val underTest     = new QueryController(connectorMock, appConfig, Helpers.stubControllerComponents())
    when(appConfig.inPairedEnvironment).thenReturn(true)
  }

  trait SetupSingleEnvironment extends QueryConnectorMockModule {
    val connectorMock = EnvironmentAwareQueryConnectorMock.instance
    val underTest     = new QueryController(connectorMock, appConfig, Helpers.stubControllerComponents())
    when(appConfig.inPairedEnvironment).thenReturn(false)
  }

  def asOkResponse(apps: ApplicationWithCollaborators*): HttpResponse = {
    HttpResponse(OK, Json.toJson(apps.toList.map(QueriedApplication.apply)).toString)
  }

  def asSingleAppOkResponse(app: ApplicationWithCollaborators): HttpResponse = {
    HttpResponse(OK, Json.toJson(QueriedApplication(app)).toString)
  }

  def ensureFailure(result: Future[Result], statusCode: Int, code: String, message: String) = {
    status(result) shouldBe statusCode
    contentAsString(result) should include(s""""code":"$code"""")
    contentAsString(result) should include(message)
  }

  def ensureBadRequest(result: Future[Result], code: String, message: String) = {
    ensureFailure(result, BAD_REQUEST, code, message)
  }

  def ensureNotFound(result: Future[Result], code: String, message: String) = {
    ensureFailure(result, NOT_FOUND, code, message)
  }

  "queryEnv" should {
    "issue query to the sandbox environment" in new SetupPairedEnvironment {
      SubordinateQueryConnectorMock.ByQueryParams.returns(asOkResponse(standardApp))

      val request = FakeRequest("GET", "/environment/SANDBOX/query")
      val result  = underTest.queryEnv(Environment.SANDBOX)(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(List(standardApp))
    }

    "issue query to the production environment" in new SetupPairedEnvironment {
      PrincipalQueryConnectorMock.ByQueryParams.returns(asOkResponse(standardApp))

      val request = FakeRequest("GET", "/environment/PRODUCTION/query")
      val result  = underTest.queryEnv(Environment.PRODUCTION)(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(List(standardApp))
    }

    "issue query testing effectiveParams as if in Staging or local env" in new SetupSingleEnvironment {
      PrincipalQueryConnectorMock.ByQueryParams.returnsFor(Map(ParamNames.Environment -> "PRODUCTION"), asOkResponse(standardApp))

      val request = FakeRequest("GET", "/environment/PRODUCTION/query")
      val result  = underTest.queryEnv(Environment.PRODUCTION)(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(List(standardApp))
    }

    "fail when query params contain environment" in new SetupPairedEnvironment {
      val request = FakeRequest("GET", "/environment/PRODUCTION/query?environment=PRODUCTION")
      val result  = underTest.queryEnv(Environment.PRODUCTION)(request)

      ensureBadRequest(result, "UNEXPECTED_PARAMETER", "Cannot provide an environment query parameter when using environment path parameter")
    }
  }

  "query in general" should {
    "when invalid environment param is specified return BAD_REQUEST" in new SetupPairedEnvironment {
      val request = FakeRequest("GET", "/query?environment=BOBBINS")
      val result  = underTest.query()(request)

      ensureBadRequest(result, "INVALID_QUERY", "is not a valid environment")
    }

    "when environment param is specified call queryEnv" in new SetupPairedEnvironment {
      PrincipalQueryConnectorMock.ByQueryParams.returns(asOkResponse(standardApp))

      val request = FakeRequest("GET", "/query?environment=PRODUCTION")
      val result  = underTest.query()(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(List(standardApp))
    }

    "ensure paginated queries for both environments return BAD_REQUEST" in new SetupPairedEnvironment {
      val request = FakeRequest("GET", "/query?pageNbr=1")
      val result  = underTest.query()(request)

      ensureBadRequest(result, "UNEXPECTED_PARAMETER", "Cannot request paginated queries across both environments")
    }
  }

  "query for single app queries" should {
    def asNone(): Either[HttpResponse, Option[QueriedApplication]] = {
      Left(HttpResponse(404, "Blah"))
    }

    def asAnAppResponse(app: ApplicationWithCollaborators): Either[HttpResponse, QueriedApplication] = {
      Right(QueriedApplication(app))
    }

    "ensure single application queries for both environments return zero apps when not found" in new SetupPairedEnvironment {
      val request = FakeRequest("GET", s"/query?applicationId=$applicationIdOne")
      PrincipalQueryConnectorMock.ByQueryParams.returnsFor(Map(ParamNames.ApplicationId -> s"$applicationIdOne"), asNone())
      SubordinateQueryConnectorMock.ByQueryParams.returnsFor(Map(ParamNames.ApplicationId -> s"$applicationIdOne"), asNone())

      val result = underTest.query()(request)

      ensureNotFound(result, "APPLICATION_NOT_FOUND", "No application found for query")
    }

    "ensure single application queries production only when app is found" in new SetupPairedEnvironment {
      val request = FakeRequest("GET", s"/query?applicationId=$applicationIdOne")
      PrincipalQueryConnectorMock.ByQueryParams.returnsFor(Map(ParamNames.ApplicationId -> s"$applicationIdOne"), asAnAppResponse(standardApp))

      val result = underTest.query()(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(standardApp)
    }

    "ensure single application queries both environments when app is found in sandbox" in new SetupPairedEnvironment {
      val request = FakeRequest("GET", s"/query?applicationId=$applicationIdOne")
      PrincipalQueryConnectorMock.ByQueryParams.returnsFor(Map(ParamNames.ApplicationId -> s"$applicationIdOne"), asNone())
      SubordinateQueryConnectorMock.ByQueryParams.returnsFor(Map(ParamNames.ApplicationId -> s"$applicationIdOne"), asAnAppResponse(standardApp))

      val result = underTest.query()(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(standardApp)
    }
  }

  "query for open ended app queries" should {
    def asEmptyList(): Either[HttpResponse, List[QueriedApplication]] = {
      Right(Nil)
    }

    def asAppsResponse(apps: ApplicationWithCollaborators*): Either[HttpResponse, List[QueriedApplication]] = {
      Right(apps.toList.map(QueriedApplication(_)))
    }

    "ensure open ended application queries for both environments return empty list when not found" in new SetupPairedEnvironment {
      val request = FakeRequest("GET", s"/query?userId=$userIdOne")
      PrincipalQueryConnectorMock.ByQueryParams.returnsFor(Map(ParamNames.UserId -> s"$userIdOne"), asEmptyList())
      SubordinateQueryConnectorMock.ByQueryParams.returnsFor(Map(ParamNames.UserId -> s"$userIdOne"), asEmptyList())

      val result = underTest.query()(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe JsArray(Nil)
    }

    "ensure open ended application queries both environments even if no apps are found in production" in new SetupPairedEnvironment {
      val request = FakeRequest("GET", s"/query?userId=$userIdOne")
      PrincipalQueryConnectorMock.ByQueryParams.returnsFor(Map(ParamNames.UserId -> s"$userIdOne"), asAppsResponse(standardApp))
      SubordinateQueryConnectorMock.ByQueryParams.returnsFor(Map(ParamNames.UserId -> s"$userIdOne"), asAppsResponse(standardApp2))

      val result = underTest.query()(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(List(standardApp, standardApp2))
    }

    "ensure open ended application queries both environments and survives a sandbox failure" in new SetupPairedEnvironment {
      val request = FakeRequest("GET", s"/query?userId=$userIdOne")
      PrincipalQueryConnectorMock.ByQueryParams.returnsFor(Map(ParamNames.UserId -> s"$userIdOne"), asAppsResponse(standardApp, standardApp2))
      SubordinateQueryConnectorMock.ByQueryParams.fails(new RuntimeException("Pretend we get a gateway exception"))

      val result = underTest.query()(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(List(standardApp, standardApp2))
    }

    "ensure open ended application queries both environments when app is found in sandbox" in new SetupPairedEnvironment {
      val request = FakeRequest("GET", s"/query?userId=$userIdOne")
      PrincipalQueryConnectorMock.ByQueryParams.returnsFor(Map(ParamNames.UserId -> s"$userIdOne"), asEmptyList())
      SubordinateQueryConnectorMock.ByQueryParams.returnsFor(Map(ParamNames.UserId -> s"$userIdOne"), asAppsResponse(standardApp, standardApp2))

      val result = underTest.query()(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(List(standardApp, standardApp2))
    }
  }
}
