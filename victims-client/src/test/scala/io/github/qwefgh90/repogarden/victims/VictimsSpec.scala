package io.github.qwefgh90.repogarden.victims

import scala.io._
import scala.collection.JavaConverters._
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

import org.sonatype.aether.RepositorySystem
import org.sonatype.aether.RepositorySystemSession
import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.graph.Dependency
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.resolution.ArtifactDescriptorRequest
import org.sonatype.aether.resolution.ArtifactDescriptorResult
import org.sonatype.aether.util.artifact.DefaultArtifact
import io.github.qwefgh90.repogarden.victims.util


class VictimsSpec extends FlatSpec with Matchers{
  private val logger = Logger(classOf[VictimsSpec])
  val systemToken = System.getProperties.getProperty("oauthToken")
  val systemTokenOpt = if(systemToken == null)
	Option.empty
  else
	Option(systemToken)
  
  require(systemTokenOpt.isDefined)
  /*
   "A victims loader" should "load some files having cve" in {
   assert(VictimsLoader(systemTokenOpt).getFirstPageIteratorOfCommits.size() > 0)
   assert(VictimsLoader(systemTokenOpt).getLatestCveList.size > 20)
   }
   
   "A yaml parser for victic model" should "not throw exceptions" in{
   val constructor = new Constructor(classOf[Victim]);//Car.class is root
   val yaml = new Yaml(constructor);
   VictimsLoader(systemTokenOpt).getLatestCveList.foreach(repositoryContent => {
   logger.debug(repositoryContent._1.getPath)
   val content: String = repositoryContent._1.getContent
   val victim = yaml.load(content).asInstanceOf[Victim]
   assert(victim.getTitle != null && victim.getTitle.length() > 0)
   assert(victim.getCve != null && victim.getCve.length() > 0)
   assert(victim.getAffected != null && victim.getAffected.size() > 0)
   })
   }
   
   "A Victim model" should "hold information in fields" in {
   val constructor = new Constructor(classOf[Victim]);//Car.class is root
   val yaml = new Yaml(constructor);
   val victim = yaml.load(getClass().getResource("/test.yaml").openStream()).asInstanceOf[Victim];
   
   assert(victim.getCve === "2016-3092")
   assert(victim.getTitle === "Apache Commons Fileupload: Denial of Service")
   assert(victim.getDescription.replaceAll("[\n\r\t ]", "") === """
   A malicious client can send file upload requests that cause the HTTP server
   using the Apache Commons Fileupload library to become unresponsive, preventing
   the server from servicing other requests. A fork of this component
   is also included in Apache Tomcat.""".replaceAll("[\n\r\t ]", "") )
   assert(victim.getCvss_v2 === "4.3")
   assert(victim.getReferences.size === 5)
   assert(victim.getAffected.size === 2)
   val mod1 = new JavaModule("commons-fileupload", "commons-fileupload"
   , List("<=1.3.1,1.3", "<=1.2.2,1.2").asJava
   , List(">=1.3.2,1.3").asJava
   , null);
   val mod2 = new JavaModule("org.apache.tomcat", "tomcat-catalina"
   , List("<=9.0.0.M7,9", "<=8.5.2,8.5"
   , "<=8.0.35,8.0"
   , "<=7.0.69,7").asJava
   , List(
   ">=9.0.0.M8,9"
   , ">=8.5.3,8.5"
   , ">=8.0.36,8.0"
   , ">=7.0.70,7").asJava
   , null);
   assert(victim.getAffected.get(0) === mod1)
   assert(victim.getAffected.get(1) === mod2)
   }
   */

  "Maven Model" should "return all dependencies" in {

    System.out.println( "------------------------------------------------------------" );

    val system = Booter.newRepositorySystem();
    val session = Booter.newRepositorySystemSession( system );

    val artifact = new DefaultArtifact( "org.sonatype.aether:aether-impl:1.9" );

    val repo = Booter.newCentralRepository();

    val descriptorRequest = new ArtifactDescriptorRequest();
    descriptorRequest.setArtifact( artifact );
    descriptorRequest.addRepository( repo );

    val descriptorResult = system.readArtifactDescriptor( session, descriptorRequest );

    val iter = descriptorResult.getDependencies().iterator
    while(iter.hasNext)
    {
      System.out.println( iter.next );
    }

  }
  
  //https://github.com/victims/victims-cve-db/blob/master/database/java/2016/3092.yaml
  "A VictimsLoader" should "scan a vulnerable package" in {
    val victimLoader = VictimsLoader(systemTokenOpt);
    val resultOpt = victimLoader.scanSingleArtifact("org.apache.tomcat", "tomcat-catalina", "8.0.33")
    assert(resultOpt.isDefined)
    assert(resultOpt.get.vulerableList.size == 1)
    val resultOpt2 = victimLoader.scanSingleArtifact("org.apache.tomcat", "tomcat-catalina", "8.0.35")
    assert(resultOpt2.isDefined)
    assert(resultOpt2.get.vulerableList.size == 1)
    val resultOpt3 = victimLoader.scanSingleArtifact("org.apache.tomcat", "tomcat-catalina", "8.0.36")
    assert(resultOpt3.isEmpty)
    val resultOpt4 = victimLoader.scanSingleArtifact("commons-fileupload", "commons-fileupload", "1.3.2")
    assert(resultOpt4.isDefined)
    val resultOpt5 = victimLoader.scanSingleArtifact("commons-fileupload", "commons-fileupload", "1.3.1-jenkins-1")
    assert(resultOpt5.isDefined)
    assert(resultOpt5.get.vulerableList.size == 4)
    val resultOpt6 = victimLoader.scanSingleArtifact("commons-fileupload", "commons-fileupload", "1.3")
    assert(resultOpt6.isDefined)
    assert(resultOpt6.get.vulerableList.size == 4)
  }
}
