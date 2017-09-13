package io.github.qwefgh90.repogarden.web.controllers

import play.api.cache._
import java.io.IOException
import javax.inject._
import play.api._
import play.api.mvc._
import io.github.qwefgh90.repogarden.web.controllers._
import scala.concurrent.ExecutionContext
import scala.concurrent._
import scala.concurrent.duration._
import play.api.libs.json._
import play.api.libs.json.Writes._
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
class TypoController @Inject()(builder: ActionBuilder, cache: AsyncCacheApi, githubProvider: GithubServiceProvider, typoService: TypoService, typoDao: TypoDao, switchDao: SwitchDao, @Named("pub-actor") pubActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext, implicit val actorSystem: ActorSystem, materializer: Materializer) extends Controller with SameOriginCheck {

  /*
   * Find typo in a last commit.
   */
  def buildLastCommit(owner: String, name: String, branchName: String) = (builder andThen builder.UserAction andThen builder.PermissionCheckAction(owner, name)).async {request =>
    val githubService = githubProvider.getInstance(request.token)
    val reqOpt = TypoRequest.createLastRequest(githubService, owner, name, branchName)
    reqOpt.map{req =>
      typoService.build(req, Duration(2, TimeUnit.SECONDS)).map{id =>
        Ok(Json.obj("id" -> id))
      }
    }.getOrElse{
      Future{
        BadRequest("invalid paremters")
      }
    }
  }

  /*
   * Get a list of typostats in the branch.
   */
  def getTypoStats(owner: String, name: String, branchName: String) = (builder andThen builder.UserAction andThen builder.PermissionCheckAction(owner, name)).async { implicit request =>
    //need validate!!

    val githubService = githubProvider.getInstance(request.token)
    val repositoryOpt = githubService.getRepository(owner, name)

    repositoryOpt.map{repository =>
      val futureToSend = typoDao.selectTypoStats(repository.getOwner.getId, repository.getId, branchName, request.user.getId).map{
        _.map{typoStat =>
          val sha = typoStat.commitSha
          val commitOpt = githubService.getCommit(repository, sha)
          commitOpt.map{(typoStat, _)}
        }.collect{case v if v.isDefined => Json.toJson(v.get)(typoStatsWritesToBrowser)}
      }
      futureToSend.map{list => Ok(Json.toJson(list))}.recover{
        case e: Exception => {
          Logger.error("", e)
          InternalServerError("error occurs. we would handle this issue soon.")
        }
      }
    }.getOrElse(Future{BadRequest("invalid parameters")})
  }

  /*
   * Get positions of typo and suggested word list.
   */
  def getTypos(owner: String, name: String, branchName: String, typoStatId: Long) = (builder andThen builder.UserAction andThen builder.TypoStatPermissionCheckAction(typoStatId)).async { implicit request =>

    val githubService = githubProvider.getInstance(request.token)
    val repositoryOpt = githubService.getRepository(owner, name)

    typoDao.selectTypos(typoStatId).flatMap{ typos =>
      Future.sequence(typos.map{typo =>
        repositoryOpt.flatMap{repository => 
          githubService.getContentByTreeSha(repository, typo.treeSha).map{body =>
            typoDao.selectTypoComponents(typo.id.get).map{ components =>
              (typo, components, body)
            }
          }
        }.getOrElse(Future.failed(new IOException(s"the repository(${owner}, ${name}) does not exist.")))
      })
    }.map{tupleList => Ok(Json.toJson(tupleList)(seq(typoAndComponentsToBrowser)))
    }.recover{
      case t =>
        Logger.error("",t)
        InternalServerError
    }
  }

  /*
   * Put up a disable flag of a component.
   */
  def disableTypoComponent(typoComponentId: Long, disabled: Boolean) = (builder andThen builder.UserAction andThen builder.TypoCompPermissionCheckAction(typoComponentId)).async { implicit request =>
    typoDao.updateDisabledToTypoComponent(typoComponentId, disabled).map{count => 
        Ok
    }.recover{
      case t =>
        Logger.error("",t)
        InternalServerError
    }
  }
}
