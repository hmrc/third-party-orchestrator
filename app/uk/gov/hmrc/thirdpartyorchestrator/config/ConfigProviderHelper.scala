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

package uk.gov.hmrc.thirdpartyorchestrator.config

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait ConfigProviderHelper {
  protected val sc: ServicesConfig

  def serviceUrl(defaultContext: String)(serviceName: String): String = {
    if (useProxy(serviceName)) s"${sc.baseUrl(serviceName)}/${sc.getConfString(s"$serviceName.context", defaultContext)}"
    else sc.baseUrl(serviceName)
  }

  def useProxy(serviceName: String) = sc.getConfBool(s"$serviceName.use-proxy", false)

  def bearerToken(serviceName: String) = sc.getConfString(s"$serviceName.bearer-token", "")

  def apiKey(serviceName: String) = sc.getConfString(s"$serviceName.api-key", "")
}
