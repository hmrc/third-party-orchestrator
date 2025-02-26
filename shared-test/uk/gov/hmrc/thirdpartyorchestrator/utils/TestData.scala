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

package uk.gov.hmrc.thirdpartyorchestrator.utils

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationName, ApplicationWithCollaboratorsFixtures, Collaborators}
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.CreateApplicationRequestV1
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Environment, LaxEmailAddress}

trait TestData extends ApplicationWithCollaboratorsFixtures {

  val admin = Collaborators.Administrator(userIdThree, LaxEmailAddress("jim@example.com"))

  val createSandboxApplicationRequest =
    CreateApplicationRequestV1.create(
      name = ApplicationName("Test V1 Application"),
      access = Access.Standard(),
      description = None,
      environment = Environment.SANDBOX,
      collaborators = Set(admin),
      subscriptions = None
    )
}
