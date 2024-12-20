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
import scala.util.control.NonFatal

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, UserId}
import uk.gov.hmrc.thirdpartyorchestrator.connectors.EnvironmentAwareThirdPartyApplicationConnector
import uk.gov.hmrc.thirdpartyorchestrator.utils.ApplicationLogger

@Singleton
class ApplicationFetcher @Inject() (
    thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector
  )(implicit ec: ExecutionContext
  ) extends ApplicationLogger {

  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    val subordinateApp: Future[Option[ApplicationWithCollaborators]] = thirdPartyApplicationConnector.subordinate.fetchApplication(applicationId) recover recoverWithDefault(None)
    val principalApp: Future[Option[ApplicationWithCollaborators]]   = thirdPartyApplicationConnector.principal.fetchApplication(applicationId)

    for {
      subordinate <- subordinateApp
      principal   <- principalApp
    } yield principal.orElse(subordinate)
  }

  def fetchApplication(clientId: ClientId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    val subordinateApp: Future[Option[ApplicationWithCollaborators]] = thirdPartyApplicationConnector.subordinate.fetchApplication(clientId) recover recoverWithDefault(None)
    val principalApp: Future[Option[ApplicationWithCollaborators]]   = thirdPartyApplicationConnector.principal.fetchApplication(clientId)

    for {
      subordinate <- subordinateApp
      principal   <- principalApp
    } yield principal.orElse(subordinate)
  }

  def fetchApplicationsByUserIds(userIds: List[UserId], params: Map[String, String])(implicit hc: HeaderCarrier): Future[List[ApplicationWithCollaborators]] = {
    if (userIds.nonEmpty) {
      val subordinateApp: Future[List[ApplicationWithCollaborators]] = thirdPartyApplicationConnector.subordinate.fetchApplicationsByUserIds(userIds, params)
      val principalApp: Future[List[ApplicationWithCollaborators]]   = thirdPartyApplicationConnector.principal.fetchApplicationsByUserIds(userIds, params)

      for {
        subordinate <- subordinateApp
        principal   <- principalApp
      } yield principal ++ subordinate
    } else {
      Future.successful(List.empty)
    }
  }

  private def recoverWithDefault[T](default: T): PartialFunction[Throwable, T] = {
    case NonFatal(e) =>
      logger.warn(s"Error occurred fetching application: ${e.getMessage}", e)
      default
  }
}
