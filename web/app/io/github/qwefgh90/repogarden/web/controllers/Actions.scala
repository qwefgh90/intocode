package io.github.qwefgh90.repogarden.web.controllers

import javax.inject._
import scala.concurrent._
import play.api._
import play.api.mvc._
import play.api.mvc.Results._

class ActionBuilder @Inject() (parser: BodyParsers.Default)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {
  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    block(request)
  }
  
  def UserAction(implicit ec: ExecutionContext) = new ActionRefiner[Request, UserRequest] {
    def executionContext = ec
    def refine[A](req: Request[A]) = Future.successful {
      val signed = "signed".equals(req.session.get("signed").getOrElse(""))
      Either.cond(!req.session.isEmpty && signed,
      new UserRequest("", req),
      Unauthorized)
    }
  }
}

class UserRequest[A](val accessToken: String, request: Request[A]) extends WrappedRequest[A](request)