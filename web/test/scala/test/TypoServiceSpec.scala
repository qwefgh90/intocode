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

import org.scalatest.concurrent.PatienceConfiguration.{Timeout, Interval}
import org.scalatest.time.{Span, Seconds}
import org.scalatest.concurrent.{Eventually}
import org.scalatest.concurrent.Eventually._
import io.backchat.hookup._
import scala.collection.mutable._
import akka.actor.ActorRef
import akka.actor.{Actor, Props}
import akka.actor.ActorSystem
import java.net.URI

class TypoServiceSpec extends MockWebServer {//extends PlaySpec with GuiceOneAppPerSuite {
  val oauthToken = System.getProperties.getProperty("oauthToken")
  val oauthTokenOpt = if(oauthToken == null)
	Option.empty
  else
	Option(oauthToken)
  
  require(oauthTokenOpt.isDefined)
  
  /*override def fakeApplication(): Application = new GuiceApplicationBuilder()
	.in(Mode.Test)
	.build()*/

  val provider = app.injector.instanceOf[GithubServiceProvider]
  val typoService = app.injector.instanceOf[TypoService]
  val typoDao = app.injector.instanceOf[TypoDao]
  val switchDao = app.injector.instanceOf[SwitchDao]
  val githubService = provider.getInstance(oauthToken)

  typoDao.create()
  switchDao.create()

  "TypoService" should {
    "start finding typos and subscribe messages" in {

      val req = TypoRequest.createLastRequest(githubService, "repogarden", "repogarden-test", "master").get
      Logger.debug(req.toString)
      val future = typoService.build(req, Duration(2, TimeUnit.SECONDS))
      val id = Await.result(future, Duration(20, TimeUnit.SECONDS))

      val hookupClient = new DefaultHookupClient(HookupClientConfig(URI.create(s"ws://localhost:${port}/ws?ch=${id}"))) {
        val messages = ListBuffer[String]()

        def receive = {
          case Connected =>{
            Logger.debug("Connected")
          }
          case Disconnected(_) =>
            messages += "disconnected"
            Logger.debug("Disconnected")

          case JsonMessage(json) =>
            messages += json.toString
            Logger.debug("Json message = " + json.toString)

          case TextMessage(text) =>
            messages += text
            Logger.debug("Text message = " + text)
        }

        connect() onSuccess {
          case Success => {
            Logger.debug("Send text message")
          }
        }
      }

      eventually(new Timeout(Span(60, Seconds))){
        hookupClient.messages.exists(e => e.contains("FINISHED")) mustBe true
      }

      eventually(new Timeout(Span(6, Seconds)), new Interval(Span(60, Seconds))){

      }

      val typoList = Await.result(typoDao.selectTypos(id), Duration(10, TimeUnit.SECONDS))
      assert(typoList.length > 1)
    }
  }
}
