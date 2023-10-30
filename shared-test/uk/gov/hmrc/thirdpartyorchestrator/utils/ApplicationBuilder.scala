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

package uk.gov.hmrc.thirdpartyorchestrator.utils

import java.time.LocalDateTime

import uk.gov.hmrc.apiplatform.modules.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.SubmissionId

trait ApplicationBuilder {

  def buildApplication(applicationId: ApplicationId, clientId: ClientId, userId: UserId, submissionId: SubmissionId, name: String, createdOn: LocalDateTime): Application = {
    Application(
      applicationId,
      clientId,
      "gateway-id",
      name,
      "PRODUCTION",
      Some("app description"),
      Set(buildCollaborator(userId)),
      createdOn,
      Some(LocalDateTime.parse("2023-10-02T12:24:31.123")),
      18,
      None,
      List.empty,
      None,
      None,
      buildStandardAccess(submissionId),
      ApplicationState.testing,
      RateLimitTier.BRONZE,
      None,
      false,
      false,
      IpAllowlist(),
      MoreApplication(false)
    )
  }

  def buildStandardAccess(submissionId: SubmissionId): Standard = {
    Standard(
      List.empty,
      None,
      None,
      Set.empty,
      None,
      Some(buildImportantSubmissionData(submissionId))
    )
  }

  def buildImportantSubmissionData(submissionId: SubmissionId): ImportantSubmissionData = {
    ImportantSubmissionData(
      Some("https://www.example.com"),
      ResponsibleIndividual(
        ResponsibleIndividual.Name("Bob Fleming"),
        LaxEmailAddress("bob@example.com")
      ),
      Set(ServerLocation.InUK),
      TermsAndConditionsLocations.InDesktopSoftware,
      PrivacyPolicyLocations.InDesktopSoftware,
      List(TermsOfUseAcceptance(
        ResponsibleIndividual(
          ResponsibleIndividual.Name("Bob Fleming"),
          LaxEmailAddress("bob@example.com")
        ),
        LocalDateTime.parse("2022-10-08T12:24:31.123"),
        submissionId,
        0
      ))
    )
  }

  def buildCollaborator(userId: UserId) = {
    Collaborator(
      LaxEmailAddress("bob@example.com"),
      Collaborator.Roles.ADMINISTRATOR,
      userId
    )
  }
}
