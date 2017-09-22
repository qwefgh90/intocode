package io.github.qwefgh90.repogarden.web.service

import play.api.cache._
import javax.inject._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.github.qwefgh90.repogarden.bp.github.Implicits._
import io.github.qwefgh90.repogarden.bp.Boilerplate._
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
import org.languagetool.language.AmericanEnglish
import org.languagetool.Language
import org.languagetool.rules._
import org.languagetool._
import org.languagetool.rules.spelling.SpellingCheckRule
import akka.actor.ActorRef
import io.github.qwefgh90.repogarden.web.actor.PubActor._
import io.github.qwefgh90.jsearch.JSearch
import java.nio.file.Files

class TypoService @Inject() (typoDao: TypoDao, @NamedCache("typo-service-cache") cache: AsyncCacheApi, @Named("pub-actor") pubActor: ActorRef)(implicit executionContext: ExecutionContext) {

  
  /*
   * Start to check spells and get id for a event.
   */
  def build(typoRequest: TypoRequest, delay: Duration = Duration.Zero): Future[Long] = {
    getRunningId(typoRequest).flatMap(currentIdOpt => {
      if(currentIdOpt.isDefined){
        Future{
          currentIdOpt.get
        }
      }else{
        val idFuture = getFailedId(typoRequest)
          .flatMap{failedIdOpt =>
            if(failedIdOpt.isEmpty)
              getNewId(typoRequest)
            else
              Future(failedIdOpt.get)
          }
        idFuture.onSuccess{case parentId => {
          Future{blocking{Thread.sleep(delay.toMillis)}}.flatMap{unit =>
            checkSpell(parentId, typoRequest).flatMap{list => 
              typoDao.insertTypoAndDetailList(list).flatMap{numOpt =>
                val updateFuture = typoDao.updateTypoStat(parentId, TypoStatus.FINISHED, "", Some(System.currentTimeMillis))
                updateFuture.onSuccess{case num => 
                  pubActor ! PubMessage(parentId, Json.toJson(Map("status" -> TypoStatus.FINISHED.toString)))
                  pubActor ! TerminateMessage(parentId, None)
                }
                updateFuture.onFailure{case t =>
                  typoDao.deleteTypos(parentId).onFailure{case t =>
                    Logger.error(s"Fail to clean up typos of ${parentId}", t)
                  }
                  pubActor ! PubMessage(parentId, Json.toJson(Map("status" -> TypoStatus.FAILED.toString)))
                  pubActor ! TerminateMessage(parentId, None)
                  Logger.error("", t)
                }
                updateFuture
              }
            }.recover{
              case t => {
                val updateFuture = typoDao.updateTypoStat(parentId, TypoStatus.FAILED, t.toString, Some(System.currentTimeMillis))
                updateFuture.onSuccess{case num =>
                  pubActor ! PubMessage(parentId, Json.toJson(Map("status" -> TypoStatus.FAILED.toString)))
                  pubActor ! TerminateMessage(parentId, None)
                  Logger.error("", t)
                }
                updateFuture.onFailure{case t =>
                  pubActor ! PubMessage(parentId, Json.toJson(Map("status" -> TypoStatus.FAILED.toString)))
                  pubActor ! TerminateMessage(parentId, None)
                  Logger.error("", t)
                }
              }
            }}
        }}
        idFuture
      }
    })
  }

  /*
   * Return a typo list, after checking spelling of nodes in treeEx.
   */
  private def checkSpell(parentId: Long, typoRequest: TypoRequest): Future[List[(Typo, List[TypoComponent])]]  = {
    val treeEx = typoRequest.treeEx
	val langTool = new JLanguageTool(new AmericanEnglish());
    langTool.getAllRules().asScala.foreach{rule =>
      if (!rule.isDictionaryBasedSpellingRule()) {
        langTool.disableRule(rule.getId());
      }
      if (rule.isInstanceOf[SpellingCheckRule]) {
        val wordsToIgnore = List("@author");
        (rule.asInstanceOf[SpellingCheckRule]).addIgnoreTokens(wordsToIgnore.asJava);
      }
    }
    //    langTool.disableCategory(new CategoryId("TYPOGRAPHY"))
    //    langTool.disableCategory(new CategoryId("CAPITALIZATION"))
    //    langTool.disableCategory(new CategoryId("PUNCTUATION"))
    //    langTool.disableRule("UPPERCASE_SENTENCE_START")
    //    langTool.getAllActiveRules().asScala.foreach{r=> Logger.debug(s"rule: ${r.getId} | ${r.getCategory.getId}")}

    Future{
      val visitor = new io.github.qwefgh90.repogarden.bp.github.Implicits.Visitor[TreeEntryEx, List[(Typo, List[TypoComponent])]]{
        override var acc: List[(Typo, List[TypoComponent])] = List[(Typo, List[TypoComponent])]()
        override def enter(node: TreeEntryEx, stack: List[TreeEntryEx]){
          val bytesOpt = scala.util.Try(node.getBytes)
          if(bytesOpt.isSuccess){
            import io.github.qwefgh90.repogarden.extractor._
            val bytes = bytesOpt.get
            val content = bytesToReadableString(bytes, node.name)
/*
            val tempPath = Files.createTempFile("for_extract", node.name)
            Files.write(tempPath, bytes)

            val content = JSearch.extractContentsFromFile(tempPath.toFile)
            Files.delete(tempPath)*/

            val stream = new java.io.ByteArrayInputStream(content.getBytes)
            val comment = Extractor.extractCommentsByStream(stream, node.name).getOrElse(List())
            stream.close()

            val matches: List[TypoComponent] = comment.map{result =>
              val matches = langTool.check(result.comment)
              matches.asScala.filter{ruleMatch => ruleMatch.getSuggestedReplacements.size() > 0}.map(ruleMatch => {
                val c = TypoComponent(None, None, node.entry.getPath, result.startOffset + ruleMatch.getFromPos, result.startOffset + ruleMatch.getToPos, ruleMatch.getLine, ruleMatch.getColumn, Json.toJson(ruleMatch.getSuggestedReplacements.asScala.toList).toString)

               Logger.debug(s"(${node.name} | ${result.startOffset} | ${c.from} | ${c.to} | ${result.comment.length} | ${ruleMatch.getRule.getId.toString} | ${ruleMatch.getRule.getCategory.getId.toString} | ${node.entry.getSha})")
               Logger.debug(s"${content.substring(c.from, c.to)} => ${c.suggestedList.toString}")
                c
              }).toList
            }.foldRight(List[TypoComponent]())((e, acc) => {e ++ acc})

            val issueCount = matches.length
            val spellCheckResult = matches
            val highlight = ""
            val typo = Typo(None, parentId, node.entry.getPath, node.entry.getSha, issueCount, highlight)
            acc = (typo, matches) :: acc
            }
        }
        override def leave(node: TreeEntryEx){}
      }
      treeEx.traverse(visitor)
    }
  }

  /*
   * Return a id of a new typostat.
   */
  private def getNewId(typoRequest: TypoRequest): Future[Long] = {
    val startTime = System.currentTimeMillis
    val newRecord = TypoStat(None, typoRequest.ownerId, typoRequest.repositoryId, typoRequest.branchName, typoRequest.commitSha, Some(startTime), None, "", TypoStatus.PROGRESS.toString, typoRequest.userId)
    typoDao.insertTypoStat(newRecord)
  }

  /*
   * Return a running job's id whose status is progress.
   */
  private def getRunningId(typoRequest: TypoRequest): Future[Option[Long]] = {
    typoDao.selectTypoStats(typoRequest.ownerId, typoRequest.repositoryId, typoRequest.branchName, typoRequest.userId, TypoStatus.PROGRESS).map(typoStatSeq => {
      if(typoStatSeq.length > 0)
        typoStatSeq.head.id
      else None
    })
  }

  /*
   * Return a running job's id whose status is failed.
   */
  private def getFailedId(typoRequest: TypoRequest): Future[Option[Long]] = {
    typoDao.selectTypoStats(typoRequest.ownerId, typoRequest.repositoryId, typoRequest.branchName, typoRequest.userId, TypoStatus.FAILED).map(typoStatSeq => {
      if(typoStatSeq.length > 0)
        typoStatSeq.head.id
      else None
    })
  }
}
