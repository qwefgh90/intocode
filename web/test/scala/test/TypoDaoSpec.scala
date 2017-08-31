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
import io.github.qwefgh90.repogarden.web.model.{Typo, TypoStat, TypoStatus, TypoComponent}
import io.github.qwefgh90.repogarden.bp.Boilerplate._
import java.util.concurrent._
import scala.concurrent.Future
import net.sf.ehcache.CacheManager;
import play.api.Application

class TypoDaoSpec extends PlaySpec with GuiceOneAppPerSuite {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
	  .in(Mode.Test)
	  .build()

  val typoDao = app.injector.instanceOf[TypoDao]
  Await.ready(typoDao.create(), Duration(10, TimeUnit.SECONDS))

  "TypoDao" should {
    "execute CRUD operations nomally" in {
      val currentTime = System.currentTimeMillis()
      val ownerId = 1
      val repoId = 1
      val branchName = "master"
      val commitSha = "abcdsha"
      val userId = 2
      val stat = TypoStat(Option.empty, ownerId, repoId, branchName, commitSha, Some(currentTime), None, "message field", TypoStatus.PROGRESS.toString, userId)
      val result1 = typoDao.insertTypoStat(stat)
      val idx = Await.result(result1, Duration(10, TimeUnit.SECONDS))
      val typo1 = Typo(None, idx, "src/main/scala/main.scala", "treeSha", 3, "text: hello")
      val typo2 = Typo(None, idx, "src/main/scala/main.scala", "treeSha", 3, "text: hello")
      val typo3 = Typo(None, idx, "src/main/scala/main.scala", "treeSha", 3, "text: hello")
      val typoComp1 = TypoComponent(None, None, "path1", 99, 999, 1, 20, "suggest1")
      val typoComp2 = TypoComponent(None, None, "path2", 99, 999, 1, 20, "suggest2")

      val insertResult1 = typoDao.insertTypo(typo1)
      val insertResult2 = typoDao.insertTypo(typo2)

      Await.ready(insertResult1.flatMap(result => insertResult2), Duration(10, TimeUnit.SECONDS))
      val tryValue = scala.util.Try(Await.result(typoDao.selectTypoStat(ownerId, repoId, commitSha), Duration(10, TimeUnit.SECONDS)))
      assert(tryValue.isSuccess)
      assert(tryValue.get.isDefined)
      val tryList = scala.util.Try(Await.result(typoDao.selectTypos(ownerId, repoId, commitSha), Duration(10, TimeUnit.SECONDS)))
      assert(tryList.isSuccess)
      assert(tryList.get.length == 2)

      val idxList1 = Await.result(typoDao.insertTypoAndDetailList(List((typo3, List(typoComp1, typoComp2)))), Duration(10, TimeUnit.SECONDS))
      assert(idxList1.length == 1)
      val idxForTypo3 = idxList1(0)
      val tryTypos = scala.util.Try(Await.result(typoDao.selectTypos(idx), Duration(10, TimeUnit.SECONDS)))
      val tryTypoComps = scala.util.Try(Await.result(typoDao.selectTypoComponentByParentId(idxForTypo3), Duration(10, TimeUnit.SECONDS)))
      assert(tryTypos.isSuccess)
      assert(tryTypos.get.length == 3)
      assert(tryTypoComps.isSuccess)
      assert(tryTypoComps.get.length == 2)
      Logger.debug(tryTypoComps.get.toString)

      Await.ready(Future.sequence(tryTypoComps.get.map{comp =>
        typoDao.updateDisabledToTypoComponent(comp.id.get, true)
      }), Duration(5, TimeUnit.SECONDS))

      val tryUpdatedTypos = scala.util.Try(Await.result(typoDao.selectTypoComponentByParentId(idxForTypo3), Duration(10, TimeUnit.SECONDS)))

      assert(tryUpdatedTypos.isSuccess)
      tryUpdatedTypos.get.foreach{typo =>
        assert(typo.disabled == true)
      }
    }
  }
}
