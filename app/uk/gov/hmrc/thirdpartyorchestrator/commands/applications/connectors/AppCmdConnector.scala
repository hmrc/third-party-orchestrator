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

package uk.gov.hmrc.thirdpartyorchestrator.commands.applications.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{StringContextOps, _}

import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.thirdpartyorchestrator.commands.applications.domain.models.{AppCmdHandlerTypes, DispatchSuccessResult}
import uk.gov.hmrc.thirdpartyorchestrator.connectors.EnvironmentAware
import uk.gov.hmrc.thirdpartyorchestrator.utils.{ApplicationLogger, EbridgeConfigurator}

trait AppCmdConnector {

  def dispatch(
      applicationId: ApplicationId,
      dispatchRequest: DispatchRequest
    )(implicit hc: HeaderCarrier
    ): AppCmdHandlerTypes.AppCmdResult
}

abstract private[commands] class AbstractAppCmdConnector
    extends AppCmdConnector
    with ApplicationLogger {

  implicit def ec: ExecutionContext
  val serviceBaseUrl: String
  def http: HttpClientV2

  def baseApplicationUrl(applicationId: ApplicationId) = s"$serviceBaseUrl/application/${applicationId}"

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder

  def dispatch(
      applicationId: ApplicationId,
      dispatchRequest: DispatchRequest
    )(implicit hc: HeaderCarrier
    ): AppCmdHandlerTypes.AppCmdResult = {

    import uk.gov.hmrc.apiplatform.modules.common.domain.services.NonEmptyListFormatters._
    import play.api.libs.json._
    import uk.gov.hmrc.http.HttpReads.Implicits._
    import play.api.http.Status._

    def parseWithLogAndThrow[T](input: String)(implicit reads: Reads[T]): T = {
      Json.parse(input).validate[T] match {
        case JsSuccess(t, _) => t
        case JsError(err)    =>
          logger.error(s"Failed to parse >>$input<< due to errors $err")
          throw new InternalServerException("Failed parsing response to dispatch")
      }
    }

    val url = s"${baseApplicationUrl(applicationId)}/dispatch"
    import cats.syntax.either._

    configureEbridgeIfRequired(
      http
        .patch(url"$url")
        .withBody(Json.toJson(dispatchRequest))
    )
      .execute[HttpResponse]
      .map(response =>
        response.status match {
          case OK           => parseWithLogAndThrow[DispatchSuccessResult](response.body).asRight[AppCmdHandlerTypes.Failures]
          case BAD_REQUEST  => parseWithLogAndThrow[AppCmdHandlerTypes.Failures](response.body).asLeft[DispatchSuccessResult]
          case UNAUTHORIZED => throw new UnauthorizedException("Command unauthorised")
          case status       =>
            logger.error(s"Dispatch failed with status code: $status")
            throw new InternalServerException(s"Failed calling dispatch $status")
        }
      )
  }
}

@Singleton
class SubordinateAppCmdConnector @Inject() (
    config: SubordinateAppCmdConnector.Config,
    val http: HttpClientV2
  )(implicit override val ec: ExecutionContext
  ) extends AbstractAppCmdConnector {

  import config._
  val serviceBaseUrl: String = config.baseUrl

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder =
    EbridgeConfigurator.configure(useProxy, bearerToken, apiKey)(requestBuilder)
}

object SubordinateAppCmdConnector {

  case class Config(
      baseUrl: String,
      useProxy: Boolean,
      bearerToken: String,
      apiKey: String
    )
}

@Singleton
class PrincipalAppCmdConnector @Inject() (
    config: PrincipalAppCmdConnector.Config,
    val http: HttpClientV2
  )(implicit val ec: ExecutionContext
  ) extends AbstractAppCmdConnector {

  val serviceBaseUrl: String = config.baseUrl

  def configureEbridgeIfRequired(requestBuilder: RequestBuilder): RequestBuilder = requestBuilder
}

object PrincipalAppCmdConnector {

  case class Config(
      baseUrl: String
    )
}

@Singleton
class EnvironmentAwareAppCmdConnector @Inject() (
    val subordinate: SubordinateAppCmdConnector,
    val principal: PrincipalAppCmdConnector
  ) extends EnvironmentAware[AppCmdConnector]
