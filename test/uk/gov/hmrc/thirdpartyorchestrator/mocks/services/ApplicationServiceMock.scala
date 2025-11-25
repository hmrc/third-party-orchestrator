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

import scala.concurrent.Future._

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.UpstreamErrorResponse

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.CreateApplicationRequest
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, LaxEmailAddress}
import uk.gov.hmrc.apiplatform.modules.tpd.core.domain.models.User
import uk.gov.hmrc.thirdpartyorchestrator.services.ApplicationService

trait ApplicationServiceMock extends MockitoSugar with ArgumentMatchersSugar {
  val applicationServiceMock = mock[ApplicationService]

  def createApplicationReturns(appCreateRequest: CreateApplicationRequest, returns: ApplicationWithCollaborators) =
    when(applicationServiceMock.createApplication(eqTo(appCreateRequest))(*)).thenReturn(successful(returns))

  def fetchApplicationsForEmailReturns(emails: List[LaxEmailAddress], returns: ApplicationWithCollaborators) =
    when(applicationServiceMock.fetchApplicationsForEmails(eqTo(emails))(*)).thenReturn(successful(List(returns)))

  def fetchApplicationsForEmailFails() =
    when(applicationServiceMock.fetchApplicationsForEmails(*)(*)).thenReturn(failed(UpstreamErrorResponse("some problem happened", 500)))

  def fetchVerifiedCollaboratorsForApplicationReturns(applicationId: ApplicationId, returns: Set[User]) =
    when(applicationServiceMock.fetchVerifiedCollaboratorsForApplication(eqTo(applicationId))(*)).thenReturn(successful(Right(returns)))

  def fetchVerifiedCollaboratorsForApplicationNotFound(applicationId: ApplicationId) =
    when(applicationServiceMock.fetchVerifiedCollaboratorsForApplication(eqTo(applicationId))(*)).thenReturn(successful(Left("Application not found")))
}
