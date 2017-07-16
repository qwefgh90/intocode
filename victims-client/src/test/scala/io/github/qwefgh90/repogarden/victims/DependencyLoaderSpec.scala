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
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.io.File
import java.util.Arrays

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import io.github.qwefgh90.repogarden.victims.util.Booter;
import io.github.qwefgh90.repogarden.victims.util.ConsoleDependencyGraphDumper;

import org.eclipse.aether.graph._
import org.eclipse.aether.util.artifact.SubArtifact
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.collection.CollectResult
import io.github.qwefgh90.repogarden.victims.util.Booter
import io.github.qwefgh90.repogarden.victims.util.ConsoleDependencyGraphDumper
import org.eclipse.aether.resolution.ArtifactDescriptorRequest
import org.eclipse.aether.resolution.ArtifactDescriptorResult
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils
import org.eclipse.aether.util.graph.transformer.ConflictResolver
import com.typesafe.config.ConfigFactory
import scala.concurrent.Future
import scala.concurrent.forkjoin._
import scala.concurrent.ExecutionContext
import io.github.qwefgh90.repogarden.victims.maven.deps._
import scala.concurrent.duration._

class DependencyLoaderSpec extends FlatSpec with Matchers{
  private val logger = Logger(classOf[DependencyLoaderSpec])

  "DependencyLoader" should "find parents of a artifact" in {
    val conf = ConfigFactory.load("victims_client.conf")
    val mvnPath = Paths.get(conf.getString("maven.path"))
    require(Files.exists(mvnPath), s"${mvnPath.toAbsolutePath.toString} does not exists.")

    val dl = DependencyLoader(mvnPath, ExecutionContext.global)
    val ExecutionResult(ob1, result1) = dl.execute(Paths.get(getClass().getResource("/valid_pom/pom.xml").toURI))
    
    val mvnResult = Await.result(result1, Duration(60, SECONDS))

    val source2 = DependencyTree(mvnResult.outputPath.get)
    val parentList2 = source2.findParents( levelLine => {
      levelLine.line.contains("org.codehaus.woodstox:stax2-api:jar:3.1.4:compile")
    })
    
    parentList2.foreach(found => {
      logger.debug(s"found: ${found.line}")
      found.parents.foreach({ parent =>
        logger.debug(s"parent: ${parent.line}")
      })
    })

    assert(parentList2(0).parents.map(line => line.line).sameElements(
      List(
        "org.codehaus.woodstox:woodstox-core-asl:jar:4.4.1:compile"
          ,"org.apache.cxf:cxf-core:jar:3.0.3:compile"
          ,"org.apache.cxf:cxf-rt-rs-client:jar:3.0.3:compile"
          ,"org.apache.tika:tika-parsers:jar:1.14:compile"
          ,"io.github.qwefgh90:jsearch:jar:0.2.0")), "Two list must be same.")

    assert(source2.findParents(levelLine => {
      levelLine.line.contains("io.github.qwefgh90:jsearch:jar:0.2.0")
    })(0).parents.length == 0, "A size of parent of root must be zero.")

    assert(source2.findParents(levelLine => {
      levelLine.line.contains("always return false")
    }).length == 0, "When it can't find levelLine to meet condition, it must return zero.")
  }

  "DependencyLoader" should "process valid or invalid pom.xml" in {
    val conf = ConfigFactory.load("victims_client.conf")
    val mvnPath = Paths.get(conf.getString("maven.path"))
    require(Files.exists(mvnPath), s"${mvnPath.toAbsolutePath.toString} does not exists.")

    val dl = DependencyLoader(mvnPath, ExecutionContext.global)
    val ExecutionResult(ob1, result1) = dl.execute(Paths.get(getClass().getResource("/valid_pom/pom.xml").toURI))
    ob1.foreach((a)=>{logger.debug(a)})

    assert(Await.result(result1, Duration(60, SECONDS)).resultCode.equals(MvnResult.Success))
    assert(Files.exists(Await.result(result1, Duration(60, SECONDS)).outputPath.get))

    val ExecutionResult(ob2, result2) = dl.execute(Paths.get(getClass().getResource("/invalid_pom/pom.xml").toURI))
    ob2.foreach((a)=>{logger.debug(a)})

    assert(Await.result(result2, Duration(60, SECONDS)).resultCode.equals(MvnResult.Fail))
    assert(Await.result(result2, Duration(60, SECONDS)).outputPath.isEmpty)
    
    val outputPath = Await.result(result1, Duration(60, SECONDS)).outputPath.get
    val tree = DependencyTree(outputPath)

    val msgOccurrenceCount = tree.traverseLevelLines(new Visitor[LevelLine,Int]{
      override var acc = 0
      override def enter(levelLine: LevelLine, stack: List[LevelLine]){
        logger.debug(s"enter: ${levelLine.toString}")
        acc = acc + 1
      }
      override def leave(levelLine: LevelLine){
        acc = acc + 1
        logger.debug(s"leave: ${levelLine.toString}")
      }
    })
    assert(msgOccurrenceCount == 204)
  }
}
