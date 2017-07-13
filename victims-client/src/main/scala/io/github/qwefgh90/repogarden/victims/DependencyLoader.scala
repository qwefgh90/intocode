package io.github.qwefgh90.repogarden.victims

import io.github.qwefgh90.repogarden.victims
import java.nio.file._
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.collection.CollectResult
import io.github.qwefgh90.repogarden.victims.util.Booter
import io.github.qwefgh90.repogarden.victims.util.ConsoleDependencyGraphDumper
import com.typesafe.scalalogging._


import org.eclipse.aether.graph.DependencyVisitor
import scala.collection.JavaConverters._
import org.apache.maven.model.building._
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
import scala.concurrent._
import scala.concurrent.blocking
import scala.io._
import java.io._


object MvnResult extends Enumeration {
  type MvnResult = Value
  val Success, Fail = Value
}

import MvnResult._

class DependencyLoader (mvnPath: Path, implicit val ec: ExecutionContext) {
  private val logger = Logger(classOf[DependencyLoader])
  def execute(pomFile: Path): (Stream[String], Future[MvnResult]) = {
    val builder = new ProcessBuilder(mvnPath.toAbsolutePath().toString(), "dependency:tree")
    builder.directory(pomFile.getParent.toFile)
    val process = builder.start()

    val br = new BufferedReader(new InputStreamReader(process.getInputStream()))
    val stream = Stream.continually({
      val line = br.readLine()
      if(line == null)
        br.close
      line
    }).takeWhile(str => str != null)



    (stream.toStream
      , Future{blocking(
        {
          if(process.waitFor() == 0) MvnResult.Success else MvnResult.Fail
        }
      )
    }
    )
  }

/*  def execute(pomPath: Path): Iterator[String] = {
    val source = Source.fromFile(pomPath.toFile)
    source.getLines
  }*/

/*    Future {
      blocking({
        ""
      })
    }*/


}



object DependencyLoader {

  def apply(mvnPath: Path, ec: ExecutionContext): DependencyLoader = {
    return new DependencyLoader(mvnPath, ec)
  }

  /**
    * work with DependencyVisitor
    */
/*  def getCollectResult(path: Path, visitor: DependencyVisitor): Boolean = {
    val system = Booter.newRepositorySystem();
    val session = Booter.newRepositorySystemSession( system );
    session.setConfigProperty( ConflictResolver.CONFIG_PROP_VERBOSE, true );
    session.setConfigProperty( DependencyManagerUtils.CONFIG_PROP_VERBOSE, true );

	val modelBuilder = new DefaultModelBuilderFactory().newInstance();

    val pomFile = path.toFile
    val modelRequest = new DefaultModelBuildingRequest()
    modelRequest.setPomFile(pomFile)
    val modelBuildingResult = modelBuilder.build(modelRequest)
    val mavenDependencies = modelBuildingResult.getEffectiveModel.getDependencies

    val dp = mavenDependencies.asScala.map(md => {
      val dependency = new org.eclipse.aether.graph.Dependency(new DefaultArtifact(md.getGroupId, md.getArtifactId, md.getClassifier, md.getType, md.getVersion), md.getScope)
      dependency
    })

    val collectRequest = new CollectRequest()
    collectRequest.setRootArtifact(new DefaultArtifact( "_:_:_" ))
    //    collectRequest.setDependencies( descriptorResult.getDependencies() )
    collectRequest.setDependencies(dp.asJava)
    //    collectRequest.setManagedDependencies(dp.asJava)
    collectRequest.setRepositories( Booter.newRepositories( system, session ) )
    val collectResult = system.collectDependencies( session, collectRequest )

    collectResult.getRoot().accept( visitor );
  }*/
}
