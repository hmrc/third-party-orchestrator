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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}
import uk.gov.hmrc.play.http.metrics.common._

import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.tpd.core.domain.models.User
import uk.gov.hmrc.apiplatform.modules.tpd.session.domain.models.{UserSession, UserSessionId}
import uk.gov.hmrc.thirdpartyorchestrator.config.AppConfig

@Singleton
class ThirdPartyDeveloperConnector @Inject() (
    http: HttpClient,
    config: AppConfig,
    val apiMetrics: ApiMetrics
  )(implicit val ec: ExecutionContext
  ) extends RecordMetrics {

  lazy val serviceBaseUrl: String = config.thirdPartyDeveloperUrl
  val api                         = API("third-party-developer")

  def fetchSession(userSessionId: UserSessionId)(implicit hc: HeaderCarrier): Future[Option[UserSession]] =
    record {
      http.GET[Option[UserSession]](s"$serviceBaseUrl/session/$userSessionId")
    }

  def fetchDeveloper(userId: UserId)(implicit hc: HeaderCarrier): Future[Option[User]] = {
    record {
      http.GET[Option[User]](s"$serviceBaseUrl/developer", Seq("developerId" -> userId.toString()))
    }
  }
}
