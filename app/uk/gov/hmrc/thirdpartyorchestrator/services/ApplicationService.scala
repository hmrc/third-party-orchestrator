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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.CreateApplicationRequest
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import uk.gov.hmrc.apiplatform.modules.tpd.core.domain.models.User
import uk.gov.hmrc.thirdpartyorchestrator.connectors.{EnvironmentAwareThirdPartyApplicationConnector, ThirdPartyDeveloperConnector}

@Singleton
class ApplicationService @Inject() (
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    val applicationFetcher: ApplicationFetcher,
    val thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector
  )(implicit val ec: ExecutionContext
  ) {

  def createApplication(request: CreateApplicationRequest)(implicit hc: HeaderCarrier): Future[ApplicationWithCollaborators] = {
    thirdPartyApplicationConnector(request.environment).create(request)
  }

  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    applicationFetcher.fetchApplication(applicationId)
  }

  def fetchApplication(clientId: ClientId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    applicationFetcher.fetchApplication(clientId)
  }

  def fetchApplicationsForEmails(emails: List[LaxEmailAddress])(implicit hc: HeaderCarrier): Future[List[ApplicationWithCollaborators]] = {
    for {
      developers              <- thirdPartyDeveloperConnector.fetchDevelopers(emails)
      verifiedDeveloperUserIds = developers.filter(_.verified).map(_.userId)
      applications            <- applicationFetcher.fetchApplicationsByUserIds(verifiedDeveloperUserIds)
    } yield applications
  }

  def fetchApplicationsByUserIds(userIds: List[UserId])(implicit hc: HeaderCarrier): Future[List[ApplicationWithCollaborators]] = {
    for {
      applications <- applicationFetcher.fetchApplicationsByUserIds(userIds)
    } yield applications
  }

  def fetchVerifiedCollaboratorsForApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Either[String, Set[User]]] = {
    val E = EitherTHelper.make[String]
    (
      for {
        application       <- E.fromOptionF(applicationFetcher.fetchApplication(applicationId), "Application not found")
        allDevelopers     <- E.liftF(Future.sequence(application.collaborators.map(collaborator => thirdPartyDeveloperConnector.fetchDeveloper(collaborator.userId))))
        verifiedDevelopers = allDevelopers.flatten.filter(developer => developer.verified)
      } yield verifiedDevelopers
    )
      .value
  }
}
