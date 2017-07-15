package io.github.qwefgh90.repogarden.victims.maven.remote

import io.github.qwefgh90.repogarden.victims._
import io.github.qwefgh90.repogarden.victims.util._
import io.github.qwefgh90.repogarden.victims.model._

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph._
import org.eclipse.aether.util.artifact.SubArtifact
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.collection.CollectResult
import org.eclipse.aether.resolution.ArtifactDescriptorRequest
import org.eclipse.aether.resolution.ArtifactDescriptorResult
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils
import org.eclipse.aether.util.graph.transformer.ConflictResolver
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;

class Remote {
  def checkUpdate(groupId: String, artifactId: String): Version = {
    System.out.println( "------------------------------------------------------------" );
    val system = Booter.newRepositorySystem();

    val session = Booter.newRepositorySystemSession( system );

    val artifact = new DefaultArtifact( "org.eclipse.aether:aether-util:[0,)" );

    val rangeRequest = new VersionRangeRequest();
    rangeRequest.setArtifact( artifact );
    rangeRequest.setRepositories( Booter.newRepositories( system, session ) );

    val rangeResult = system.resolveVersionRange( session, rangeRequest );

    val newestVersion = rangeResult.getHighestVersion();

    System.out.println( "Newest version " + newestVersion + " from repository "
      + rangeResult.getRepository( newestVersion ) );

    newestVersion
  }
}

object Remote{
  def apply() = {
    new Remote()
  }
}
