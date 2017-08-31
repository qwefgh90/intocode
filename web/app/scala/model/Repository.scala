package io.github.qwefgh90.repogarden.web.model

import org.eclipse.egit.github.core._
import org.eclipse.egit.github.core.service._
import scala.collection.JavaConverters._

trait RepositoryExtension {
//  implicit def repositoryConverter(repository: Repository) = new RepositoryEx(repository)
//  implicit def branchConverter(branch: RepositoryBranch) = new BranchEx(branch)
/*  implicit def commitConverter(commit: Commit) = new CommitEx(commit)

  class RepositoryEx(base: Repository) extends Repository {
    def getBranches(repositoryService: RepositoryService) = {
      repositoryService.getBranches(repository).asScala
    }
  }

  class BranchEx(branch: RepositoryBranch){
    def getCommits(commitService: CommitService, repository: Repository) = {
      commitService.getCommits(repository)
    }
    def getCommit(commitService: CommitService, repository: Repository, sha: String) = {
      commitService.getCommit(repository, sha)
    }
  }

  class CommitEx(commit: Commit){
    def getTree(){

    }
  }*/
}
