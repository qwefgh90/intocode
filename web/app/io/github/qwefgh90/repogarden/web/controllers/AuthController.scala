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

@Singleton
class AuthController @Inject()(authService: AuthService, implicit val context: ExecutionContext, cache: AsyncCacheApi) extends Controller {
  def client = Action { implicit request => 
    val client_id = authService.getClientId
    val state = authService.getState
    Ok(Json.toJson(new InitialVector(client_id, state)))
  }
  
  def accessToken = Action.async { implicit request =>
    val body: AnyContent = request.body
    body.asJson.map{json => {
        //code: String, state: String, clientId: String
        val code = json \ "code"
        val state = json \ "state"
        val clientId = json \ "clientId"
        val tokenFuture = authService.getAccessToken(code.as[String], state.as[String], clientId.as[String])
        tokenFuture.map(token => {
          val githubService = new GithubService(token)
          val user = githubService.getUser
          //cache.set(user.getEmail, token)
          Ok("").withSession("signed" -> "signed","user" -> Json.toJson(user).toString)        }
        ).recover{
          case e: RuntimeException => {
            Logger.warn(s"accessToken ${request.toString()} ${e}")
            BadRequest("invalid request to token.")
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
