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

package uk.gov.hmrc.thirdpartyorchestrator.services

import scala.concurrent.ExecutionContext.Implicits.global

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationResponse
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, UserId}
import uk.gov.hmrc.thirdpartyorchestrator.mocks.connectors._
import uk.gov.hmrc.thirdpartyorchestrator.utils.{ApplicationBuilder, AsyncHmrcSpec}

class ApplicationByIdFetcherSpec extends AsyncHmrcSpec {

  implicit val hc = HeaderCarrier()

  trait Setup extends ThirdPartyApplicationConnectorMockModule with MockitoSugar
      with ArgumentMatchersSugar with ApplicationBuilder {

    val applicationId: ApplicationId     = ApplicationId.random
    val clientId: ClientId               = ClientId.random
    val userId1: UserId                  = UserId.random
    val userId2: UserId                  = UserId.random
    val application: ApplicationResponse = buildApplication(applicationId, clientId, userId1, userId2)
    val exception                        = new RuntimeException("error")

    val fetcher = new ApplicationFetcher(
      EnvironmentAwareThirdPartyApplicationConnectorMock.instance
    )
  }

  "ApplicationByIdFetcher" when {
    "fetchApplication is called" should {
      "return None if absent from principal and subordinate" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplication.thenReturnNone(applicationId)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplication.thenReturnNone(applicationId)

        await(fetcher.fetchApplication(applicationId)) shouldBe None
      }

      "return an application from subordinate if present" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplication.thenReturn(applicationId)(Some(application))
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplication.thenReturnNone(applicationId)

        await(fetcher.fetchApplication(applicationId)) shouldBe Some(application)
      }

      "return an application from principal if present" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplication.thenReturnNone(applicationId)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplication.thenReturn(applicationId)(Some(application))
        await(fetcher.fetchApplication(applicationId)) shouldBe Some(application)
      }

      "return an application from principal if present even when subordinate throws" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplication.thenThrowException(applicationId)(exception)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplication.thenReturn(applicationId)(Some(application))
        await(fetcher.fetchApplication(applicationId)) shouldBe Some(application)
      }

      "return an exception if principal throws even if subordinate has the application" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplication.thenReturn(applicationId)(Some(application))
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplication.thenThrowException(applicationId)(exception)
        intercept[Exception] {
          await(fetcher.fetchApplication(applicationId)) shouldBe Some(application)
        }.shouldBe(exception)
      }
    }

    "fetchApplicationByClientIdId is called" should {
      "return None if absent from principal and subordinate" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationByClientId.thenReturnNone(clientId)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationByClientId.thenReturnNone(clientId)

        await(fetcher.fetchApplicationByClientId(clientId)) shouldBe None
      }

      "return an application from subordinate if present" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationByClientId.thenReturn(clientId)(Some(application))
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationByClientId.thenReturnNone(clientId)

        await(fetcher.fetchApplicationByClientId(clientId)) shouldBe Some(application)
      }

      "return an application from principal if present" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationByClientId.thenReturnNone(clientId)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationByClientId.thenReturn(clientId)(Some(application))
        await(fetcher.fetchApplicationByClientId(clientId)) shouldBe Some(application)
      }

      "return an application from principal if present even when subordinate throws" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationByClientId.thenThrowException(clientId)(exception)
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationByClientId.thenReturn(clientId)(Some(application))
        await(fetcher.fetchApplicationByClientId(clientId)) shouldBe Some(application)
      }

      "return an exception if principal throws even if subordinate has the application" in new Setup {
        EnvironmentAwareThirdPartyApplicationConnectorMock.Subordinate.FetchApplicationByClientId.thenReturn(clientId)(Some(application))
        EnvironmentAwareThirdPartyApplicationConnectorMock.Principal.FetchApplicationByClientId.thenThrowException(clientId)(exception)
        intercept[Exception] {
          await(fetcher.fetchApplicationByClientId(clientId)) shouldBe Some(application)
        }.shouldBe(exception)
      }
    }
  }
}
