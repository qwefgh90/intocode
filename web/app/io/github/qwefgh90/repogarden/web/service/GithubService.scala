package io.github.qwefgh90.repogarden.web.service

import io.github.qwefgh90.repogarden.bp.github.Implicits._
import javax.inject._
import org.eclipse.egit.github.core._
import org.eclipse.egit.github.core.service._
import org.eclipse.egit.github.core.client.GitHubClient
import scala.collection.JavaConverters._

class GithubService (accessToken: String) {
  private lazy val client = {
    val client = new GitHubClient()
    client.setOAuth2Token(accessToken)
  }
  private lazy val contentsService = {
		new ContentsService(client)
  }
  private lazy val repoService = {
		new RepositoryService(client)
  }
  private lazy val userService = {
		new UserService(client);
  }
  private lazy val user = {
    userService.getUser
  }
  def getAllRepositories = {
    repoService.getRepositories.asScala
  }
  def getUser = {
    user
  }
  
}