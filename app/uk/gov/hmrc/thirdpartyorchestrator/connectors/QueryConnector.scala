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

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString

import play.api.http.ContentTypes
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyWritables
import play.mvc.Http
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.services.QueryParamsToQueryStringMap
import uk.gov.hmrc.thirdpartyorchestrator.utils.{ApplicationLogger, EbridgeConfigurator}
import scala.concurrent.duration.DurationInt
import org.apache.pekko.stream.OverflowStrategy

trait QueryConnector {
  def query[T](qry: ApplicationQuery)(using HeaderCarrier, HttpReads[T]): Future[T]
  def queryStream(qry: ApplicationQuery)(using HeaderCarrier): Future[Source[ByteString, ?]]
  def postQuery[T](qry: ApplicationQuery)(using HeaderCarrier, HttpReads[T]): Future[T]

  def query[T](qry: Map[String, String])(using HeaderCarrier, HttpReads[T]): Future[T]
  def queryStream(qry: Map[String, String])(using HeaderCarrier): Future[Source[ByteString, ?]]
  def postQuery[T](qry: Map[String, Seq[String]])(using HeaderCarrier, HttpReads[T]): Future[T]
}

abstract class AbstractQueryConnector(using ExecutionContext, Materializer)
    extends QueryConnector
    with StreamHttpReadsInstances
    with JsonBodyWritables {

  protected val serviceBaseUrl: String

  val api = ApiName("third-party-application")
  protected val metrics: ConnectorMetrics

  protected def http: HttpClientV2

  protected def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder

  override def query[T](qry: Map[String, String])(using HeaderCarrier, HttpReads[T]): Future[T] = {
    configureEbridgeIfRequired(
      http
        .get(url"${serviceBaseUrl}/query?$qry")
    )
      .setHeader(play.api.http.HeaderNames.CONTENT_TYPE -> ContentTypes.JSON)
      .execute[T]
  }

  override def queryStream(qry: Map[String, String])(using HeaderCarrier): Future[Source[ByteString, ?]] = {
    configureEbridgeIfRequired(
      http
        .get(url"${serviceBaseUrl}/query?${qry}")
        .transform(_.withRequestTimeout(1.minutes))
    )
      .setHeader(Http.HeaderNames.ACCEPT -> "application/stream+json")
      .stream[Source[ByteString, ?]]
      .map(_.buffer(100, OverflowStrategy.backpressure))

  }

  override def query[T](qry: ApplicationQuery)(using HeaderCarrier, HttpReads[T]): Future[T] = {
    val params = QueryParamsToQueryStringMap.toHttpQueryString(qry)
    query[T](params)
  }

  override def queryStream(qry: ApplicationQuery)(using HeaderCarrier): Future[Source[ByteString, ?]] = {
    val params = QueryParamsToQueryStringMap.toHttpQueryString(qry)
    queryStream(params)
  }

  override def postQuery[T](qry: Map[String, Seq[String]])(using HeaderCarrier, HttpReads[T]): Future[T] = {
    configureEbridgeIfRequired(
      http
        .post(url"${serviceBaseUrl}/query")
        .withBody[JsValue](Json.toJson(qry))
    )
      .setHeader(play.api.http.HeaderNames.CONTENT_TYPE -> ContentTypes.JSON)
      .execute[T]
  }

  override def postQuery[T](qry: ApplicationQuery)(using hc: HeaderCarrier, rds: HttpReads[T]): Future[T] = {
    val params = QueryParamsToQueryStringMap.toQuery(qry)
      .map {
        case (paramName, values) => paramName.text -> values
      }
    postQuery[T](params)
  }
}

@Singleton
@Named("principal")
class PrincipalQueryConnector @Inject() (
    val config: PrincipalThirdPartyApplicationConnector.Config,
    val http: HttpClientV2,
    val metrics: ConnectorMetrics
  )(using ExecutionContext,
    Materializer
  ) extends AbstractQueryConnector {

  val serviceBaseUrl = config.serviceBaseUrl

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder = requestBuilder
}

@Singleton
@Named("subordinate")
class SubordinateQueryConnector @Inject() (
    val config: SubordinateThirdPartyApplicationConnector.Config,
    val http: HttpClientV2,
    val metrics: ConnectorMetrics
  )(using ExecutionContext,
    Materializer
  ) extends AbstractQueryConnector with ApplicationLogger {

  val serviceBaseUrl: String = config.serviceBaseUrl

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder =
    EbridgeConfigurator.configure(config.useProxy, config.bearerToken, config.apiKey)(requestBuilder)
}

@Singleton
class EnvironmentAwareQueryConnector @Inject() (
    @Named("subordinate") val subordinate: QueryConnector,
    @Named("principal") val principal: QueryConnector
  ) extends EnvironmentAware[QueryConnector]
