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
import io.github.qwefgh90.repogarden.web.model.Switch
import io.github.qwefgh90.repogarden.bp.Boilerplate._
import java.util.concurrent._
import scala.concurrent.Future
import net.sf.ehcache.CacheManager;
import play.api.Application

class SwitchDaoSpec extends PlaySpec with GuiceOneAppPerSuite {
  //it's hack for solving a global cache manager issue
  //Before test, app must shutdown a instance of CacheManager
  //CacheManager.getInstance().shutdown();
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
	  .in(Mode.Test)
	  .build()

  val switchDao = app.injector.instanceOf[SwitchDao]

  "SwitchDao" should {
    Await.result(switchDao.create(), Duration(10, TimeUnit.SECONDS))
    val switch1 = Switch("123414913849", "1023002310", false)
    val switch2 = Switch("3423423", "3242425", true)
    val switch3 = Switch("5543334", "646434", false)
    Await.result(switchDao.insert(switch1), Duration(10, TimeUnit.SECONDS))
    Await.result(switchDao.insert(switch2), Duration(10, TimeUnit.SECONDS))
    Await.result(switchDao.insert(switch3), Duration(10, TimeUnit.SECONDS))
    Logger.debug("tables is ready")

    "return all switches" in {
      val switches = Await.result(
        switchDao.all(), Duration(10, TimeUnit.SECONDS))
      assert(switches.toSet.equals(Set(switch3, switch2, switch1)))
      Logger.debug(switches.toString)
    }

    "edit and compare values " in {
      Await.result(switchDao.updateSwitch(switch1.userId, switch1.repositoryId, !switch1.yn), Duration(10, TimeUnit.SECONDS))
      val returned = Await.result(
        switchDao.select(switch1.userId, switch1.repositoryId), Duration(10, TimeUnit.SECONDS)).get
      Logger.debug(s"returned: ${returned.toString}")
      assert(returned.yn == !switch1.yn)
    }
  }
}
