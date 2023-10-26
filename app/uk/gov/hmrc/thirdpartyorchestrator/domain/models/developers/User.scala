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

package uk.gov.hmrc.thirdpartyorchestrator.domain.models.developers

import java.time.LocalDateTime

import play.api.libs.json.{Format, Json, _}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}
import uk.gov.hmrc.thirdpartyorchestrator.domain.models.developers.MfaDetailFormats._
import uk.gov.hmrc.thirdpartyorchestrator.domain.models.developers.MfaDetail
import uk.gov.hmrc.thirdpartyorchestrator.domain.models.developers.AccountSetup
import uk.gov.hmrc.thirdpartyorchestrator.domain.models.developers.EmailPreferences

case class User(
    email: LaxEmailAddress,
    firstName: String,
    lastName: String,
    registrationTime: LocalDateTime,
    lastModified: LocalDateTime,
    verified: Boolean,
    accountSetup: Option[AccountSetup] = None,
    organisation: Option[String] = None,
    mfaEnabled: Boolean = false,
    mfaDetails: List[MfaDetail],
    nonce: Option[String] = None,
    emailPreferences: EmailPreferences = EmailPreferences.noPreferences,
    userId: UserId
  )

object User extends EnvReads with EnvWrites {

  implicit val dateTimeFormat: Format[LocalDateTime] = Format(DefaultLocalDateTimeReads, DefaultLocalDateTimeWrites)
  implicit val format: OFormat[User]                 = Json.format[User]
}
