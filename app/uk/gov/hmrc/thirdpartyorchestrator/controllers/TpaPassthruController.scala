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

package uk.gov.hmrc.thirdpartyorchestrator.controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import play.api.mvc.ControllerComponents
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import uk.gov.hmrc.thirdpartyorchestrator.connectors.PrincipalThirdPartyApplicationConnector
import uk.gov.hmrc.thirdpartyorchestrator.utils.ApplicationLogger

@Singleton()
class TpaPassthruController @Inject() (
    principalTpaConnector: PrincipalThirdPartyApplicationConnector,
    cc: ControllerComponents
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc) with JsonUtils with ApplicationLogger with JsonExceptionMapper {

  def verifyUplift(verificationCode: String) = Action.async { implicit request =>
    principalTpaConnector.verify(verificationCode)
      .map(_ match {
        case HttpResponse(NO_CONTENT, _, _)    => NoContent
        case HttpResponse(BAD_REQUEST, msg, _) => BadRequest(msg)
        case HttpResponse(status, body, _)     => asJson(new RuntimeException(s"Got unexpected status $status and body $body"))
        case _                                 => asJson(new RuntimeException("Unexpected error when verifying uplift"))
      })
  }
}
