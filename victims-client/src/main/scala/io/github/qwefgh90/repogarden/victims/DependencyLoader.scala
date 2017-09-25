package io.github.qwefgh90.repogarden.victims.maven.deps

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
import rx.lang.scala._
import rx.lang.scala.observables._
import rx.lang.scala.observers._
import rx.lang.scala.schedulers._
import rx.lang.scala.subjects._
import rx.lang.scala.subscriptions._
import scala.collection._
import scala.annotation.tailrec

object MvnResult extends Enumeration {
  type MvnResult = Value
  val Success, Fail = Value
}
import MvnResult._

case class ExecutionResult(log: Observable[String], result: Future[MavenResult]){}
case class MavenResult(resultCode: MvnResult, outputPath: Option[Path]){}

class DependencyLoader (mvnPath: Path, implicit val ec: ExecutionContext) {

  private val logger = Logger(classOf[DependencyLoader])
  /*
   * run "mvn dependency:tree" in a dir which containing pom.xml
   * and return observable of a result output of command
   * and return a future to hold command exit code.
   */
  def execute(pomFile: Path): ExecutionResult = {
    val builder = new ProcessBuilder(mvnPath.toAbsolutePath().toString(), "dependency:tree", "-DoutputFile=log.txt")
    builder.directory(pomFile.getParent.toFile)

    val process = builder.start()

    /*
     ReplaySubject is used for not blocking a execution of subprocess infinitely.
     Because some native platforms only provide limited buffer size for standard input and output streams, failure to promptly write the input stream or read the output stream of the subprocess may cause the subprocess to block, or even deadlock. https://docs.oracle.com/javase/7/docs/api/java/lang/Process.html#getInputStream()
     */
    val br = new BufferedReader(new InputStreamReader(process.getInputStream()))
    val stream = Stream.continually(br.readLine()).takeWhile(str => str != null)
    val replay = ReplaySubject[String]()

    ExecutionResult(replay
      , Future{blocking(
        {
          try{
            stream.foreach(str => replay.onNext(str))
          }finally{
            logger.debug("finally finished!")
            br.close()
            replay.onCompleted()
          }
          if(process.waitFor() == 0) MavenResult(MvnResult.Success, Option(pomFile.getParent.resolve("log.txt"))) else MavenResult(MvnResult.Fail, Option.empty)
        }
      )
    }
    )
  }
}
object DependencyLoader {

  def apply(mvnPath: Path, ec: ExecutionContext): DependencyLoader = {
    return new DependencyLoader(mvnPath, ec)
  }
}
