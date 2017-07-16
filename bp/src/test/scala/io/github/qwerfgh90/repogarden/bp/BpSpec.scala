package io.github.qwerfgh90.repogarden.bp

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import com.typesafe.scalalogging.Logger
import io.github.qwefgh90.repogarden.bp.github.Implicits._
import scala.collection.JavaConverters._

import org.eclipse.egit.github.core.client._
import org.eclipse.egit.github.core.service._

class BpSpec extends FlatSpec with Matchers{
  private val logger = Logger(classOf[BpSpec])
  
  private lazy val client = {
	val oauthToken = System.getProperties.getProperty("oauthToken")
	val oauthTokenOpt = if(oauthToken == null)
	  Option.empty
	else
	  Option(oauthToken)
	
	val githubClient = new GitHubClient()
	if(oauthTokenOpt.isDefined){
	  githubClient.setOAuth2Token(oauthTokenOpt.get)
	}
	githubClient
  }
  

  "Github api" should "get tree structure" in {
	val contentService = new ContentsService(client)
	val repositoryService = new RepositoryService(client)
	val commitService = new CommitService(client)
    val repository = repositoryService.getRepository("victims", "victims-cve-db")
	val root = contentService.getContentsTree(repository, "database/java", "heads/master")

    def print(node: Node){
      if(node.isInstanceOf[TreeNode]){
        logger.debug(node.asInstanceOf[TreeNode].get.getName)
        node.asInstanceOf[TreeNode].children.foreach(child =>{
          print(child)
        })
      }else{
        logger.debug(node.asInstanceOf[TerminalNode].get.getName)
      }
    }
    root.children.foreach(node => {
      print(node)
    })
    assert(root.children.length >= 10, "A qcount of tree must be more than 9")
  }

  "Github api" should "get trees of commits" in {
	val contentService = new ContentsService(client)
	val repositoryService = new RepositoryService(client)
	val commitService = new CommitService(client)
    val repository = repositoryService.getRepository("qwefgh90", "repogarden-web")
	val initialList = contentService.getContents(repository, "", "ed6a96f07855cd4ec3317dde8721a6f981c9d24a", true)
	assert(initialList.length == 1)
    val otherRepository = repositoryService.getRepository("reactivemanifesto", "reactivemanifesto")
	val list2 = contentService.getContents(otherRepository, "", "8f758d46ae201886098b80dc0eab0c484305cd8e", true, true)
	assert(list2.length == 27)
	list2.foreach(content => { logger.debug(s"${content.getType} : ${content.getPath} ") })
  }
  
  "A version string" should "be correctly compared with other version string" in {
    val version0 = "1"
    val targetVersion0 = "2"
    assert(version0.toVersion < targetVersion0.toVersion)
    assert(version0.toVersion <= targetVersion0.toVersion)
    val version1 = "1.1.1.2"
    val targetVersion1 = "1.1.1.3"
    assert(version1.toVersion < targetVersion1.toVersion)
    assert(version1.toVersion <= targetVersion1.toVersion)
    val version2 = "1.1.1.2"
    val targetVersion2 = "1.1.2.3"
    assert(version2.toVersion < targetVersion2.toVersion)
    assert(version2.toVersion <= targetVersion2.toVersion)
    val version3 = "1.1.4.5"
    val targetVersion3 = "1.1.4.4"
    assert(version3.toVersion > targetVersion3.toVersion)
    assert(version3.toVersion >= targetVersion3.toVersion)
    val version4 = "1.1.4.5"
    val targetVersion4 = "1.1.4.5"
    assert(version4.toVersion == targetVersion4.toVersion)
    val version5 = "9.0.0.M7"
    val targetVersion5 = "9.0.0.M20"
    assert(version5.toVersion <= targetVersion5.toVersion)
    val version6 = "9.0"
    val targetVersion6 = "9.0.1"
    assert(version6.toVersion <= targetVersion6.toVersion)
  }
}
