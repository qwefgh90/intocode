package io.github.qwefgh90.repogarden.web.model

import io.github.qwefgh90.repogarden.web.model.TypoStatus._
import io.github.qwefgh90.repogarden.bp.github.Implicits._
import io.github.qwefgh90.repogarden.web.service.GithubService

case class TypoStat(id: Option[Long], ownerId: Long, repositoryId: Long, branchName: String, commitSha: String, startTime: Option[Long], completeTime: Option[Long], message: String, state: String, userId: Long)

case class Typo(parentId: Long, path: String, treeSha: String, issueCount: Int, components: String, highlight: String)

case class TypoComponent(parentId: Long, path: String, from: Int, to: Int, endLine: Int, column: Int, suggestedList: List[String])


object TypoRequest extends ((Long, Long, String, String, Long, TreeEx) => TypoRequest) {
  //replace the toString implementation coming from the inherited class (FunctionN)
  override def toString =
    getClass.getName.split("""\$""").reverse.dropWhile(x => {val char = x.take(1).head; !((char == '_') || char.isLetter)}).head
  def createLastRequest(githubService: GithubService, owner: String, repoName: String, branchName: String): TypoRequest = {
    val repoOpt = githubService.getRepository(owner, repoName)
assert(repoOpt.isDefined)
    val repo = repoOpt.get
    val ownerId: Long = repo.getOwner.getId
    val repositoryId = repo.getId
    val commit = githubService.getLastestCommitByBranchName(repo, branchName).get
    val userId: Long = githubService.getUser.getId
    val treeEx = githubService.getTree(repo, commit.getCommit.getTree.getSha)(true).get
    TypoRequest(ownerId, repositoryId, branchName, commit.getSha, userId, treeEx)

  }
}

case class TypoRequest(ownerId: Long, repositoryId: Long, branchName: String, commitSha: String, userId: Long, treeEx: TreeEx)
