package io.github.qwerfgh90.repogarden.bp

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import com.typesafe.scalalogging.Logger
import io.github.qwefgh90.repogarden.bp.github.Implicits._
import scala.collection.JavaConverters._
import java.nio.file._
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

  "Egit" should "return a list of branches" in {
	val contentService = new ContentsService(client)
	val repositoryService = new RepositoryService(client)
	val commitService = new CommitService(client)
    val dataService = new DataService(client)
    val repository = repositoryService.getRepository("apache", "spark")
    val list = repositoryService.getBranches(repository).asScala
    val sha = list.find(_.getName=="master").get.getCommit.getSha
    list.foreach(b => {
      logger.debug("-")
      logger.debug(b.getName)
      logger.debug(b.getCommit.getUrl)
      logger.debug(b.getCommit.getType)
      logger.debug(b.getCommit.getSha)
    })
    
    val tree = dataService.getTree(repository, sha, true)
    val t = GitTree(tree.getTree.asScala.take(50).toList)
    t.traverse(new Visitor[TreeEntryEx, Unit]{
      override var acc: Unit = Unit
      override def enter(e: TreeEntryEx, stack: List[TreeEntryEx]){
        logger.debug(s"path: ${e.entry.getPath.toString}")
      }
      override def leave(e: TreeEntryEx){
        
      }
    })
  }

  "Github util" should "create directory structure to which a filter is applied  " in {
	val contentService = new ContentsService(client)
	val repositoryService = new RepositoryService(client)
	val commitService = new CommitService(client)
    val dataService = new DataService(client)
    val repository = repositoryService.getRepository("victims", "victims-cve-db")
    val list = repositoryService.getBranches(repository).asScala
    val sha = list.find(_.getName=="master").get.getCommit.getSha
    val rawTree = dataService.getTree(repository, sha, true)
	val tree = GitTree(rawTree).shorten(Array("database","java"))
    import org.eclipse.egit.github.core._
    tree.syncContents(repository, dataService)
    val tempPath = Files.createTempDirectory(s"test_${System.currentTimeMillis().toString}")
    tree.writeToFileSystem(tempPath, e => {
      if(e.name == "2017" || (e.entry.getType == TreeEntry.TYPE_BLOB))
        true
      else
        false
    })
    logger.debug(s"A filtered structure in '${tempPath}' is created from a remote")
    assert(Files.list(tempPath).count == 1)
  }
 
  "Github util" should "create temp directory structure" in {
	val contentService = new ContentsService(client)
	val repositoryService = new RepositoryService(client)
	val commitService = new CommitService(client)
    val dataService = new DataService(client)
    val repository = repositoryService.getRepository("victims", "victims-cve-db")
    val list = repositoryService.getBranches(repository).asScala
    val sha = list.find(_.getName=="master").get.getCommit.getSha
    val rawTree = dataService.getTree(repository, sha, true)
	val tree = GitTree(rawTree).shorten(Array("database","java"))
    tree.syncContents(repository, dataService)
    val tempPath = Files.createTempDirectory(s"test_${System.currentTimeMillis().toString}")
    tree.writeToFileSystem(tempPath)
    logger.debug(s"File structure in '${tempPath}' is created from a remote")
    assert(Files.list(tempPath).count >= 10)
  }
  
  "Github util" should "get tree structure" in {
	val contentService = new ContentsService(client)
	val repositoryService = new RepositoryService(client)
	val commitService = new CommitService(client)
    val dataService = new DataService(client)
    val repository = repositoryService.getRepository("victims", "victims-cve-db")
    val list = repositoryService.getBranches(repository).asScala
    val sha = list.find(_.getName=="master").get.getCommit.getSha
    val rawTree = dataService.getTree(repository, sha, true)
	val tree = GitTree(rawTree).shorten(Array("database","java"))
    import org.eclipse.egit.github.core._
    tree.traverse(new Visitor[TreeEntryEx, Unit]{
      override var acc: Unit = Unit
      override def enter(e: TreeEntryEx, stack: List[TreeEntryEx]) = e.entry.getType match {
        case TreeEntry.TYPE_TREE => println(s"tree: ${e.level}, ${stack.reverse.map(_.name).mkString("/")}, ${e.name} ")
        case TreeEntry.TYPE_BLOB => println(s"blob: ${e.level}, ${stack.reverse.map(_.name).mkString("/")}, ${e.name} ")
      }
      override def leave(e: TreeEntryEx){
      }
    })
    assert(tree.list.count( _.entry.getType == TYPE_TREE ) >= 10, "A count of nodes must be more than 9")
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
