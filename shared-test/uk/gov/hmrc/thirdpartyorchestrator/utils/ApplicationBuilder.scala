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
import java.util.UUID

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.common.domain.models.FullName
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationResponse, _}
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

trait ApplicationBuilder {

  def buildApplication(applicationId: ApplicationId, clientId: ClientId, userId1: UserId, userId2: UserId): ApplicationResponse = {
    val standardAccess = Access.Standard(
      importantSubmissionData = Some(ImportantSubmissionData(
        organisationUrl = Some("https://www.example.com"),
        responsibleIndividual = ResponsibleIndividual(FullName("Bob Fleming"), LaxEmailAddress("bob@example.com")),
        serverLocations = Set(ServerLocation.InUK),
        termsAndConditionsLocation = TermsAndConditionsLocations.InDesktopSoftware,
        privacyPolicyLocation = PrivacyPolicyLocations.InDesktopSoftware,
        termsOfUseAcceptances = List(TermsOfUseAcceptance(
          responsibleIndividual = ResponsibleIndividual(FullName("Bob Fleming"), LaxEmailAddress("bob@example.com")),
          dateTime = LocalDateTime.parse("2022-10-08T12:24:31.123"),
          submissionId = SubmissionId(UUID.fromString("4e62811a-7ab3-4421-a89e-65a8bad9b6ae")),
          submissionInstance = 0
        ))
      ))
    )

    ApplicationResponse(
      id = applicationId,
      clientId = clientId,
      gatewayId = "gateway-id",
      name = "Petes test application",
      deployedTo = Environment.PRODUCTION,
      description = Some("Petes test application description"),
      collaborators = Set(buildCollaborator(userId1), buildCollaborator(userId2)),
      createdOn = LocalDateTime.parse("2022-12-23T12:24:31.123"),
      lastAccess = Some(LocalDateTime.parse("2023-10-02T12:24:31.123")),
      grantLength = 18,
      lastAccessTokenUsage = None,
      redirectUris = List.empty,
      termsAndConditionsUrl = None,
      privacyPolicyUrl = None,
      access = standardAccess,
      state = ApplicationState(name = State.TESTING, updatedOn = LocalDateTime.parse("2022-10-08T12:24:31.123")),
      rateLimitTier = RateLimitTier.BRONZE,
      checkInformation = None,
      blocked = false,
      trusted = false,
      ipAllowlist = IpAllowlist(false, Set.empty),
      moreApplication = MoreApplication(false)
    )
  }

  def buildCollaborator(userId: UserId) = {
    Collaborator(
      emailAddress = LaxEmailAddress("bob@example.com"),
      role = Collaborator.Roles.ADMINISTRATOR,
      userId = userId
    )
  }
}
