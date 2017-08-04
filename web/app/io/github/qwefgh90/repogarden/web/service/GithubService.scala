package io.github.qwefgh90.repogarden.web.service

import play.api.cache._
import javax.inject._
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
import scala.concurrent.duration._
import java.util.concurrent._

@Singleton
class GithubServiceProvider @Inject() (@NamedCache("github-api-cache") cache: AsyncCacheApi) {
  def getInstance(accessToken: String): GithubService = {
    cache.sync.getOrElseUpdate(accessToken, Duration(60, TimeUnit.SECONDS)){
      new GithubService(accessToken, Some(cache.sync))
    }
  }
}

class GithubService (accessToken: String, cacheOpt: Option[SyncCacheApi] = Option.empty) {
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

  def getUser = {
    userService.getUser
  }
  
  object KeyType extends Enumeration{
    type KeyType = Value
    val getAllRepositories, getBranches, getCommits, getCommit, getRepository = Value
    def getKey(key: String, t: KeyType, params: List[String] = List()) =
      s"${key}:${t.toString}:${params.mkString(":")}"
  }

  def cached[A: scala.reflect.ClassTag](key: String, block: => A)(implicit invalidate: Boolean = false): A = {
    cacheOpt match {
      case Some(cache) if invalidate == false => cache.getOrElseUpdate(key, Duration(60, TimeUnit.SECONDS))(block)
      case Some(cache) if invalidate == true => block
      case None => block
    }
  }

  def getAllRepositories(implicit invalidate: Boolean = false): List[Repository] = {
    cached(KeyType.getKey(accessToken, KeyType.getAllRepositories) ,{
      repoService.getRepositories.asScala.toList
    })
  }

  def getRepository(owner: String, name: String)(implicit invalidate: Boolean = false): Repository = {
    cached(KeyType.getKey(accessToken, KeyType.getRepository, List(owner, name)) ,{
      repoService.getRepository(owner, name)
    })
  }

  def getBranches(repository: Repository)(implicit invalidate: Boolean = false) = {
    cached(KeyType.getKey("", KeyType.getBranches, List(repository.getOwner.getName, repository.getName)) ,{
      repoService.getBranches(repository).asScala
    })
  }

  def getCommits(repository: Repository)(implicit invalidate: Boolean = false) = {
    cached(KeyType.getKey("", KeyType.getCommits, List(repository.getOwner.getName, repository.getName)) ,{
      commitService.getCommits(repository)
    })
  }

  def getCommit(repository: Repository, sha: String)(implicit invalidate: Boolean = false): Option[RepositoryCommit] = {
    cached(KeyType.getKey("", KeyType.getCommit, List(repository.getOwner.getName, repository.getName, sha)) ,{
      val commit = commitService.getCommit(repository, sha)
      if(commit.getCommit == null)
        Option.empty
      else
        Option(commit)
    })
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
