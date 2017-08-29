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

class WebsocketSpec extends MockWebServer {
  "Websocket controller" should {
    import io.github.qwefgh90.repogarden.web.actor.{PubActor, ClientActor}
    import io.github.qwefgh90.repogarden.web.actor.PubActor._
    import io.backchat.hookup._
    import scala.collection.mutable._
    import akka.actor.ActorRef
    import akka.actor.{Actor, Props}
    import akka.actor.ActorSystem
    import java.net.URI

    val actorRef = app.injector.instanceOf(BindingKey(classOf[ActorRef]).qualifiedWith("pub-actor"))

    "accept a client and terminate a connection" in {
      val channelId: Long = 1234
      val hookupClient = new DefaultHookupClient(HookupClientConfig(URI.create(s"ws://localhost:${port}/ws?ch=${channelId}"))) {
        val messages = ListBuffer[String]()

        def receive = {
          case Connected =>{
            Logger.debug("Connected")
            actorRef ! PubMessage(channelId, Json.parse("""{"id":"1234"}"""))
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
      import org.scalatest.concurrent.PatienceConfiguration.Timeout
      import org.scalatest.time.{Span, Seconds}
      eventually(new Timeout(Span(2, Seconds))){
        hookupClient.messages.exists(e => e.contains("1234")) mustBe true
      }

      actorRef ! TerminateMessage(channelId, None)
      eventually(new Timeout(Span(2, Seconds))){
        hookupClient.messages.exists(e => e.contains("disconnected")) mustBe true
      }
    }

    "accept a client" in {
      val channelId: Long = 1234
      val hookupClient = new DefaultHookupClient(HookupClientConfig(URI.create(s"ws://localhost:${port}/ws?ch=${channelId}"))) {
        val messages = ListBuffer[String]()

        def receive = {
          case Connected =>{
            Logger.debug("Connected")
            actorRef ! PubMessage(channelId, Json.parse("""{"id":"1234"}"""))
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
      import org.scalatest.concurrent.PatienceConfiguration.Timeout
      import org.scalatest.time.{Span, Seconds}
      eventually(new Timeout(Span(2, Seconds))){
        hookupClient.messages.exists(e => e.contains("1234")) mustBe true
      }

      actorRef ! TerminateMessage(channelId, None)
      eventually(new Timeout(Span(2, Seconds))){
        hookupClient.messages.exists(e => e.contains("disconnected")) mustBe true
      }
    }
  }
}
