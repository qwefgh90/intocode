package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import org.scalatest._
import play.api.inject.guice.GuiceBuilder
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.test.Helpers._
import java.io._
import play.api.Mode
import util._
import play.api.Logger
import java.util.Base64
//mock service
import play.api.mvc._
import play.core.server.Server
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.json._
import play.api.Configuration
import scala.concurrent._


/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class HomeControllerSpec extends PlaySpec with GuiceOneAppPerSuite  {
	val application = new GuiceApplicationBuilder()
			.in(new File("application.conf"))
			.in(Mode.Test)
			.build()

	val encryption = application.injector.instanceOf[Encryption]
	val authService = application.injector.instanceOf[AuthService]
	val configuration = application.injector.instanceOf[Configuration]
	val context = application.injector.instanceOf[ExecutionContext]
	
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

  "HomeController GET" should {

    "render the index page from a new instance of controller" in {
      val controller = new HomeController
      val home = controller.index().apply(FakeRequest())

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("Welcome to Play")
    }

    "render the index page from the application" in {
      val controller = app.injector.instanceOf[HomeController]
      val home = controller.index().apply(FakeRequest())

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("Welcome to Play")
    }
/*
    "render the index page from the router" in {
      // Need to specify Host header to get through AllowedHostsFilter
      val request = FakeRequest(GET, "/").withHeaders("Host" -> "localhost")
      val home = route(app, request).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("Welcome to Play")
    }*/
  }
}
