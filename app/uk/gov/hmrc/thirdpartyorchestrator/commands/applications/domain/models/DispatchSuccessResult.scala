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

package uk.gov.hmrc.thirdpartyorchestrator.commands.applications.domain.models

import play.api.libs.json.{Json, OFormat}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators

case class DispatchSuccessResult(applicationResponse: ApplicationWithCollaborators)

object DispatchSuccessResult {

  // TODO in APM we have the following JSon formatter in scope..... do we need same in TPO???
  /*
    trait ApplicationJsonFormatters extends EnvReads with EnvWrites {
    import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.domain.models.applications._
    import uk.gov.hmrc.apiplatformmicroservice.thirdpartyapplication.controllers.domain.AddCollaboratorRequestOld

    object TOUAHelper {
      // DO NOT POLLUTE WHOLE SCOPE WITH THIS WRITER
      import uk.gov.hmrc.apiplatform.modules.common.domain.services.InstantJsonFormatter.lenientInstantReads
      implicit val formatDateTime                 = Format(lenientInstantReads, InstantEpochMilliWrites)
      val formatTOUA: Format[TermsOfUseAgreement] = Json.format[TermsOfUseAgreement]
    }

    implicit val formatTermsOfUseAgreement = TOUAHelper.formatTOUA

    implicit val formatApplication: Format[Application] = Json.format[Application]

    implicit val formatApplicationWithSubscriptionData = Json.format[ApplicationWithSubscriptionData]

    implicit val formatAddCollaboratorRequest = Json.format[AddCollaboratorRequestOld]
  }

  object ApplicationJsonFormatters extends ApplicationJsonFormatters
   */

  implicit val format: OFormat[DispatchSuccessResult] = Json.format[DispatchSuccessResult]
}
