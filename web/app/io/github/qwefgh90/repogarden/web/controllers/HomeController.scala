package io.github.qwefgh90.repogarden.web.controllers

import play.api.cache._
import javax.inject._
import play.api._
import play.api.mvc._
import io.github.qwefgh90.repogarden.web.controllers._
import scala.concurrent.ExecutionContext
import scala.concurrent._
import scala.concurrent.duration._
import play.api.libs.json._
import io.github.qwefgh90.repogarden.web.model.Implicits._
import io.github.qwefgh90.repogarden.web.model.UserInformation
import io.github.qwefgh90.repogarden.web.service._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(implicit ec: ExecutionContext, builder: ActionBuilder, cache: AsyncCacheApi, githubProvider: GithubServiceProvider) extends Controller {

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

  def userInfo = (builder andThen builder.UserAction) { implicit request =>
    Ok(Json.toJson(request.user))
  }

  def getOnlyRepositories = (builder andThen builder.UserAction).async { implicit request =>
    val tokenFuture = cache.get[String](request.user.getEmail)
    tokenFuture.map({ tokenOpt =>
      if(tokenOpt.isDefined){
        val githubService = githubProvider.getInstance(tokenOpt.get)
        Ok(Json.toJson(githubService.getAllRepositories))
      }else{
        Logger.warn(s"${request.user.getEmail} unauthorized")
        Unauthorized
      }
    }).recover({
      case e: Exception => {
        Logger.warn(e.toString)
        BadRequest
      }
    })
  }

  def getRepositories = (builder andThen builder.UserAction).async { implicit request =>
    val tokenFuture = cache.get[String](request.user.getEmail)
    tokenFuture.map({ tokenOpt =>
      if(tokenOpt.isDefined){
        val githubService = githubProvider.getInstance(tokenOpt.get)
        Ok(githubService.getAllRepositoriesJson)
      }else{
        Logger.warn(s"${request.user.getEmail} unauthorized")
        Unauthorized
      }
    }).recover({
      case e: Exception => {
        Logger.warn(e.toString)
        BadRequest
      }
    })
  }
}
