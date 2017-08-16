package io.github.qwefgh90.repogarden.web.service

import play.api.cache._
import javax.inject._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.github.qwefgh90.repogarden.bp.github.Implicits._
import io.github.qwefgh90.repogarden.web.model.State._
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

@Singleton
class TypoService @Inject() (typoDao: TypoDao, @NamedCache("typo-service-cache") cache: AsyncCacheApi)(implicit executionContext: ExecutionContext) {

  def build(typoRequest: TypoRequest): Future[Long] = {
    getProgress(typoRequest.ownerId, typoRequest.repositoryId, typoRequest.branchName).map(currentIdOpt => {
      currentIdOpt.getOrElse({
        //new job
        val startTime = System.currentTimeMillis
        val newRecord = TypoStat(None, typoRequest.ownerId, typoRequest.repositoryId, typoRequest.branchName, typoRequest.commitSha, Some(startTime), None, "", State.PROGRESS.toString, typoRequest.userId)
        val idFuture = typoDao.insertTypoStat(newRecord)
        val id = Await.result(idFuture, Duration(10, TimeUnit.SECONDS))
        val futureList = startNewProcess(id, typoRequest.treeEx)
        futureList.onComplete{
          case Success(list) => typoDao.insertTypos(list)
          case Failure(t) => Logger.error(t.toString)
        }
        id
      })
    })
  }

  def startNewProcess(parentId: Long, treeEx: TreeEx): Future[List[Typo]]  = {
    Future{
      val visitor = new io.github.qwefgh90.repogarden.bp.github.Implicits.Visitor[TreeEntryEx, List[Typo]]{
        override var acc: List[Typo] = List[Typo]()
        override def enter(node: TreeEntryEx, stack: List[TreeEntryEx]){
          val content = node.getContent
          val issueCount = 0
          val spellCheckResult = ""
          val highlight = ""
          val typo = Typo(parentId, node.entry.getPath, node.entry.getSha, issueCount, spellCheckResult, highlight)
          acc = typo :: acc
        }
        override def leave(node: TreeEntryEx){}
      }
      treeEx.traverse(visitor)
    }
  }

  def getProgress(ownerId: String, repositoryId: String, branchName: String): Future[Option[Long]] = {
    typoDao.selectLastTypoStat(ownerId, repositoryId, branchName).map(typoStatOpt => {
      if(typoStatOpt.map(typoStat => State.withName(typoStat.state) == State.PROGRESS).getOrElse(false))
        typoStatOpt.map(_.id.get)
      else
        None
    })
  }

}
