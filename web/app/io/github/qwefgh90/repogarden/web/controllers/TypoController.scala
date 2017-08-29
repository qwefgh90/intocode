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
import io.github.qwefgh90.repogarden.web.model.{UserInformation, TypoRequest}
import io.github.qwefgh90.repogarden.web.service._
import io.github.qwefgh90.repogarden.web.dao._
import io.github.qwefgh90.repogarden.web.actor._
import play.api.libs.streams.ActorFlow
import akka.actor._
import akka.stream.Materializer
import java.util.concurrent.TimeUnit

/**
  * This controller creates actions to find typo or retreive typo data and status.
  */
class TypoController @Inject()(builder: ActionBuilder, cache: AsyncCacheApi, githubProvider: GithubServiceProvider, typoService: TypoService, switchDao: SwitchDao, @Named("pub-actor") pubActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext, implicit val actorSystem: ActorSystem, materializer: Materializer) extends Controller with SameOriginCheck {

  def buildLastCommit(owner: String, name: String, branchName: String) = (builder andThen builder.UserAction).async {request =>
    val githubService = githubProvider.getInstance(request.token)
    val reqOpt = TypoRequest.createLastRequest(githubService, owner, name, branchName)
    reqOpt.map{req =>
      typoService.buildLastCommit(req, Duration(2, TimeUnit.SECONDS)).map{id =>
        Ok(Json.obj("id" -> id))
      }
    }.getOrElse{
      Future{
        BadRequest("invalid paremters")
      }
    }
  }
}
