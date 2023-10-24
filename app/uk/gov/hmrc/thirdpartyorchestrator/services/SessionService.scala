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

import uk.gov.hmrc.thirdpartyorchestrator.connectors.ThirdPartyDeveloperConnector
import uk.gov.hmrc.thirdpartyorchestrator.domain.models.{Session, SessionId}

@Singleton
class SessionService @Inject() (
    val thirdPartyDeveloperConnector: ThirdPartyDeveloperConnector
  )(implicit val ec: ExecutionContext
  ) {

  def fetch(sessionId: SessionId)(implicit hc: HeaderCarrier): Future[Option[Session]] =
    thirdPartyDeveloperConnector.fetchSession(sessionId)
}