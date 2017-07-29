package io.github.qwefgh90.repogarden.web.service

import io.github.qwefgh90.repogarden.bp.github.Implicits._
import javax.inject._
import org.eclipse.egit.github.core._
import org.eclipse.egit.github.core.service._
import org.eclipse.egit.github.core.client.GitHubClient
import scala.collection.JavaConverters._
import io.github.qwefgh90.repogarden.web.model._

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
  private lazy val commitService = {
		new CommitService(client);
  }
  private lazy val user = {
    userService.getUser
  }

  def getAllRepositories: List[Repository] = {
    repoService.getRepositories.asScala.toList
  }

  def getUser = {
    user
  }
  
  def repository(owner: String, name: String): Repository = {
    repoService.getRepository(owner, name)
  }

  def getBranches(repository: Repository) = {
    repoService.getBranches(repository).asScala
  }

  def getCommits(repository: Repository) = {
    commitService.getCommits(repository)
  }

  def getCommit(repository: Repository, sha: String) = {
    commitService.getCommit(repository, sha)
  }
}
