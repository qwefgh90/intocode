package io.github.qwefgh90.repogarden.web.controllers

import javax.inject._
import play.api.cache._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import io.github.qwefgh90.repogarden.web.model._
import io.github.qwefgh90.repogarden.web.model.Implicits._
import io.github.qwefgh90.repogarden.web.service.AuthService
import io.github.qwefgh90.repogarden.web.service.GithubService
import io.github.qwefgh90.repogarden.web.service.GithubServiceProvider

@Singleton
class AuthController @Inject()(authService: AuthService, implicit val context: ExecutionContext, cache: AsyncCacheApi, githubProvider: GithubServiceProvider) extends Controller {
  def client = Action { implicit request =>
    val client_id = authService.getClientId
    val state = authService.getState
    Ok(Json.toJson(new InitialVector(client_id, state)))
  }
  
  def accessToken = Action.async { implicit request =>
    val body: AnyContent = request.body
    body.asJson.map{json => {
      val code = json \ "code"
      val state = json \ "state"
      val clientId = json \ "clientId"
      val params = Seq(code, state, clientId)
      if(params.forall(p => p.isDefined)){
        val tokenFuture = authService.getAccessToken(code.as[String], state.as[String], clientId.as[String])
        tokenFuture.map(token => {
          val githubService = githubProvider.getInstance(token)
          val user = githubService.getUser
          cache.set(user.getId.toString, token)
          Ok("").withSession("signed" -> "signed","user" -> Json.toJson(user)(userWritesToSession).toString())}
        ).recover{
          case e: RuntimeException => {
            Logger.warn(s"accessToken ${request.toString()} ${e} cache: ${cache}")
            InternalServerError("invalid request to token.")
          }
        }
      }else{
        Future{
          Logger.warn(params.filter(_.isEmpty).map(_.toEither.left.toString).mkString)
          BadRequest("Expecting application/json request body")
        }
      }
    }
    }.getOrElse{
      Future{
        Logger.warn(s"${request.toString()}")
        BadRequest("Expecting application/json request body")
      }
    }
  }

  def logout = Action { implicit request =>
    Ok("").withNewSession
  }

}
