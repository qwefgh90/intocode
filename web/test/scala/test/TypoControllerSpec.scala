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
import io.github.qwefgh90.repogarden.web.model.TypoStatus._
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
import io.github.qwefgh90.repogarden.web.model.TypoRequest
import io.github.qwefgh90.repogarden.bp.Boilerplate._
import play.api.Application
import java.util.concurrent._

import org.scalatest.concurrent.PatienceConfiguration.{Timeout, Interval}
import org.scalatest.time.{Span, Seconds}
import org.scalatest.concurrent.{Eventually}
import org.scalatest.concurrent.Eventually._

class TypoControllerSpec extends PlaySpec with BeforeAndAfterAll {

  lazy val app =
    new GuiceApplicationBuilder()
    .configure("akka.remote.netty.tcp.port" -> 2551)
    .configure("akka.cluster.seed-nodes.0" -> "akka.tcp://ClusterSystem@127.0.0.1:2551")
	.in(Mode.Test)
	.build()
  
  override def beforeAll() {
    // start up your web server or whatever
  }

  override def afterAll() {
    app.stop() // shut down the web server
  }

  "TypoController" should {
    val homeController = app.injector.instanceOf[HomeController]
    val typoController = app.injector.instanceOf[TypoController]
    val encryption = app.injector.instanceOf[Encryption]
    val authService = app.injector.instanceOf[AuthService]
    val configuration = app.injector.instanceOf[Configuration]
    val context = app.injector.instanceOf[ExecutionContext]
    val cache = app.injector.instanceOf[AsyncCacheApi]
    val githubProvider = app.injector.instanceOf[GithubServiceProvider]
    val typoService = app.injector.instanceOf[TypoService]
    val typoDao = app.injector.instanceOf[TypoDao]

    val switchDao = app.injector.instanceOf[SwitchDao]
    typoDao.create()
    Await.result(switchDao.create(), Duration(10, TimeUnit.SECONDS))

    val oauthTokenOpt = configuration.getOptional[String]("oauthToken")
	require(oauthTokenOpt.isDefined)
    val oauthToken = oauthTokenOpt.get
    val githubService = githubProvider.getInstance(oauthToken)

    val owner = "repogarden"
    val name = "repogarden-test"
    val branchName = "master"
    val mockAuthService = new MockAuthService(accessToken = oauthToken)(context)
    val authController = new AuthController(mockAuthService, context, cache, githubProvider)
    val result = authController.accessToken.apply(FakeRequest().withJsonBody(Json.parse("""{"code":"code", "state":"state", "clientId":"clientId"}""")))
    status(result) mustBe OK
    session(result).data("signed") mustBe "signed"
    val user = session(result).data("user")
    val authFr = FakeRequest().withSession("signed" -> "signed", "user" -> user)

    def blockingToFindTypo(): Long = {
      val result = typoController.buildLastCommit(owner, name, branchName)(authFr)
      val js = contentAsJson(result)
      status(result) mustBe OK
      val id = (js \ "id").as[Long]
      Logger.debug("a registerd id: " + id)

      eventually(new Timeout(Span(40, Seconds)), new Interval(Span(1, Seconds))){
        val typoStatOpt = Await.result(typoDao.selectTypoStat(id), Duration(2, TimeUnit.SECONDS))
        typoStatOpt.isDefined mustBe true
        typoStatOpt.get.status mustBe FINISHED.toString
      }
      id
    }

    "return a commit and typo list after finding typos" in {
      val commitsResultBefore = typoController.getTypoStats(owner, name, branchName)(authFr)
      val arr = contentAsJson(commitsResultBefore).as[JsArray]

      status(commitsResultBefore) mustBe OK
      arr.value.length mustBe 0

      val id1 = blockingToFindTypo()
      val id2 = blockingToFindTypo()
      val commitsResult = typoController.getTypoStats(owner, name, branchName)(authFr)
      val commitsJson = contentAsJson(commitsResult)

      status(commitsResult) mustBe OK
      (commitsJson \\ "id").find(_.as[JsNumber].value == id1).isDefined mustBe true
      (commitsJson \\ "id").find(_.as[JsNumber].value == id2).isDefined mustBe true

      val typosResult = typoController.getTypos(owner, name, branchName, id1)(authFr)

      val typosJson = contentAsJson(typosResult)
      val typosListFromService = Await.result(typoDao.selectTypos(id1), Duration(10, TimeUnit.SECONDS))

      status(typosResult) mustBe OK
      typosJson.as[JsArray].value.length mustBe typosListFromService.length
    }
  }
}
