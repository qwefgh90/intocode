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

@Singleton
class TypoService @Inject() (typoDao: TypoDao, @NamedCache("typo-service-cache") cache: AsyncCacheApi)(implicit executionContext: ExecutionContext) {
  def build(typoRequest: TypoRequest): Future[Long] = {
    getRunningId(typoRequest.ownerId, typoRequest.repositoryId, typoRequest.branchName).flatMap(currentIdOpt => {
      if(currentIdOpt.isDefined){
        Future{
          currentIdOpt.get
        }
      }else{
        val idFuture = getNewId(typoRequest)
        idFuture.map(parentId => {
          val checkFuture = checkSpell(parentId, typoRequest)
          checkFuture.flatMap{list => 
            typoDao.insertTypos(list).flatMap{numOpt =>
              typoDao.updateTypoStat(parentId, TypoStatus.FINISHED, "")
            }
          }.recover{
            case t => {
              typoDao.updateTypoStat(parentId, TypoStatus.FAILED, t.toString)
              Logger.error("", t)
            }
          }
          parentId
        })
      }
    })
  }

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
            val matches = langTool.check(contentOpt.get)
            val issueCount = matches.size()
            val matchesToSave = matches.asScala.map(ruleMatch => {
              TypoComponent(parentId, node.entry.getPath, ruleMatch.getFromPos, ruleMatch.getToPos, ruleMatch.getLine, ruleMatch.getColumn, ruleMatch.getSuggestedReplacements.asScala.toList)
            })
            val spellCheckResult = Json.toJson(matchesToSave).toString
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

  private def getNewId(typoRequest: TypoRequest): Future[Long] = {
    val startTime = System.currentTimeMillis
    val newRecord = TypoStat(None, typoRequest.ownerId, typoRequest.repositoryId, typoRequest.branchName, typoRequest.commitSha, Some(startTime), None, "", TypoStatus.PROGRESS.toString, typoRequest.userId)
    typoDao.insertTypoStat(newRecord)
  }

  private def getRunningId(ownerId: Long, repositoryId: Long, branchName: String): Future[Option[Long]] = {
    typoDao.selectLastTypoStat(ownerId, repositoryId, branchName).map(typoStatOpt => {
      if(typoStatOpt.map(typoStat => TypoStatus.withName(typoStat.state) == TypoStatus.PROGRESS).getOrElse(false))
        typoStatOpt.map(_.id.get)
      else
        None
    })
  }

}
