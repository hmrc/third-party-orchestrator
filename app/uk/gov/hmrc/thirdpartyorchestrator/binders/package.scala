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

package uk.gov.hmrc.thirdpartyorchestrator

import java.{util => ju}
import scala.util.Try

import play.api.mvc.{PathBindable, QueryStringBindable}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ClientSecret
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

package object binders {

  // $COVERAGE-OFF$
  private def applicationIdFromString(text: String): Either[String, ApplicationId] = {
    ApplicationId.apply(text)
      .toRight(s"Cannot accept $text as ApplicationId")
  }

  private def userIdFromString(text: String): Either[String, UserId] = {
    UserId.apply(text)
      .toRight(s"Cannot accept $text as UserId")
  }

  implicit def applicationIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApplicationId] = new PathBindable[ApplicationId] {

    override def bind(key: String, value: String): Either[String, ApplicationId] = {
      textBinder.bind(key, value).flatMap(applicationIdFromString)
    }

    override def unbind(key: String, applicationId: ApplicationId): String = {
      applicationId.value.toString()
    }
  }

  implicit def applicationIdQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[ApplicationId] = new QueryStringBindable[ApplicationId] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApplicationId]] = {
      textBinder.bind(key, params).map(_.flatMap(applicationIdFromString))
    }

    override def unbind(key: String, applicationId: ApplicationId): String = {
      textBinder.unbind(key, applicationId.value.toString())
    }
  }

  implicit def userIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[UserId] = new PathBindable[UserId] {

    override def bind(key: String, value: String): Either[String, UserId] = {
      textBinder.bind(key, value).flatMap(userIdFromString)
    }

    override def unbind(key: String, userId: UserId): String = {
      userId.value.toString()
    }
  }

  implicit def queryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[UserId] = new QueryStringBindable[UserId] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, UserId]] = {
      for {
        textOrBindError <- textBinder.bind("developerId", params)
      } yield textOrBindError match {
        case Right(idText) => UserId.apply(idText).toRight(s"Cannot accept $idText as a developer identifier")
        case _             => Left("Unable to bind a developer identifier")
      }
    }

    override def unbind(key: String, developerId: UserId): String = {
      textBinder.unbind("developerId", developerId.toString())
    }
  }

  private def clientSecretIdFromString(text: String): Either[String, ClientSecret.Id] = {
    Try(ju.UUID.fromString(text))
      .toOption
      .toRight(s"Cannot accept $text as ClientSecret.Id")
      .map(ClientSecret.Id(_))
  }

  implicit def clientSecretIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ClientSecret.Id] = new PathBindable[ClientSecret.Id] {

    override def bind(key: String, value: String): Either[String, ClientSecret.Id] = {
      textBinder.bind(key, value).flatMap(clientSecretIdFromString(_))
    }

    override def unbind(key: String, clientSecretId: ClientSecret.Id): String = {
      clientSecretId.value.toString()
    }
  }

  implicit def apiContextPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApiContext] = new PathBindable[ApiContext] {

    override def bind(key: String, value: String): Either[String, ApiContext] = {
      textBinder.bind(key, value).map(ApiContext(_))
    }

    override def unbind(key: String, apiContext: ApiContext): String = {
      apiContext.value
    }
  }

  implicit def apiContextQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[ApiContext] = new QueryStringBindable[ApiContext] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApiContext]] = {
      for {
        text <- textBinder.bind(key, params)
      } yield {
        text match {
          case Right(context) => Right(ApiContext(context))
          case _              => Left("Unable to bind an api context")
        }
      }
    }

    override def unbind(key: String, context: ApiContext): String = {
      textBinder.unbind(key, context.value)
    }
  }

  implicit def apiVersionPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApiVersionNbr] = new PathBindable[ApiVersionNbr] {

    override def bind(key: String, value: String): Either[String, ApiVersionNbr] = {
      textBinder.bind(key, value).map(ApiVersionNbr(_))
    }

    override def unbind(key: String, apiVersion: ApiVersionNbr): String = {
      apiVersion.value
    }
  }

  implicit def apiVersionQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[ApiVersionNbr] = new QueryStringBindable[ApiVersionNbr] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApiVersionNbr]] = {
      for {
        text <- textBinder.bind(key, params)
      } yield {
        text match {
          case Right(version) => Right(ApiVersionNbr(version))
          case _              => Left("Unable to bind an api version")
        }
      }
    }

    override def unbind(key: String, version: ApiVersionNbr): String = {
      textBinder.unbind(key, version.value)
    }
  }

  implicit def clientIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ClientId] = new PathBindable[ClientId] {

    override def bind(key: String, value: String): Either[String, ClientId] = {
      textBinder.bind(key, value).map(ClientId(_))
    }

    override def unbind(key: String, clientId: ClientId): String = {
      clientId.value
    }
  }

  implicit def clientIdQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[ClientId] = new QueryStringBindable[ClientId] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ClientId]] = {
      for {
        text <- textBinder.bind(key, params)
      } yield {
        text match {
          case Right(clientId) => Right(ClientId(clientId))
          case _               => Left("Unable to bind an clientId")
        }
      }
    }

    override def unbind(key: String, clientId: ClientId): String = {
      textBinder.unbind(key, clientId.value)
    }
  }

  implicit def environmentPathBinder(implicit textBinder: PathBindable[String]): PathBindable[Environment] = new PathBindable[Environment] {

    override def bind(key: String, value: String): Either[String, Environment] = {
      for {
        text <- textBinder.bind(key, value)
        env  <- Environment.apply(text).toRight("Not a valid environment")
      } yield env
    }

    override def unbind(key: String, env: Environment): String = {
      env.toString.toLowerCase
    }
  }
  // $COVERAGE-ON$
}
