package io.github.qwefgh90.repogarden.bp.github

import scala.collection.JavaConverters._
import org.eclipse.egit.github.core._
import org.eclipse.egit.github.core.service._
import org.eclipse.egit.github.core.client._

object GithubContainer{
  def apply(client: GitHubClient){
    new GithubContainer(client)
  }
}

/*
 * A simple holder of service classes.
 */
class GithubContainer (client: GitHubClient){
  lazy val contentService = new ContentsService(client)
  lazy val repoService = new RepositoryService(client)
  lazy val dataService = new DataService(client)
  lazy val commitService = new DataService(client)
}
