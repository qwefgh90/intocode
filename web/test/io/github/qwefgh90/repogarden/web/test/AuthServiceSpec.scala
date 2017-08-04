package io.github.qwefgh90.repogarden.web.test

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import org.scalatest._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.test.Helpers._
import java.io._
import play.api.Mode
import io.github.qwefgh90.repogarden.web.service._
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
import scala.concurrent.ExecutionContext.Implicits.global
import net.sf.ehcache.CacheManager
import play.api.Application

/**
  * Add your spec here.
  * You can mock out a whole application including requests, plugins etc.
  *
  * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
  */
class AuthServiceSpec extends PlaySpec with GuiceOneAppPerSuite {
  //it's hack for solving a global cache manager issue
  //Before test, app must shutdown a instance of CacheManager
  //CacheManager.getInstance().shutdown();
  
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
	.in(Mode.Test)
	.build()

  val encryption = app.injector.instanceOf[Encryption]
  val authService = app.injector.instanceOf[AuthService]
  val configuration = app.injector.instanceOf[Configuration]
  val context = app.injector.instanceOf[ExecutionContext]

  "AuthService" should {
	"return access token" in {
	  import play.api.routing.sird._
	  Server.withRouter() {
        case play.api.routing.sird.POST(p"/login/oauth/access_token") => Action {
          Results.Ok(Json.obj("access_token" -> "fake token","token_type" -> "type"))
        }
      } { implicit port =>
        WsTestClient.withClient { clientToMock =>
          val authService = new AuthService(configuration, encryption, clientToMock, context, "")
          val result = Await.result(authService.getAccessToken("code", "state", "clientId"), 10.seconds)
          assert(result.equals("fake token"))
        }
      }
	}
  }
  
  "Encryption" should {

    "encrypt plain text and then decrypt" in {
      val plainText = "HelloWorld"
      val encrypted = encryption.encrypt(plainText.getBytes)
      val decrypted = encryption.decrypt(encrypted)
      assert(Base64.getEncoder.encodeToString(plainText.getBytes).equals(Base64.getEncoder.encodeToString(decrypted)))
      assert(authService.getState.length > 0)
    }
  }
}
