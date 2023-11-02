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

package uk.gov.hmrc.thirdpartyorchestrator.mocks.services

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Application
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.thirdpartyorchestrator.services.ApplicationByIdFetcher

trait ApplicationByIdFetcherMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait AbstractApplicationByIdFetcherMock {
    def aMock: ApplicationByIdFetcher

    object FetchApplication {

      def thenReturn(applicationId: ApplicationId)(application: Option[Application]) =
        when(aMock.fetchApplication(eqTo(applicationId))(*)).thenReturn(successful(application))
    }
  }

  object ApplicationByIdFetcherMock extends AbstractApplicationByIdFetcherMock {
    val aMock = mock[ApplicationByIdFetcher]
  }
}