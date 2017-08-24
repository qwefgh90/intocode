package io.github.qwefgh90.repogarden.web.service

import play.api.cache._
import play.api._
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
import io.github.qwefgh90.repogarden.web.dao._
import scala.util.Try

@Singleton
class GithubServiceProvider @Inject() (@NamedCache("github-api-cache") cache: AsyncCacheApi, switchDao: SwitchDao) {
  def getInstance(accessToken: String): GithubService = {
    cache.sync.getOrElseUpdate(accessToken, Duration(60, TimeUnit.SECONDS)){
      new GithubService(accessToken, switchDao, Some(cache.sync))
    }
  }
}

class GithubService (accessToken: String, switchDao: SwitchDao, cacheOpt: Option[SyncCacheApi] = Option.empty) {
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
  private lazy val dataService = {
	new DataService(client);
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
      val repositoriesJava = repoService.getRepositories
      if(repositoriesJava == null)
        List()
      else
        repositoriesJava.asScala.toList
    })
  }

  def getRepository(owner: String, name: String)(implicit invalidate: Boolean = false): Option[Repository] = {
    val repo = cached(KeyType.getKey(accessToken, KeyType.getRepository, List(owner, name)) ,{
      repoService.getRepository(owner, name)
    })
    if(repo == null) None else Some(repo)
  }


  def getBranchesByName(owner: String, name: String)(implicit invalidate: Boolean = false): List[RepositoryBranch] = {
    val repositoryOpt = getRepository(owner,name)
    repositoryOpt.map( repository =>
      cached(KeyType.getKey("", KeyType.getBranches, List(repository.getOwner.getName, repository.getName)) ,{
        val branchesJava = repoService.getBranches(repository)
        if(branchesJava == null)
          List()
        else
          branchesJava.asScala.toList
      })).getOrElse(List())
  }

  def getBranches(repository: Repository)(implicit invalidate: Boolean = false) = {
    cached(KeyType.getKey("", KeyType.getBranches, List(repository.getOwner.getName, repository.getName)) ,{
      val branchesJava = repoService.getBranches(repository)
      if(branchesJava == null)
        List()
      else
        branchesJava.asScala.toList
    })
  }

  def getBranchByBranchName(repository: Repository, branchName: String)(implicit invalidate: Boolean = false) = {
    getBranches(repository).find(b => b.getName == branchName)
  }

  def getLastestCommitByBranchName(repository: Repository, branchName: String)(implicit invalidate: Boolean = false) = {
    val branchOpt = getBranchByBranchName(repository, branchName)
    branchOpt.flatMap(b => {
      getCommit(repository, b.getCommit.getSha)
    })
  }

  def getCommits(repository: Repository)(implicit invalidate: Boolean = false) = {
    cached(KeyType.getKey("", KeyType.getCommits, List(repository.getOwner.getName, repository.getName)) ,{
      val commitsJava = commitService.getCommits(repository)
      if(commitsJava == null)
        List()
      else
        commitsJava.asScala.toList
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

  def getTree(repository: Repository, sha: String)(implicit sync: Boolean = false): Option[TreeEx] = {
    val tree = this.dataService.getTree(repository, sha, true)
    val treeOpt = if(tree == null) None else Some(tree)
    treeOpt.map(tree => {
      val initTree = TreeEx(tree)
      if(sync){
        initTree.syncContents(repository, this.dataService)
      }
      initTree
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
