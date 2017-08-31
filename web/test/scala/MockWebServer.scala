package io.github.qwefgh90.repogarden.web.test

import play.api.inject.{ApplicationLifecycle, BindingKey, bind}
import play.api.cache._
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import org.scalatest._
import org.scalatest.concurrent.{Eventually}
import org.scalatest.concurrent.Eventually._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.test.Helpers._
import java.io._
import play.api.Mode
import io.github.qwefgh90.repogarden.web.service._
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
import io.github.qwefgh90.repogarden.bp.Boilerplate._
import play.api.Application
import java.util.concurrent._

trait MockWebServer extends PlaySpec with BeforeAndAfterAll {
  val port = 15551
  val app: Application =
    new GuiceApplicationBuilder()
      .configure("play.acceptOriginList" -> Seq("localhost:" + port))
      .overrides(bind[UserSessionChecker].to[MockUserSessionChecker])
	  .in(Mode.Test)
	  .build()
  val tserver = TestServer(port, app)

  override def beforeAll() {
    tserver.start()
    // start up your web server or whatever
  }

  override def afterAll() {
    tserver.stop()
    app.stop() // shut down the web server
  }
}
