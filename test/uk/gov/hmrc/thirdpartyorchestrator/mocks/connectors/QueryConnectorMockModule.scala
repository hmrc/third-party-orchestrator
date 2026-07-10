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

package uk.gov.hmrc.thirdpartyorchestrator.mocks.connectors

import scala.concurrent.Future.{failed, successful}

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import play.api.libs.json.{Json, Writes}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.QueriedApplication
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.thirdpartyorchestrator.connectors.{EnvironmentAwareQueryConnector, PrincipalQueryConnector, QueryConnector, SubordinateQueryConnector}

trait QueryConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait AbstractQueryConnectMock {
    def aMock: QueryConnector

    object ByQuery {

      def returnsFor[T](query: ApplicationQuery, results: T) = {
        when(aMock.query[T](eqTo(query))(using *, *)).thenReturn(successful(results))
      }

      def failsFor[T](query: ApplicationQuery, err: Throwable) = {
        when(aMock.query[T](eqTo(query))(using *, *)).thenReturn(failed(err))
      }

      def returns[T](results: T) = {
        when(aMock.query[T](*[ApplicationQuery])(using *, *)).thenReturn(successful(results))
      }

      def fails[T](err: Throwable) = {
        when(aMock.query[T](*[ApplicationQuery])(using *, *)).thenReturn(failed(err))
      }
    }

    object ByQueryParams {

      def returns[T](results: T) = {
        when(aMock.query[T](*[Map[String, String]])(using *, *)).thenReturn(successful(results))
      }

      def returnsFor[T](params: Map[String, String], results: T) = {
        when(aMock.query[T](eqTo(params))(using *, *)).thenReturn(successful(results))
      }

      def fails[T](err: Throwable) = {
        when(aMock.query[T](*[Map[String, String]])(using *, *)).thenReturn(failed(err))
      }
    }

    object ByQueryStreamParams {

      private def asStreamOfByteStrings(apps: Seq[QueriedApplication])(using Writes[QueriedApplication]): Source[ByteString, ?] =
        Source(apps.map(a => ByteString(Json.toJson(a).toString)))

      def returns(apps: ApplicationWithCollaborators*)(using Writes[QueriedApplication]) = {
        when(aMock.queryStream(*[Map[String, String]])(using *)).thenReturn(successful(asStreamOfByteStrings(apps.map(QueriedApplication(_)))))
      }

      def returnsFor(params: Map[String, String], apps: ApplicationWithCollaborators*)(using Writes[QueriedApplication]) = {
        when(aMock.queryStream(eqTo(params))(using *)).thenReturn(successful(asStreamOfByteStrings(apps.map(QueriedApplication(_)))))
      }
    }

    object ByQueryPost {

      def returnsFor[T](query: ApplicationQuery, results: T) = {
        when(aMock.postQuery[T](eqTo(query))(using *, *)).thenReturn(successful(results))
      }

      def failsFor[T](query: ApplicationQuery, err: Throwable) = {
        when(aMock.postQuery[T](eqTo(query))(using *, *)).thenReturn(failed(err))
      }

      def returns[T](results: T) = {
        when(aMock.postQuery[T](*[ApplicationQuery])(using *, *)).thenReturn(successful(results))
      }

      def fails[T](err: Throwable) = {
        when(aMock.postQuery[T](*[ApplicationQuery])(using *, *)).thenReturn(failed(err))
      }
    }

    object ByQueryParamsPost {

      def returns[T](results: T) = {
        when(aMock.postQuery[T](*[Map[String, Seq[String]]])(using *, *)).thenReturn(successful(results))
      }

      def returnsFor[T](params: Map[String, Seq[String]], results: T) = {
        when(aMock.postQuery[T](eqTo(params))(using *, *)).thenReturn(successful(results))
      }

      def fails[T](err: Throwable) = {
        when(aMock.postQuery[T](*[Map[String, Seq[String]]])(using *, *)).thenReturn(failed(err))
      }
    }
  }

  object SubordinateQueryConnectorMock extends AbstractQueryConnectMock {
    override val aMock: QueryConnector = mock[SubordinateQueryConnector]
  }

  object PrincipalQueryConnectorMock extends AbstractQueryConnectMock {
    override val aMock: PrincipalQueryConnector = mock[PrincipalQueryConnector]
  }

  object EnvironmentAwareQueryConnectorMock {
    private val subordinateConnector = SubordinateQueryConnectorMock
    private val principalConnector   = PrincipalQueryConnectorMock

    lazy val instance = {
      new EnvironmentAwareQueryConnector(subordinateConnector.aMock, principalConnector.aMock)
    }

    lazy val Principal   = principalConnector
    lazy val Subordinate = subordinateConnector
  }
}
