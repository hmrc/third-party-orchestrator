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

package uk.gov.hmrc.thirdpartyorchestrator.commands.applications.config

import com.google.inject.{Inject, Provider, Singleton}

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import uk.gov.hmrc.thirdpartyorchestrator.commands.applications.connectors.{PrincipalAppCmdConnector, SubordinateAppCmdConnector}
import uk.gov.hmrc.thirdpartyorchestrator.config.ConfigProviderHelper

@Singleton
class PrincipalAppCmdConnectorConfigProvider @Inject() (override val sc: ServicesConfig)
    extends Provider[PrincipalAppCmdConnector.Config]
    with ConfigProviderHelper {

  override def get(): PrincipalAppCmdConnector.Config = {
    val serviceName = "third-party-application-principal"
    PrincipalAppCmdConnector.Config(
      serviceUrl("third-party-application")(serviceName)
    )
  }
}

@Singleton
class SubordinateAppCmdConnectorConfigProvider @Inject() (override val sc: ServicesConfig)
    extends Provider[SubordinateAppCmdConnector.Config]
    with ConfigProviderHelper {

  override def get(): SubordinateAppCmdConnector.Config = {
    val serviceName = "third-party-application-subordinate"
    SubordinateAppCmdConnector.Config(
      serviceUrl("third-party-application")(serviceName),
      useProxy(serviceName),
      bearerToken(serviceName),
      apiKey(serviceName)
    )
  }
}
