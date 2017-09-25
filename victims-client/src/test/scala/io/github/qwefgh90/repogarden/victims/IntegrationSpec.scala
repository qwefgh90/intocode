package io.github.qwefgh90.repogarden.victims

import scala.io._
import scala.collection.JavaConverters._
import org.apache.maven.model.building._
import java.io.File
import java.nio.file._

import org.scalactic._
import org.scalatest._
import org.yaml.snakeyaml._
import org.yaml.snakeyaml.constructor.Constructor
import io.github.qwefgh90.repogarden.bp.github.Implicits._

import org.eclipse.egit.github.core.Blob
import org.eclipse.egit.github.core.RepositoryCommit
import org.eclipse.egit.github.core.RepositoryContents
import org.eclipse.egit.github.core.TreeEntry
import org.eclipse.egit.github.core.client._
import org.eclipse.egit.github.core.service._
import com.typesafe.scalalogging._

import io.github.qwefgh90.repogarden.victims.model.JavaModule
import io.github.qwefgh90.repogarden.victims.model.Victim
import io.github.qwefgh90.repogarden.victims.maven.deps._
import io.github.qwefgh90.repogarden.victims.maven._
import java.io.File
import java.util.Arrays
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent._
import java.util.concurrent._

class IntegrationSpec extends FlatSpec with Matchers{
  private val logger = Logger(classOf[IntegrationSpec])
  val systemToken = System.getProperties.getProperty("oauthToken")
  val systemTokenOpt = if(systemToken == null)
	Option.empty
  else
	Option(systemToken)
  require(systemTokenOpt.isDefined)

  val conf = ConfigFactory.load("victims_client.conf")
  val mvnPath = Paths.get(conf.getString("maven.path"))
  val loader = DependencyLoader(mvnPath, ExecutionContext.global)
  val victim = VictimsLoader(systemTokenOpt)
  require(Files.exists(mvnPath), s"${mvnPath.toAbsolutePath.toString} does not exists.")

  "A dependency loader" should "load tree from remote pom.xml" in {
	val githubClient = new GitHubClient()
	githubClient.setOAuth2Token(systemTokenOpt.get)
    val contentService = new ContentsService(githubClient)
    val repoService = new RepositoryService(githubClient)
    val dataService = new DataService(githubClient)
    val repository = repoService.getRepository("apache", "spark")
    val list = repoService.getBranches(repository).asScala
    val sha = list.find(_.getName=="master").get.getCommit.getSha
    val repo = repoService.getRepository("apache", "spark")
    val rawTree = dataService.getTree(repository, sha, true)
	val tree = TreeEx(rawTree).filterBlob(e => e.entry.getType == TYPE_TREE || e.name == "pom.xml")
    tree.syncContents(repo, dataService)
    val dir = Files.createTempDirectory(s"temp_${System.currentTimeMillis.toString}")
    tree.writeToFileSystem(dir)
    logger.debug(s"spark temp dir: ${dir.toAbsolutePath().toString}")
    tree.foreachEnter((e, list) => { // print pom.xml
      if(e.name == "pom.xml")
        logger.debug(list.map(e => e.name).reverse.mkString("/") + "/" + e.name)
    })

    assert(tree.list.filter(e => e.name == "pom.xml").length >= 37, "Apache Spark has pom files more than 37.")

    val exResult = loader.execute(dir.resolve("pom.xml"))
    exResult.log.subscribe(s => logger.debug("execution logs: " + s))
    val MavenResult(value, path) = Await.result(exResult.result, Duration(90, TimeUnit.SECONDS))
    assert(value == MvnResult.Success)
  }

  "A victims loader" should "find vulnerables from local pom.xml" in {
    val exResult = loader.execute(Paths.get(getClass.getResource("/vulnerable_pom/pom.xml").toURI()))
    logger.debug("wait before!")

    val MavenResult(value, path) = Await.result(exResult.result, Duration(60, TimeUnit.SECONDS))
    assert(value == MvnResult.Success)
    val outputPath = path.get

    val tree = DependencyTree(outputPath)

    val vulLines = tree.levelLines.collect(
      {case line if victim.scanSingleArtifact(line.artifact.getGroupId, line.artifact.getArtifactId, line.artifact.getVersion).isDefined => line
      }
    ).toList    
    
    logger.debug("Finished to collect cves.")

    vulLines.foreach(l => logger.debug(s"vulerable artifact: ${l.toString}"))
    assert(vulLines.length >= 2, "It should find artifacts equal to or more than 2")
  }
}
