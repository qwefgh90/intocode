package io.github.qwefgh90.repogarden.web.controllers

import javax.inject._
import play.api._
import play.api.mvc._
import io.github.qwefgh90.repogarden.web.controllers._
import scala.concurrent.ExecutionContext
import scala.concurrent._
import play.api.libs.json._
import io.github.qwefgh90.repogarden.web.model.Implicits._
import io.github.qwefgh90.repogarden.web.model.UserInformation

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(ec: ExecutionContext, builder: ActionBuilder) extends Controller {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action { implicit request =>
    Ok(io.github.qwefgh90.repogarden.web.views.html.index()).withNewSession
  }
  
  def userInfo = (builder andThen builder.UserAction(ec)) { implicit request =>
    Ok(Json.toJson(new UserInformation("userid")))
  }
}
