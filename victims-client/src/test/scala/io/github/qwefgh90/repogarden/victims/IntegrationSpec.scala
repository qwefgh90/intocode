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

import com.typesafe.scalalogging._

import io.github.qwefgh90.repogarden.victims.model.JavaModule
import io.github.qwefgh90.repogarden.victims.model.Victim
import io.github.qwefgh90.repogarden.victims.maven.deps._
import io.github.qwefgh90.repogarden.victims.maven._
import java.io.File
import java.util.Arrays
import com.typesafe.config.ConfigFactory

class IntegrationSpec extends FlatSpec with Matchers{
  private val logger = Logger(classOf[IntegrationSpec])
  val systemToken = System.getProperties.getProperty("oauthToken")
  val systemTokenOpt = if(systemToken == null)
	Option.empty
  else
	Option(systemToken)
  require(systemTokenOpt.isDefined)

  import com.typesafe.config.ConfigFactory
  import scala.concurrent.duration._
  import scala.concurrent._
  import java.util.concurrent._

  "A victims loader" should "find vulnerables from pom.xml" in {
    val conf = ConfigFactory.load("victims_client.conf")
    val mvnPath = Paths.get(conf.getString("maven.path"))
    require(Files.exists(mvnPath), s"${mvnPath.toAbsolutePath.toString} does not exists.")

    val loader = DependencyLoader(mvnPath, ExecutionContext.global)
    val victim = VictimsLoader(systemTokenOpt)
    val exResult = loader.execute(Paths.get(getClass.getResource("/vulnerable_pom/pom.xml").toURI()))
    exResult.log.subscribe(str => logger.debug(str))

    val MavenResult(value, path) = Await.result(exResult.result, Duration(60, TimeUnit.SECONDS))

    assert(value == MvnResult.Success)
    val outputPath = path.get

    val source = Source.fromFile(outputPath.toFile)
    val lines = loader.createLevelLineFromMvnOutput(source.getLines)
    val vulLines = lines.collect(
      {case line if victim.scanSingleArtifact(line.artifact.getGroupId, line.artifact.getArtifactId, line.artifact.getVersion).isDefined => line
      }
    ).toList
    vulLines.foreach(l => logger.debug(s"vulerable artifact: ${l.toString}"))
    assert(vulLines.length >= 2, "It should find artifacts equal to or more than 2")
    source.close()
  }
}
