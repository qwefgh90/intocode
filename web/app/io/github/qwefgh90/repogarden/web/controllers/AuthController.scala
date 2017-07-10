package io.github.qwefgh90.repogarden.web.controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import io.github.qwefgh90.repogarden.web.model._
import io.github.qwefgh90.repogarden.web.model.Implicits._
import io.github.qwefgh90.repogarden.web.service.AuthService

@Singleton
class AuthController @Inject()(authService: AuthService) extends Controller {
  
  def client = Action { implicit request => 
    val client_id = "github client id"
    val state = "unsuggestable random string"
    Ok(Json.toJson(new InitialVector(client_id, state)))
  }
  
  def accessToken = Action { implicit request =>
    val body: AnyContent = request.body
    body.asJson.map{json => {
        //code: String, state: String, clientId: String
        val code = json \ "code"
        val state = json \ "state"
        val clientId = json \ "clientId"
        authService.getAccessToken(code.as[String], state.as[String], clientId.as[String])
        Ok("").withNewSession.withSession("signed" -> "signed")
      }
    }.getOrElse{
      BadRequest("Expecting application/json request body")
    }
  }
}