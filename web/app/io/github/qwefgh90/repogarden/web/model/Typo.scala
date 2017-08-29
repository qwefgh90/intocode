package io.github.qwefgh90.repogarden.web.model

//import io.github.qwefgh90.repogarden.web.model.TypoStatus._
import io.github.qwefgh90.repogarden.bp.github.Implicits._
import io.github.qwefgh90.repogarden.web.service.GithubService

case class TypoStat(id: Option[Long], ownerId: Long, repositoryId: Long, branchName: String, commitSha: String, startTime: Option[Long], completeTime: Option[Long], message: String, status: String, userId: Long)

case class Typo(id: Option[Long], parentId: Long, path: String, treeSha: String, issueCount: Int, highlight: String)

case class TypoComponent(id: Option[Long], parentId: Option[Long], path: String, from: Int, to: Int, endLine: Int, columnNum: Int, suggestedList: String, disabled: Boolean = false)


object TypoRequest extends ((Long, Long, String, String, Long, TreeEx) => TypoRequest) {
  //replace the toString implementation coming from the inherited class (FunctionN)
  override def toString =
    getClass.getName.split("""\$""").reverse.dropWhile(x => {val char = x.take(1).head; !((char == '_') || char.isLetter)}).head
  def createLastRequest(githubService: GithubService, owner: String, repoName: String, branchName: String): Option[TypoRequest] = {
    val repoOpt = githubService.getRepository(owner, repoName)
    repoOpt.flatMap{repo =>
      val ownerId: Long = repo.getOwner.getId
      val repositoryId = repo.getId
      val commitOpt = githubService.getLastestCommitByBranchName(repo, branchName)
      commitOpt.map{commit => 
        val userId: Long = githubService.getUser.getId
        val treeEx = githubService.getTree(repo, commit.getCommit.getTree.getSha)(true).get
        TypoRequest(ownerId, repositoryId, branchName, commit.getSha, userId, treeEx)
      }
    }
  }
}

case class TypoRequest(ownerId: Long, repositoryId: Long, branchName: String, commitSha: String, userId: Long, treeEx: TreeEx)

object TypoStatus extends Enumeration{
  type TypoStatus = Value
  val PROGRESS, FINISHED, FAILED = Value
}
