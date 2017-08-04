package io.github.qwefgh90.repogarden.web.test

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import io.github.qwefgh90.repogarden.web.service.GithubService
import io.github.qwefgh90.repogarden.web.model._
import play.api.Logger
import play.api.libs.json._

class GithubServiceSpec extends FlatSpec with Matchers {
	val oauthToken = System.getProperties.getProperty("oauthToken")
	val oauthTokenOpt = if(oauthToken == null)
	  Option.empty
	else
	  Option(oauthToken)
	
	require(oauthTokenOpt.isDefined)
	
  "GithubService" should "get user information using access token" in {
    val githubService = new GithubService(oauthTokenOpt.get)
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
      val githubService = new GithubService(oauthTokenOpt.get)
      githubService.getAllRepositories.foreach(repo => {
         Logger.debug(repo.getName) 
         Logger.debug(repo.getHtmlUrl) 
         assert(repo.getName.length > 0)  
         assert(repo.getGitUrl.length > 0)  
      })
  }
}
