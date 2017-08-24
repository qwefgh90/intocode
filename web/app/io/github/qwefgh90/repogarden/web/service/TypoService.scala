package io.github.qwefgh90.repogarden.web.service

import play.api.cache._
import javax.inject._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.github.qwefgh90.repogarden.bp.github.Implicits._
import io.github.qwefgh90.repogarden.web.model.TypoStatus._
import javax.inject._
import org.eclipse.egit.github.core._
import org.eclipse.egit.github.core.service._
import org.eclipse.egit.github.core.client.GitHubClient
import scala.collection.JavaConverters._
import io.github.qwefgh90.repogarden.web.model._
import io.github.qwefgh90.repogarden.web.model.Implicits._
import scala.concurrent.duration._
import io.github.qwefgh90.repogarden.web.dao._
import scala.concurrent._
import rx.lang.scala._
import rx.lang.scala.subjects._
import play.api._
import scala.util.{Success, Failure}
import java.util.concurrent.TimeUnit
import org.languagetool.JLanguageTool
import org.languagetool.Language
import akka.actor.ActorRef
import io.github.qwefgh90.repogarden.web.actor.PubActor._

@Singleton
class TypoService @Inject() (typoDao: TypoDao, @NamedCache("typo-service-cache") cache: AsyncCacheApi, @Named("pub-actor") pubActor: ActorRef)(implicit executionContext: ExecutionContext) {
  /*
   * Start to check spells and get id of a event.
   */
  def build(typoRequest: TypoRequest, delay: Duration = Duration.Zero): Future[Long] = {
    getRunningId(typoRequest.ownerId, typoRequest.repositoryId, typoRequest.branchName).flatMap(currentIdOpt => {
      if(currentIdOpt.isDefined){
        Future{
          currentIdOpt.get
        }
      }else{
        val idFuture = getNewId(typoRequest)
        idFuture.map(parentId => {

          Future{blocking{Thread.sleep(delay.toMillis)}}.flatMap{unit =>
            checkSpell(parentId, typoRequest).flatMap{list =>
              typoDao.insertTypos(list).flatMap{numOpt =>
                val updateFuture = typoDao.updateTypoStat(parentId, TypoStatus.FINISHED, "")
                updateFuture.onSuccess{case num => 
                  pubActor ! PubMessage(parentId, Json.toJson("status" -> TypoStatus.FINISHED.toString))
                  pubActor ! TerminateMessage(parentId, None)
                }
                updateFuture.onFailure{case t =>
                  pubActor ! PubMessage(parentId, Json.toJson("status" -> TypoStatus.FAILED.toString))
                  pubActor ! TerminateMessage(parentId, None)
                  Logger.error("", t)
                }
                updateFuture
              }
            }.recover{
              case t => {
                val updateFuture = typoDao.updateTypoStat(parentId, TypoStatus.FAILED, t.toString)
                updateFuture.onSuccess{case num =>
                  pubActor ! PubMessage(parentId, Json.toJson("status" -> TypoStatus.FAILED.toString))
                  pubActor ! TerminateMessage(parentId, None)
                  Logger.error("", t)
                }
                updateFuture.onFailure{case t =>
                  pubActor ! PubMessage(parentId, Json.toJson("status" -> TypoStatus.FAILED.toString))
                  pubActor ! TerminateMessage(parentId, None)
                  Logger.error("", t)
                }
                updateFuture
              }
            }}

          parentId
        })
      }
    })
  }

  /*
   * Return a typo list, after checking spelling of nodes in treeEx.
   */
  private def checkSpell(parentId: Long, typoRequest: TypoRequest): Future[List[Typo]]  = {
    val treeEx = typoRequest.treeEx
	val langTool = new JLanguageTool(Language.AMERICAN_ENGLISH);
	langTool.activateDefaultPatternRules();
    Future{
      val visitor = new io.github.qwefgh90.repogarden.bp.github.Implicits.Visitor[TreeEntryEx, List[Typo]]{
        override var acc: List[Typo] = List[Typo]()
        override def enter(node: TreeEntryEx, stack: List[TreeEntryEx]){
          val contentOpt = scala.util.Try(node.getContent).getOrElse(None)
          if(contentOpt.isDefined){
            import io.github.qwefgh90.repogarden.extractor._
            val stream = new java.io.ByteArrayInputStream(contentOpt.get.getBytes("utf-8"))
            val comment = Extractor.extractCommentsByStream(stream, node.name).getOrElse(List())
            stream.close()

            val matches = comment.map{result =>
              val matches = langTool.check(result.comment)
              val partialList = matches.asScala.filter{ruleMatch => ruleMatch.getSuggestedReplacements.size() > 0}.map(ruleMatch => {
                val c = TypoComponent(parentId, node.entry.getPath, result.startOffset + ruleMatch.getFromPos, result.startOffset + ruleMatch.getToPos, ruleMatch.getLine, ruleMatch.getColumn, ruleMatch.getSuggestedReplacements.asScala.toList)

                Logger.debug(s"(${node.name} | ${result.startOffset} | ${result.comment.length})")
                Logger.debug(s"${result.comment.substring(c.from, c.to)} => ${c.suggestedList.toString}")
                c
              })
              partialList.toList
            }.foldRight(List[TypoComponent]())((e, z) => {e ++ z})


            val issueCount = matches.length
            val spellCheckResult = Json.toJson(matches).toString
            val highlight = ""
            val typo = Typo(parentId, node.entry.getPath, node.entry.getSha, issueCount, spellCheckResult, highlight)
            acc = typo :: acc
            }

        }
        override def leave(node: TreeEntryEx){}
      }
      treeEx.traverse(visitor)
    }
  }

  /*
   * Return a new id of a typostat.
   */
  private def getNewId(typoRequest: TypoRequest): Future[Long] = {
    val startTime = System.currentTimeMillis
    val newRecord = TypoStat(None, typoRequest.ownerId, typoRequest.repositoryId, typoRequest.branchName, typoRequest.commitSha, Some(startTime), None, "", TypoStatus.PROGRESS.toString, typoRequest.userId)
    typoDao.insertTypoStat(newRecord)
  }

  /*
   * Return a running job's id.
   */
  private def getRunningId(ownerId: Long, repositoryId: Long, branchName: String): Future[Option[Long]] = {
    typoDao.selectLastTypoStat(ownerId, repositoryId, branchName).map(typoStatOpt => {
      if(typoStatOpt.map(typoStat => TypoStatus.withName(typoStat.state) == TypoStatus.PROGRESS).getOrElse(false))
        typoStatOpt.map(_.id.get)
      else
        None
    })
  }

}
