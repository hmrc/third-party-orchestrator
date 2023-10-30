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

package uk.gov.hmrc.apiplatform.modules.applications.domain.models

import java.time.LocalDateTime

import play.api.libs.json._

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId}

case class Application(
    id: ApplicationId,
    clientId: ClientId,
    gatewayId: String,
    name: String,
    deployedTo: String,
    description: Option[String] = None,
    collaborators: Set[Collaborator],
    createdOn: LocalDateTime,
    lastAccess: Option[LocalDateTime],
    grantLength: Int,
    lastAccessTokenUsage: Option[LocalDateTime] = None,
    redirectUris: List[String] = List.empty,
    termsAndConditionsUrl: Option[String] = None,
    privacyPolicyUrl: Option[String] = None,
    access: Access = Standard(),
    state: ApplicationState = ApplicationState(name = State.TESTING),
    rateLimitTier: RateLimitTier = RateLimitTier.BRONZE,
    checkInformation: Option[CheckInformation] = None,
    blocked: Boolean = false,
    trusted: Boolean = false,
    ipAllowlist: IpAllowlist = IpAllowlist(),
    moreApplication: MoreApplication = MoreApplication()
  )

object Application {
  implicit val formatApplication = Json.format[Application]
}