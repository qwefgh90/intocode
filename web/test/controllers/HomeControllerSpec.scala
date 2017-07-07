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

    "render the index page from the router" in {
      // Need to specify Host header to get through AllowedHostsFilter
      val request = FakeRequest(GET, "/").withHeaders("Host" -> "localhost")
      val home = route(app, request).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("Welcome to Play")
    }
  }
}
