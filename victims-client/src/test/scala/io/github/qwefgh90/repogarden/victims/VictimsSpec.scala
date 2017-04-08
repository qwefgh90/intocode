package io.github.qwefgh90.repogarden.victims

import org.scalatest._
import org.scalactic._
import scala.collection.JavaConversions
import scala.collection.JavaConverters._

import org.eclipse.egit.github.core._
import org.eclipse.egit.github.core.service._
import io.github.qwefgh90.repogarden.bp.Boilerplate._
import io.github.qwefgh90.repogarden.bp.github.Implicits._
import com.typesafe.scalalogging._
import org.eclipse.egit.github.core.client.GitHubClient

class VictimsSpec extends FlatSpec with Matchers{
  private val logger = Logger(classOf[VictimsSpec])
  
  "a github object" should "get a list of files" in {
    val client = new GitHubClient()
    client.setCredentials("qwefgh90", "")
    
    val contentService = new ContentsService(client);
    val repositoryService = new RepositoryService(client);	// 
    val repo = repositoryService.getRepository("victims", "victims-cve-db")
    
    contentService.getContents(repo, null, true).foreach(content => println(content.getPath))
  }
    
  "A VictimsClient" should "return 2 items" in {
   
  }
}