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

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Application
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.Developer
import uk.gov.hmrc.thirdpartyorchestrator.connectors.ThirdPartyDeveloperConnector

object ApplicationService {
  case class GetApplicationResult(application: Application, developers: Set[Developer])
}

@Singleton
class ApplicationService @Inject() (
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector,
    val applicationByIdFetcher: ApplicationByIdFetcher
  )(implicit val ec: ExecutionContext
  ) extends EitherTHelper[String] {

  import ApplicationService._

  def fetchVerifiedCollaboratorsForApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Either[String, GetApplicationResult]] = {
    (
      for {
        application       <- fromOptionF(applicationByIdFetcher.fetchApplication(applicationId), "Application not found")
        allDevelopers     <- liftF(Future.sequence(application.collaborators.map(collaborator => thirdPartyDeveloperConnector.fetchDeveloper(collaborator.userId))))
        verifiedDevelopers = allDevelopers.flatten.filter(developer => developer.verified)
      } yield GetApplicationResult(application, verifiedDevelopers)
    )
      .value
  }
}
