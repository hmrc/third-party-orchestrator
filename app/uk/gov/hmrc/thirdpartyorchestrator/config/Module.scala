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

import com.google.inject.AbstractModule
import com.google.inject.name.Names.named

import uk.gov.hmrc.thirdpartyorchestrator.connectors.{
  AbstractThirdPartyApplicationConnector,
  ConnectorMetrics,
  ConnectorMetricsImpl,
  PrincipalThirdPartyApplicationConnector,
  SubordinateThirdPartyApplicationConnector,
  ThirdPartyApplicationConnector
}

class Module extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[AppConfig]).asEagerSingleton()
    bind(classOf[ConnectorMetrics]).to(classOf[ConnectorMetricsImpl])

    bind(classOf[AbstractThirdPartyApplicationConnector.Config]).annotatedWith(named("principal")).toProvider(classOf[PrincipalThirdPartyApplicationConnectorConfigProvider])
    bind(classOf[AbstractThirdPartyApplicationConnector.Config]).annotatedWith(named("subordinate")).toProvider(classOf[SubordinateThirdPartyApplicationConnectorConfigProvider])

    bind(classOf[ThirdPartyApplicationConnector]).annotatedWith(named("subordinate")).to(classOf[SubordinateThirdPartyApplicationConnector])
    bind(classOf[ThirdPartyApplicationConnector]).annotatedWith(named("principal")).to(classOf[PrincipalThirdPartyApplicationConnector])
  }
}
