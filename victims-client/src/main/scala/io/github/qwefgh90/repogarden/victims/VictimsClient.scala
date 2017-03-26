package io.github.qwefgh90.repogarden.victims

import java.io._
import java.net._
import java.nio.file._

import org.apache.commons.io.FileUtils
import io.github.qwefgh90.repograden.bp.Boilerplate._
import com.typesafe.scalalogging._
import com.redhat.victims.database.VictimsDB
import com.redhat.victims.database.VictimsDBInterface
import org.apache.maven.model._
import org.apache.maven.model.io.xpp3._
import java.util.HashMap
import scala.collection.JavaConversions
import com.redhat.victims.VictimsConfig
  
//http://www.victi.ms/service/v2/update/1970-01-01T00:00:00/
//api service doesn't contain information of artifacts
class VictimsClient {
  private val logger = Logger(classOf[VictimsClient])
  private val database = VictimsDB.db()
 
  def scanPomFile(uri: URI): Result = {
    readUri(uri){
      this.scanPomFile
    }.asInstanceOf[Result]
  }
  
  def count = database.getRecordCount()
  
  def lastUpdatedDate = database.lastUpdated()
  
  def synchronize() {
    database.synchronize()
  }
  def scanPomFile(is: InputStream): Result = {
	  val pomReader = new MavenXpp3Reader()
	  val pomModel = pomReader.read(new InputStreamReader(is))
	  val dependencies = JavaConversions.asScalaBuffer(pomModel.getDependencies()).toList
	  val vulnerableList = dependencies.map(dep =>{
	    val pomDependency = Map("groupId" -> dep.getGroupId, "artifactId" -> dep.getArtifactId, "version" -> dep.getVersion)
	    logger.debug(s"Dependency : ${pomDependency.toString}")
	    val cveSet = database.getVulnerabilities(new HashMap[String,String](JavaConversions.mapAsJavaMap(pomDependency)))
	    if(!cveSet.isEmpty()){
	      val cveIterator = cveSet.iterator()
	      val depToCve = Tuple2(dep, JavaConversions.asScalaIterator(cveIterator).toList)  
	      Option(depToCve)
	    }else{
	      Option.empty
	    }
	  }).filter(_.nonEmpty).map(_.get)
	  Success(vulnerableList)
  }
}

object VictimsClient {
  def apply() = {
    new VictimsClient()
  }
}