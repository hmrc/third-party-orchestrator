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

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}
import uk.gov.hmrc.play.http.metrics.common.API

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationResponse
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.thirdpartyorchestrator.utils.ProxiedHttpClient

trait ThirdPartyApplicationConnector {
  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationResponse]]
}

abstract class AbstractThirdPartyApplicationConnector(implicit val ec: ExecutionContext) extends ThirdPartyApplicationConnector {

  protected val httpClient: HttpClient
  protected val serviceBaseUrl: String
  protected val metrics: ConnectorMetrics

  def http: HttpClient

  val api = API("third-party-application")

  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationResponse]] =
    metrics.record(api) {
      http.GET[Option[ApplicationResponse]](s"$serviceBaseUrl/application/$applicationId")
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
    val httpClient: HttpClient,
    val metrics: ConnectorMetrics
  )(implicit override val ec: ExecutionContext
  ) extends AbstractThirdPartyApplicationConnector {

  val http: HttpClient = httpClient
  val serviceBaseUrl   = config.serviceBaseUrl
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
    val httpClient: HttpClient,
    val proxiedHttpClient: ProxiedHttpClient,
    val metrics: ConnectorMetrics
  )(implicit override val ec: ExecutionContext
  ) extends AbstractThirdPartyApplicationConnector {

  val serviceBaseUrl: String = config.serviceBaseUrl
  val http: HttpClient       = if (config.useProxy) proxiedHttpClient.withHeaders(config.bearerToken, config.apiKey) else httpClient
}

@Singleton
class EnvironmentAwareThirdPartyApplicationConnector @Inject() (
    @Named("subordinate") val subordinate: ThirdPartyApplicationConnector,
    @Named("principal") val principal: ThirdPartyApplicationConnector
  ) extends EnvironmentAware[ThirdPartyApplicationConnector]
