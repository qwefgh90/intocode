package io.github.qwefgh90.repogarden.web.service

import org.eclipse.aether.artifact.Artifact
import javax.inject._
import play.api._
import play.api.cache._
import play.api.mvc._
import io.github.qwefgh90.repogarden.victims.{VictimsLoader, VulnerableResult}
import io.github.qwefgh90.repogarden.victims.VictimsLoader._
import io.github.qwefgh90.repogarden.bp.github.Implicits._
import io.github.qwefgh90.repogarden.web.model._
import java.nio.file.{Paths, Files, Path}
import io.github.qwefgh90.repogarden.victims.maven.deps.DependencyLoader
import io.github.qwefgh90.repogarden.victims.maven.deps.{MavenResult, MvnResult, DependencyTree}
import scala.concurrent.{ExecutionContext, Future}

class CveService @Inject() (provider: GithubServiceProvider, configuration: Configuration, @NamedCache("object-cache") cache: SyncCacheApi){
  private val serverToken = configuration.getOptional[String]("oauthToken")
  private def getVictimsLoader(): VictimsLoader = {
    cache.getOrElseUpdate("victim-loader")(VictimsLoader(serverToken))
  }

  val mvnPath = Paths.get(configuration.get[String]("maven.path"))
  assert(Files.exists(mvnPath), "Maven does not exists.")

  val depLoader = DependencyLoader(mvnPath, ExecutionContext.global)

  def scanRootPomFile(tree: TreeEx): Future[Option[CveResult]] = {
    val victims = getVictimsLoader()
    val filteredTree = tree.filterBlob(e => e.name == "pom.xml")
    val rootPomOpt = filteredTree.list.find{e => e.level == 0 && e.name == "pom.xml"}
    if(rootPomOpt.isDefined){
      val dir = Files.createTempDirectory(s"temp_${System.currentTimeMillis.toString}")
      filteredTree.writeToFileSystem(dir)
      val exResult = depLoader.execute(dir.resolve("pom.xml"))
//    exResult.log.subscribe(s => logger.debug("execution logs: " + s))
      exResult.result.map{mvnResult =>
        val MavenResult(value, path) = mvnResult
        if(value == MvnResult.Success){
          val depTree = DependencyTree(path.get)
          val vulList = depTree.levelLines.collect(
            {case line if victims.scanSingleArtifact(line.artifact.getGroupId, line.artifact.getArtifactId, line.artifact.getVersion).isDefined => Cve(line.artifact, victims.scanSingleArtifact(line.artifact.getGroupId, line.artifact.getArtifactId, line.artifact.getVersion).get)
            }
          ).toList
          Option(CveResult(rootPomOpt.get, vulList))
        }
        None
      }(ExecutionContext.global)
    }else
       Future{
         None
       }(ExecutionContext.global)
  }
}
