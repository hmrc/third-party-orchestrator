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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.thirdpartyorchestrator.domain.models.developers.Developer
import uk.gov.hmrc.thirdpartyorchestrator.services.ApplicationService

trait ApplicationServiceMock extends MockitoSugar with ArgumentMatchersSugar {
  val applicationServiceMock = mock[ApplicationService]

  private def fetchVerifiedCollaboratorsForApplication(applicationId: ApplicationId, returns: Set[Developer]) =
    when(applicationServiceMock.fetchVerifiedCollaboratorsForApplication(eqTo(applicationId))(*)).thenReturn(successful(Right(returns)))

  def fetchVerifiedCollaboratorsForApplicationReturns(applicationId: ApplicationId, returns: Set[Developer]) =
    fetchVerifiedCollaboratorsForApplication(applicationId, returns)

  def fetchVerifiedCollaboratorsForApplicationReturnsNone(applicationId: ApplicationId) =
    fetchVerifiedCollaboratorsForApplication(applicationId, Set.empty)

  def fetchVerifiedCollaboratorsForApplicationNotFound(applicationId: ApplicationId) =
    when(applicationServiceMock.fetchVerifiedCollaboratorsForApplication(eqTo(applicationId))(*)).thenReturn(successful(Left("Application not found")))
}