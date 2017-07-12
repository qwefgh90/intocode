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


object DependencyLoader {

  /**
    * work with DependencyVisitor
    */
  def getCollectResult(path: Path, visitor: DependencyVisitor): Boolean = {
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
  }
}
