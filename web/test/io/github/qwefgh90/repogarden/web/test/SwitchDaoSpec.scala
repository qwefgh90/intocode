package io.github.qwefgh90.repogarden.web.test
import play.api.inject.ApplicationLifecycle
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

class SwitchDaoSpec extends PlaySpec {

  val app = new GuiceApplicationBuilder()
	.in(Mode.Test)
    .build()

  val switchDao = app.injector.instanceOf[SwitchDao]

  "SwitchDao" should {

    Await.result(switchDao.create(), Duration(10, TimeUnit.SECONDS))
    Logger.debug("tables is ready")

    "return all switches" in {
      Await.result(
      switchDao.insert(Switch("123414913849", "1023002310", false)), Duration(10, TimeUnit.SECONDS))

      val switches = Await.result(
       switchDao.all(), Duration(10, TimeUnit.SECONDS))
      Logger.debug(switches.toString)

    }

  }

}
