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

import cats.data.OptionT

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits._

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.GetAppsForAdminOrRIRequest
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.Param.{ExcludeDeletedQP, UserIdsQP}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, UserId}
import uk.gov.hmrc.thirdpartyorchestrator.connectors.{EnvironmentAwareQueryConnector, EnvironmentAwareThirdPartyApplicationConnector}
import uk.gov.hmrc.thirdpartyorchestrator.utils.ApplicationLogger

@Singleton
class ApplicationFetcher @Inject() (
    thirdPartyApplicationConnector: EnvironmentAwareThirdPartyApplicationConnector,
    queryConnector: EnvironmentAwareQueryConnector
  )(implicit ec: ExecutionContext
  ) extends ApplicationLogger {

  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    val qry = ApplicationQuery.ById(applicationId, Nil)
    OptionT(queryConnector.principal.query[Option[ApplicationWithCollaborators]](qry))
      .orElseF(queryConnector.subordinate.query[Option[ApplicationWithCollaborators]](qry) recover recoverWithDefault(None))
      .value
  }

  def fetchApplication(clientId: ClientId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    val qry = ApplicationQuery.ByClientId(clientId, false, Nil)
    OptionT(queryConnector.principal.query[Option[ApplicationWithCollaborators]](qry))
      .orElseF(queryConnector.subordinate.query[Option[ApplicationWithCollaborators]](qry) recover recoverWithDefault(None))
      .value
  }

  def fetchApplicationsByUserIds(userIds: List[UserId])(implicit hc: HeaderCarrier): Future[List[ApplicationWithCollaborators]] = {
    if (userIds.isEmpty)
      Future.successful(Nil)
    else {
      val qry                                                         = ApplicationQuery.GeneralOpenEndedApplicationQuery(List(UserIdsQP(userIds), ExcludeDeletedQP))
      val subordinateApps: Future[List[ApplicationWithCollaborators]] = queryConnector.subordinate.query[List[ApplicationWithCollaborators]](qry) recover recoverWithDefault(Nil)
      val principalApps: Future[List[ApplicationWithCollaborators]]   = queryConnector.principal.query[List[ApplicationWithCollaborators]](qry)

      for {
        subordinates <- subordinateApps
        principals   <- principalApps
      } yield principals ++ subordinates
    }
  }

  def getAppsForResponsibleIndividualOrAdmin(request: GetAppsForAdminOrRIRequest)(implicit hc: HeaderCarrier): Future[List[ApplicationWithCollaborators]] = {
    if (request.adminOrRespIndEmail.text.nonEmpty) {
      val subordinateApp: Future[List[ApplicationWithCollaborators]] =
        thirdPartyApplicationConnector.subordinate.getAppsForResponsibleIndividualOrAdmin(request) recover recoverWithDefault(Nil)
      val principalApp: Future[List[ApplicationWithCollaborators]]   = thirdPartyApplicationConnector.principal.getAppsForResponsibleIndividualOrAdmin(request)

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
