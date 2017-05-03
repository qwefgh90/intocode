package io.github.qwefgh90.repogarden.victims

import org.eclipse.egit.github.core._
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service._
import org.scalactic._
import org.scalatest._
import scala.collection.JavaConverters._

import com.typesafe.scalalogging._
import java.io.InputStreamReader
import org.yaml.snakeyaml._
import io.github.qwefgh90.repogarden.victims.model.Victim
import org.yaml.snakeyaml.constructor.Constructor
import io.github.qwefgh90.repogarden.victims.model.JavaModule

class VictimsSpec extends FlatSpec with Matchers{
  private val logger = Logger(classOf[VictimsSpec])
 
  
  "a github object" should "get a list of files" in {
    //val client = new GitHubClient()
    //client.setCredentials("qwefgh90", "")
    
    //val contentService = new ContentsService(client);
    //val repositoryService = new RepositoryService(client);	// 
    //val repo = repositoryService.getRepository("victims", "victims-cve-db")
    
    //contentService.getContents(repo, null, true).foreach(content => println(content.getPath))
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
        , List(">=1.3.2,1.3").asJava);
    val mod2 = new JavaModule("org.apache.tomcat", "tomcat-catalina"
        , List("<=9.0.0.M7,9", "<=8.5.2,8.5"
        , "<=8.0.35,8.0"
        , "<=7.0.69,7").asJava
        , List(
         ">=9.0.0.M8,9"
        , ">=8.5.3,8.5"
        , ">=8.0.36,8.0"
        , ">=7.0.70,7").asJava);
    assert(victim.getAffected.get(0) === mod1)
    assert(victim.getAffected.get(1) === mod2)
    
  }
}