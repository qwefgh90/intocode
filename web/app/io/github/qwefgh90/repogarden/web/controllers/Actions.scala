package io.github.qwefgh90.repogarden.web.controllers

import javax.inject._
import scala.concurrent._
import play.api._
import play.api.cache._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.Results._
import org.eclipse.egit.github.core._
import io.github.qwefgh90.repogarden.web.model.Implicits._

class ActionBuilder @Inject() (parser: BodyParsers.Default)(implicit ec: ExecutionContext, cache: AsyncCacheApi) extends ActionBuilderImpl(parser) {
  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    block(request)
  }
  
  def UserAction(implicit ec: ExecutionContext) = new ActionRefiner[Request, UserRequest] {
    def executionContext = ec
    def refine[A](req: Request[A]) = Future.successful {
      val signed = "signed".equals(req.session.get("signed").getOrElse(""))
      val userOpt = try{
        Option(Json.parse(req.session("user")).validate[User].get)
      }catch{
        case e: Exception => {
          Logger.warn(e.toString)
          Option.empty
        }
      }

      Either.cond(signed && userOpt.isDefined ,
      new UserRequest(userOpt.getOrElse(new User()), req), Unauthorized)
    }
  }
}

class UserRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)
