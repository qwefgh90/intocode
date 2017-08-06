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
  typoDao.create()

  "SwitchDao" should {
    "execute CRUD operations nomally" in {
      val currentTime = System.currentTimeMillis()
      val repoId = "1"
      val commitSha = "abcdsha"
      val stat = TypoStat(Option.empty, repoId, commitSha, currentTime, "message field")
      val result1 = typoDao.insertTypoStat(stat)
      val idx = Await.result(result1, Duration(10, TimeUnit.SECONDS))
      val typo1 = Typo(idx, "src/main/scala/main.scala", "treeSha", 3, "{a:123}", "text: hello")
      val typo2 = Typo(idx, "src/main/scala/main.scala", "treeSha", 3, "{a:123}", "text: hello")
      val insertResult1 = typoDao.insertTypo(typo1)
      val insertResult2 = typoDao.insertTypo(typo2)
      Await.ready(insertResult1.flatMap(result => insertResult2), Duration(10, TimeUnit.SECONDS))
      val tryValue = scala.util.Try(Await.result(typoDao.selectTypoStat(repoId, commitSha), Duration(10, TimeUnit.SECONDS)))
      assert(tryValue.isSuccess)
      assert(tryValue.get.isDefined)
      val tryList = scala.util.Try(Await.result(typoDao.selectTypoList(repoId, commitSha), Duration(10, TimeUnit.SECONDS)))
      assert(tryList.isSuccess)
      assert(tryList.get.length == 2)
    }
  }
}
