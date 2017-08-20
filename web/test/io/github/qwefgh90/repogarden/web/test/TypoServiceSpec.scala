package io.github.qwefgh90.repogarden.web.test
import play.api.cache._
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import org.scalatest._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.test.Helpers._
import java.io._
import play.api.Mode
import io.github.qwefgh90.repogarden.web.dao._
import java.util.Base64
import play.api.mvc._
import play.core.server.Server
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.json._
import play.api.Configuration
import scala.concurrent._
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.routing.sird.UrlContext
import io.github.qwefgh90.repogarden.web.controllers._
import play.Logger
import net.sf.ehcache.CacheManager;
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util._
import org.eclipse.egit.github.core._
import io.github.qwefgh90.repogarden.web.model.Implicits._
import io.github.qwefgh90.repogarden.web.model.Typo
import io.github.qwefgh90.repogarden.web.model.TypoStat
import io.github.qwefgh90.repogarden.web.model.TypoStatus
import io.github.qwefgh90.repogarden.web.model.TypoRequest
import io.github.qwefgh90.repogarden.bp.Boilerplate._
import java.util.concurrent._
import scala.concurrent.Future
import net.sf.ehcache.CacheManager;
import play.api.Application
import io.github.qwefgh90.repogarden.web.service.GithubServiceProvider
import io.github.qwefgh90.repogarden.web.service.TypoService

class TypoServiceSpec extends FlatSpec with Matchers with GuiceOneAppPerSuite {
  val oauthToken = System.getProperties.getProperty("oauthToken")
  val oauthTokenOpt = if(oauthToken == null)
	Option.empty
  else
	Option(oauthToken)
  
  require(oauthTokenOpt.isDefined)
  
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
	.in(Mode.Test)
	.build()

  val provider = app.injector.instanceOf[GithubServiceProvider]
  val typoService = app.injector.instanceOf[TypoService]
  val typoDao = app.injector.instanceOf[TypoDao]
  val switchDao = app.injector.instanceOf[SwitchDao]
  val githubService = provider.getInstance(oauthToken)

  typoDao.create()
  switchDao.create()

  "TypoService" should "start finding typos" in {
/*    val repoOpt = githubService.getRepository("qwefgh90", "RepogardenTest")
assert(repoOpt.isDefined)
    val repo = repoOpt.get
    val ownerId: Long = repo.getOwner.getId
    val repositoryId = repo.getId
    val branchName = "master"
    val commit = githubService.getLastestCommitByBranchName(repo, branchName).get
    val userId: Long = githubService.getUser.getId
    val treeEx = githubService.getTree(repo, commit.getCommit.getTree.getSha).get
    TypoRequest(ownerId, repositoryId, branchName, commit.getSha, userId, treeEx)
*/

    val req = TypoRequest.createLastRequest(githubService, "qwefgh90", "RepogardenTest", "master")
    Logger.debug(req.toString)
    val future = typoService.build(req)
    val id = Await.result(future, Duration(60, TimeUnit.SECONDS))
    /*
    val typoList = Await.result(typoDao.selectTypoList(id), Duration(60, TimeUnit.SECONDS))
    typoList.foreach(typo => Logger.debug(typo.components))*/
  }
}
