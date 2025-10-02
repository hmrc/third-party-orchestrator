/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.http.metrics.common._

import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.services.QueryParamsToQueryStringMap
import uk.gov.hmrc.thirdpartyorchestrator.utils.EbridgeConfigurator

trait QueryConnector {
  def query(qry: ApplicationQuery)(implicit hc: HeaderCarrier): Future[HttpResponse]
  def query(qry: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse]
}

abstract class AbstractQueryConnector(implicit val ec: ExecutionContext) extends QueryConnector with RecordMetrics {
  protected val serviceBaseUrl: String

  val apiMetrics: ApiMetrics

  protected def http: HttpClientV2

  val api = API("third-party-application")

  protected def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder

  override def query(qry: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val simplifiedQry = qry.map {
      case (k, vs) => k -> vs.mkString
    }
    configureEbridgeIfRequired(
      http.get(url"${serviceBaseUrl}/query?$simplifiedQry")
    ).execute[HttpResponse]
  }

  override def query(qry: ApplicationQuery)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val params = QueryParamsToQueryStringMap.toQuery(qry)
    query(params)
  }
}

@Singleton
@Named("principal")
class PrincipalQueryConnector @Inject() (
    val config: PrincipalThirdPartyApplicationConnector.Config,
    val http: HttpClientV2,
    val apiMetrics: ApiMetrics
  )(implicit override val ec: ExecutionContext
  ) extends AbstractQueryConnector {

  val serviceBaseUrl = config.serviceBaseUrl

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder = requestBuilder
}

@Singleton
@Named("subordinate")
class SubordinateQueryConnector @Inject() (
    val config: SubordinateThirdPartyApplicationConnector.Config,
    val http: HttpClientV2,
    val apiMetrics: ApiMetrics
  )(implicit override val ec: ExecutionContext
  ) extends AbstractQueryConnector {

  val serviceBaseUrl: String = config.serviceBaseUrl

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder =
    EbridgeConfigurator.configure(config.useProxy, config.bearerToken, config.apiKey)(requestBuilder)
}

@Singleton
class EnvironmentAwareQueryConnector @Inject() (
    @Named("subordinate") val subordinate: QueryConnector,
    @Named("principal") val principal: QueryConnector
  ) extends EnvironmentAware[QueryConnector]
