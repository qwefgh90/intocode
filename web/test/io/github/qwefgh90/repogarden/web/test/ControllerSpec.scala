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
import io.github.qwefgh90.repogarden.web.controllers._
import play.Logger

class ControllerSpec extends PlaySpec with GuiceOneAppPerSuite  {
  
  val homeController = app.injector.instanceOf[HomeController]
	val encryption = app.injector.instanceOf[Encryption]
	val authService = app.injector.instanceOf[AuthService]
	val configuration = app.injector.instanceOf[Configuration]
	val context = app.injector.instanceOf[ExecutionContext]
  
  "Controller" should {
    "return unauthorized code to a unauthenticated user" in {
      val userInfoResponse = homeController.userInfo.apply(FakeRequest())
      status(userInfoResponse) mustBe UNAUTHORIZED
      session(userInfoResponse).isEmpty mustBe true
          
      val userInfoResponse2 = homeController.userInfo.apply(FakeRequest().withSession("signed" -> "no signed"))
      status(userInfoResponse2) mustBe UNAUTHORIZED
    }
    
    "return ok to a authenticated user" in {
       val userInfo = homeController.userInfo.apply(FakeRequest().withSession("signed" -> "signed"))
       status(userInfo) mustBe OK
    }
    
    "return session data to a authenticated user" in {
      Server.withRouter() {
        case play.api.routing.sird.POST(p"/login/oauth/access_token") => Action {
          Results.Ok(Json.obj("access_token" -> "fake token","token_type" -> "type"))
        }
      } { implicit port => 
        WsTestClient.withClient { clientToMock =>
          val authService = new AuthService(configuration, encryption, clientToMock, context, "")
          val authController = new AuthController(authService)
          val fr = FakeRequest().withJsonBody(Json.parse("""{"code":"code", "state":"state", "clientId":"clientId"}"""))
          val result = authController.accessToken.apply(fr)
          status(result) mustBe OK
          session(result).data("signed") mustBe "signed"
        }
      }
    }
  }
}