package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import model._
import model.Implicits._

@Singleton
class AuthController @Inject() extends Controller {
  
  def client = Action { implicit request => 
    val client_id = "github client id"
    val state = "unsuggestable random string"
    Ok(Json.toJson(new InitialVector(client_id, state)))
  }
    
}