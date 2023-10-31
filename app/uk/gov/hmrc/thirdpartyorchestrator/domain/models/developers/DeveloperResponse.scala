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

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.{Developer, Session}

case class DeveloperResponse(
    userId: UserId,
    email: LaxEmailAddress,
    firstName: String,
    lastName: String
  )

object DeveloperResponse {

  def from(session: Session): DeveloperResponse =
    from(session.developer)

  def from(developers: Set[Developer]): Set[DeveloperResponse] =
    developers.map(from(_))

  def from(developer: Developer): DeveloperResponse =
    DeveloperResponse(
      developer.userId,
      developer.email,
      developer.firstName,
      developer.lastName
    )

  implicit val format = Json.format[DeveloperResponse]
}
