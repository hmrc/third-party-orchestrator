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

import java.{util => ju}
import scala.util.control.Exception._

case class SessionId(value: ju.UUID) extends AnyVal {
  override def toString(): String = value.toString
}

object SessionId {
  import play.api.libs.json.{Format, Json}

  implicit val format: Format[SessionId] = Json.valueFormat[SessionId]

  def apply(raw: String): Option[SessionId] = allCatch.opt(SessionId(ju.UUID.fromString(raw)))

  def unsafeApply(raw: String): SessionId = apply(raw).getOrElse(throw new RuntimeException(s"$raw is not a valid SessionId"))

// $COVERAGE-OFF$
  def random: SessionId = SessionId(ju.UUID.randomUUID())
// $COVERAGE-ON$
}
