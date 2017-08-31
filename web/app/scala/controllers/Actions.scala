package io.github.qwefgh90.repogarden.web.controllers

import javax.inject._
import scala.concurrent._
import play.api.Logger
import play.api.cache._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.Results._
import org.eclipse.egit.github.core.{User}
import io.github.qwefgh90.repogarden.web.model.Implicits._

trait UserSessionChecker {
  def checkSign(rh: RequestHeader): Boolean
  def getUserFromSession(rh: RequestHeader): Option[User]
  def getTokenOfUser(user: User): Option[String]
}

class DefualtUserSessionChecker @Inject()(cache: SyncCacheApi) extends UserSessionChecker {

  override def checkSign(rh: RequestHeader): Boolean =
    "signed".equals(rh.session.get("signed").getOrElse(""))

  override def getUserFromSession(rh: RequestHeader): Option[User] = {
    val userJsResult = Json
      .parse(rh.session.get("user").getOrElse("{}"))
      .validate[User]

    userJsResult match {
      case success: JsSuccess[User] => Some(success.get)
      case JsError(err) => {
        Logger.debug(err.toString)
        None
      }
    }
  }

  override def getTokenOfUser(user: User): Option[String] =
    cache
      .get(user.getId.toString)
}

class ActionBuilder @Inject() (parser: play.api.mvc.BodyParsers.Default, val checker: UserSessionChecker)(implicit ec: ExecutionContext, cache: SyncCacheApi) extends ActionBuilderImpl(parser) {
  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    block(request)
  }

  def UserAction(implicit ec: ExecutionContext) = new ActionRefiner[Request, UserRequest] {
    def executionContext = ec
    def refine[A](req: Request[A]) = Future.successful {
      val signed = checker.checkSign(req)
      val userOpt = checker.getUserFromSession(req)
      val tokenOpt = userOpt.flatMap{user =>
        Logger.debug(s"request id: ${user.getId}, ${user.getEmail}")
        checker.getTokenOfUser(user)
      }

      Either.cond(signed && tokenOpt.isDefined,
        new UserRequest(userOpt.get, tokenOpt.get, req), Unauthorized)
    }
  }
}

class UserRequest[A](val user: User, val token: String, request: Request[A]) extends WrappedRequest[A](request)
