package io.github.qwefgh90.repogarden.web.test

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import io.github.qwefgh90.repogarden.web.service.GithubService
import io.github.qwefgh90.repogarden.web.service.GithubServiceProvider
import io.github.qwefgh90.repogarden.web.model._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Mode
import play.api.Logger
import play.api.libs.json._
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import org.scalatest._

class GithubServiceSpec extends FlatSpec with Matchers with GuiceOneAppPerSuite {
  val oauthToken = System.getProperties.getProperty("oauthToken")
  val oauthTokenOpt = if(oauthToken == null)
	Option.empty
  else
	Option(oauthToken)
  
  require(oauthTokenOpt.isDefined)
  
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
	.in(Mode.Test)
	.build()



  val provider = app.injector.instanceOf[GithubServiceProvider]

  val githubService = provider.getInstance(oauthToken)

  "GithubService" should "get user information using access token" in {
    //    val githubService = new GithubService(oauthTokenOpt.get)
    Logger.debug(githubService.getUser.getId.toString)
    Logger.debug(githubService.getUser.getLogin)
    Logger.debug(githubService.getUser.getAvatarUrl)
    Logger.debug(githubService.getUser.getHtmlUrl)
    Logger.debug(githubService.getUser.getEmail)
    Logger.debug(githubService.getUser.getName)
    assert(githubService.getUser.getLogin.length > 0)
    assert(githubService.getUser.getAvatarUrl.length > 0)
    assert(githubService.getUser.getHtmlUrl.length > 0)
    assert(githubService.getUser.getEmail.length > 0)
    assert(githubService.getUser.getName.length > 0)
    Logger.debug(Json.toJson(githubService.getUser)(Implicits.userWritesToBrowser).toString)
    assert((Json.toJson(githubService.getUser)(Implicits.userWritesToBrowser) \ "id").isDefined)
  }
  
  "GithubService" should "get repositories using access token" in {
    //      val githubService = new GithubService(oauthTokenOpt.get)
    assert(githubService.getAllRepositories.length > 0)
  }
}
