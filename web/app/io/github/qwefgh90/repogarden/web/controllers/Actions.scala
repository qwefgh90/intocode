package io.github.qwefgh90.repogarden.web.controllers

import javax.inject._
import scala.concurrent._
import play.api.Logger
import play.api.cache._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.Results._
import org.eclipse.egit.github.core._
import io.github.qwefgh90.repogarden.web.model.Implicits._

class ActionBuilder @Inject() (parser: play.api.mvc.BodyParsers.Default)(implicit ec: ExecutionContext, cache: SyncCacheApi) extends ActionBuilderImpl(parser) {
  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    block(request)
  }
  
  def UserAction(implicit ec: ExecutionContext) = new ActionRefiner[Request, UserRequest] {
    def executionContext = ec
    def refine[A](req: Request[A]) = Future.successful {
      val signed = "signed".equals(req.session.get("signed").getOrElse(""))
      val userJsResult = Json
        .parse(req.session.get("user").getOrElse("{}"))
        .validate[User]
//        .getOrElse(new User)

      val user = userJsResult match {
        case success: JsSuccess[User] => success.get
        case JsError(err) => {
          Logger.debug(err.toString)
          new User
        }
      }

      Logger.debug(s"id: ${user.getId}, ${user.getEmail}")

      val tokenOpt =
          cache
            .get(user.getId.toString)

      Either.cond(signed && tokenOpt.isDefined,
      new UserRequest(user, tokenOpt.getOrElse(""), req), Unauthorized)
    }
  }
}

class UserRequest[A](val user: User, val token: String, request: Request[A]) extends WrappedRequest[A](request)
