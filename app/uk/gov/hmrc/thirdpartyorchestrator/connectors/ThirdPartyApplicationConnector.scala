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

package uk.gov.hmrc.thirdpartyorchestrator.connectors

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{StringContextOps, _}
import uk.gov.hmrc.play.http.metrics.common._

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, PaginatedApplications}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId, UserId}
import uk.gov.hmrc.thirdpartyorchestrator.utils.EbridgeConfigurator

case class CollaboratorUserIds(userIds: List[UserId])

object CollaboratorUserIds {
  implicit val format: OFormat[CollaboratorUserIds] = Json.format[CollaboratorUserIds]
}

trait ThirdPartyApplicationConnector {
  def searchApplications(queryString: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[PaginatedApplications]

  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]]
  def fetchApplication(clientId: ClientId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]]
  def fetchApplicationsByUserIds(userIds: List[UserId])(implicit hc: HeaderCarrier): Future[List[ApplicationWithCollaborators]]
}

abstract class AbstractThirdPartyApplicationConnector(implicit val ec: ExecutionContext) extends ThirdPartyApplicationConnector with RecordMetrics {

  protected val serviceBaseUrl: String
  val apiMetrics: ApiMetrics

  def http: HttpClientV2

  val api = API("third-party-application")

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder

  def searchApplications(queryString: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[PaginatedApplications] =
    record {
      val queryStringFirstVal: Seq[(String, String)] = queryString.map {
        case (k, vs) => (k, vs.head)
      }.toSeq

      configureEbridgeIfRequired(http.get(url"$serviceBaseUrl/applications?$queryStringFirstVal"))
        .execute[PaginatedApplications]
    }

  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] =
    record {
      configureEbridgeIfRequired(http.get(url"$serviceBaseUrl/application/$applicationId"))
        .execute[Option[ApplicationWithCollaborators]]
    }

  def fetchApplication(clientId: ClientId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] =
    record {
      val params = Seq("clientId" -> clientId.value)

      configureEbridgeIfRequired(http.get(url"$serviceBaseUrl/application?$params"))
        .execute[Option[ApplicationWithCollaborators]]
    }

  def fetchApplicationsByUserIds(userIds: List[UserId])(implicit hc: HeaderCarrier): Future[List[ApplicationWithCollaborators]] =
    record {
      configureEbridgeIfRequired(http.post(url"$serviceBaseUrl/developer/applications"))
        .withBody(Json.toJson(CollaboratorUserIds(userIds)))
        .execute[List[ApplicationWithCollaborators]]
    }
}

object PrincipalThirdPartyApplicationConnector {

  case class Config(
      serviceBaseUrl: String
    )
}

@Singleton
@Named("principal")
class PrincipalThirdPartyApplicationConnector @Inject() (
    val config: PrincipalThirdPartyApplicationConnector.Config,
    val http: HttpClientV2,
    val apiMetrics: ApiMetrics
  )(implicit override val ec: ExecutionContext
  ) extends AbstractThirdPartyApplicationConnector {

  val serviceBaseUrl = config.serviceBaseUrl

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder = requestBuilder
}

object SubordinateThirdPartyApplicationConnector {

  case class Config(
      serviceBaseUrl: String,
      useProxy: Boolean,
      bearerToken: String,
      apiKey: String
    )
}

@Singleton
@Named("subordinate")
class SubordinateThirdPartyApplicationConnector @Inject() (
    val config: SubordinateThirdPartyApplicationConnector.Config,
    val http: HttpClientV2,
    val apiMetrics: ApiMetrics
  )(implicit override val ec: ExecutionContext
  ) extends AbstractThirdPartyApplicationConnector {

  val serviceBaseUrl: String = config.serviceBaseUrl

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder =
    EbridgeConfigurator.configure(config.useProxy, config.bearerToken, config.apiKey)(requestBuilder)
}

@Singleton
class EnvironmentAwareThirdPartyApplicationConnector @Inject() (
    @Named("subordinate") val subordinate: ThirdPartyApplicationConnector,
    @Named("principal") val principal: ThirdPartyApplicationConnector
  ) extends EnvironmentAware[ThirdPartyApplicationConnector]
