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

package uk.gov.hmrc.thirdpartyorchestrator.domain.models.applications

import java.time.{LocalDateTime, ZoneOffset}

import play.api.libs.json.{Format, OFormat}

import uk.gov.hmrc.apiplatform.modules.common.domain.services.LocalDateTimeFormatter
import uk.gov.hmrc.thirdpartyorchestrator.domain.models.applications.State.{State, _}

case class ApplicationState(
    name: State = TESTING,
    requestedByEmailAddress: Option[String] = None,
    requestedByName: Option[String] = None,
    verificationCode: Option[String] = None,
    updatedOn: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
  ) {

  def isInTesting                                                      = name == State.TESTING
  def isPendingResponsibleIndividualVerification                       = name == State.PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION
  def isPendingGatekeeperApproval                                      = name == State.PENDING_GATEKEEPER_APPROVAL
  def isPendingRequesterVerification                                   = name == State.PENDING_REQUESTER_VERIFICATION
  def isInPreProductionOrProduction                                    = name == State.PRE_PRODUCTION || name == State.PRODUCTION
  def isInPendingGatekeeperApprovalOrResponsibleIndividualVerification = name == State.PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION || name == State.PENDING_GATEKEEPER_APPROVAL
  def isInProduction                                                   = name == State.PRODUCTION
  def isDeleted                                                        = name == State.DELETED
}

object ApplicationState {
  import play.api.libs.json.Json
  implicit val dateTimeFormats: Format[LocalDateTime]            = LocalDateTimeFormatter.localDateTimeFormat
  implicit val formatApplicationState: OFormat[ApplicationState] = Json.format[ApplicationState]

  val testing: ApplicationState = ApplicationState(State.TESTING, None)
}
