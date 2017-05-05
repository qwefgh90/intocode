package io.github.qwefgh90.repogarden.victims



import scala.collection.JavaConverters._

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

class VictimsSpec extends FlatSpec with Matchers{
  private val logger = Logger(classOf[VictimsSpec])
	val systemToken = System.getProperties.getProperty("oauthToken")
	val systemTokenOpt = if(systemToken == null)
	  Option.empty
	else
	  Option(systemToken)
  "A victims loader" should "load some files having cve" in {
    assert(VictimsLoader(systemTokenOpt).getFirstPageIteratorOfCommits.size() > 0)
    assert(VictimsLoader(systemTokenOpt).getLatestCveList.size > 20)
  }
  
  "A yaml parser for victic model" should "not throw exceptions" in{
	
    val constructor = new Constructor(classOf[Victim]);//Car.class is root
    val yaml = new Yaml(constructor);
    VictimsLoader(systemTokenOpt).getLatestCveList.foreach(repositoryContent => {
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
}