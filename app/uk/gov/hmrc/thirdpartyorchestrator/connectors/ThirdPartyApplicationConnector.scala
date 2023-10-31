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

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Application
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.thirdpartyorchestrator.utils.ProxiedHttpClient

object AbstractThirdPartyApplicationConnector {

  case class Config(
      applicationBaseUrl: String,
      applicationUseProxy: Boolean,
      applicationBearerToken: String,
      applicationApiKey: String
    )
}

trait ThirdPartyApplicationConnector {
  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]]
}

abstract class AbstractThirdPartyApplicationConnector(implicit val ec: ExecutionContext) extends ThirdPartyApplicationConnector {

  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  protected val config: AbstractThirdPartyApplicationConnector.Config
  protected val metrics: ConnectorMetrics
  lazy val serviceBaseUrl: String = config.applicationBaseUrl
  lazy val useProxy: Boolean      = config.applicationUseProxy
  lazy val bearerToken: String    = config.applicationBearerToken
  lazy val apiKey: String         = config.applicationApiKey

  val api              = API("third-party-application")
  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  def fetchApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]] =
    metrics.record(api) {
      http.GET[Option[Application]](s"$serviceBaseUrl/application/$applicationId")
    }
}

@Singleton
@Named("principal")
class PrincipalThirdPartyApplicationConnector @Inject() (
    @Named("principal") override val config: AbstractThirdPartyApplicationConnector.Config,
    override val httpClient: HttpClient,
    override val proxiedHttpClient: ProxiedHttpClient,
    override val metrics: ConnectorMetrics
  )(implicit override val ec: ExecutionContext
  ) extends AbstractThirdPartyApplicationConnector

@Singleton
@Named("subordinate")
class SubordinateThirdPartyApplicationConnector @Inject() (
    @Named("subordinate") override val config: AbstractThirdPartyApplicationConnector.Config,
    override val httpClient: HttpClient,
    override val proxiedHttpClient: ProxiedHttpClient,
    override val metrics: ConnectorMetrics
  )(implicit override val ec: ExecutionContext
  ) extends AbstractThirdPartyApplicationConnector

@Singleton
class EnvironmentAwareThirdPartyApplicationConnector @Inject() (
    @Named("subordinate") val subordinate: ThirdPartyApplicationConnector,
    @Named("principal") val principal: ThirdPartyApplicationConnector
  ) extends EnvironmentAware[ThirdPartyApplicationConnector]
