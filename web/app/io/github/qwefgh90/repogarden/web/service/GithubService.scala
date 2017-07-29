package io.github.qwefgh90.repogarden.web.service

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.github.qwefgh90.repogarden.bp.github.Implicits._
import javax.inject._
import org.eclipse.egit.github.core._
import org.eclipse.egit.github.core.service._
import org.eclipse.egit.github.core.client.GitHubClient
import scala.collection.JavaConverters._
import io.github.qwefgh90.repogarden.web.model._
import io.github.qwefgh90.repogarden.web.model.Implicits._


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

  def getCommit(repository: Repository, sha: String): Option[RepositoryCommit] = {
    val commit = commitService.getCommit(repository, sha)
    if(commit.getCommit == null)
      Option.empty
    else
      Option(commit)
  }

  def getAllRepositoriesJson() = {
    val repositories = this.getAllRepositories
    val repositoriesJs = repositories.map(repository => {
      val branches = this.getBranches(repository)
      val branchesWithCommit = branches.map(branch => {
        val commit = getCommit(repository, branch.getCommit.getSha)
        val branchJson = Json.toJson(branch).asInstanceOf[JsObject]
        if(commit.isDefined){
          Json.obj("commits" -> List(commit)) ++ branchJson
        }else
          branchJson
      })

      val branchesJs = Json.toJson(branchesWithCommit)
      Json.toJson(repository).asInstanceOf[JsObject] ++ Json.obj("branches" -> branchesJs)
    })
    Json.toJson(repositoriesJs)
  }
}
