package io.github.qwefgh90.repogarden.victims

import java.util.Base64

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

import org.eclipse.egit.github.core.Blob
import org.eclipse.egit.github.core.RepositoryCommit
import org.eclipse.egit.github.core.RepositoryContents
import org.eclipse.egit.github.core.TreeEntry
import org.eclipse.egit.github.core.client._
import org.eclipse.egit.github.core.service._
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

import io.github.qwefgh90.repogarden
import io.github.qwefgh90.repogarden.bp.github.Implicits._
import io.github.qwefgh90.repogarden.victims.model._

object VictimsLoader {
  def apply(oauthToken: Option[String]) = {
    new VictimsLoader(oauthToken)
  }
}

class VictimsLoader(oauthToken: Option[String]) {
	private lazy val client = {
	  val githubClient = new GitHubClient()
	  if(oauthToken.isDefined){
			githubClient.setOAuth2Token(oauthToken.get)
	  }
	    
	  githubClient
	}
	private lazy val contentService = {
			new ContentsService(client)
	}
	private lazy val repositoryService = {
			new RepositoryService(client)
	}
	private lazy val commitService = {
			new CommitService(client)
	}
	private lazy val dataService = {
			new DataService(client)
	}
	private lazy val repository = {
	  repositoryService.getRepository("victims", "victims-cve-db")
	}
	
	private val contentList = new ListBuffer[Tuple2[RepositoryContents, Victim]]()
	private val victimHomeUrl = "https://github.com/victims/victims-cve-db"
	private val victimDbUrl = "https://github.com/victims/victims-cve-db/tree/master/database/java" 
	private val victimBuffer: ListBuffer[Victim] = new ListBuffer[Victim]()
	private var lastCommit: RepositoryCommit = null
	
	private[victims] def getFirstPageIteratorOfCommits = {
			val pageIterator = commitService.pageCommits(repository, 100)
			if(pageIterator.hasNext())
			 pageIterator.next()
			else
		   throw new RuntimeException("first of page iterator must exist") 
	}

	def getLatestCveList: List[Tuple2[RepositoryContents, Victim]] = {
	  val firstPageOfCommits = getFirstPageIteratorOfCommits
	  if(firstPageOfCommits.size() != 0){
	    val lastRemoteCommit = firstPageOfCommits.iterator().next()
      val constructor = new Constructor(classOf[Victim]);//Car.class is root
      val yaml = new Yaml(constructor); 
	    if(lastCommit == null || lastRemoteCommit.getCommit.getCommitter.getDate
	        .after(lastCommit.getCommit.getCommitter.getDate)){
	    	lastCommit = lastRemoteCommit
	    	val repositoryContentList = contentService.getFiles(repository, "database/java", "heads/master", true)
	    	repositoryContentList.foreach(repositoryContent => {
          repositoryContent.setContent(repositoryContent.getContentSync(repository, dataService))
	    	})
	    	contentList.clear()
	    	contentList ++= repositoryContentList.map(repositoryContent => {
	    	  Tuple2(repositoryContent, yaml.load(repositoryContent.getContent).asInstanceOf[Victim])
	    	})
	    }
	    contentList.toList
	  }else
		  throw new RuntimeException("The iterator of first commits page must have more items than zero") 
	}
	
	case class VulnerablePart(vulnerableVersionList: List[Option[String]], relatedJavaModule: JavaModule, relatedCve: Victim)
	case class VulnerableResult(vulerableList: List[VulnerablePart])
	
	/** Return a vulnerable list wrapped by Option if related vulnerables exists.
	 * 
	 */
	def scanSingleArtifact(groupId: String, artifactId: String, version: String): Option[VulnerableResult] = {
	  val cveMap = getLatestCveMap
	  val key = groupId + "%" + artifactId
	  if(cveMap.contains(key)){
	    val affectedArtifactList = cveMap(key)
	    val resultTuple = affectedArtifactList.map(artifact => {
	      val affectedVersionList = artifact._1.getVersion
	      val resultListForEachVersion = affectedVersionList.asScala.map(versionSet => {
	        val splitedVersion = versionSet.split(",")
	        val isVulnerable = splitedVersion.toList match {
	          case List(affectedVersion) => {
	            val operator = affectedVersion.substring(0, affectedVersion.indexOf("=") + 1)
	            val parsedVersion = affectedVersion.substring(affectedVersion.indexOf("=") + 1)
	            operator match {
	              case "<=" => version.toVersion <= parsedVersion.toVersion
	              case "==" => version.toVersion == parsedVersion.toVersion
	              case ">=" => version.toVersion >= parsedVersion.toVersion
	              case _ => throw new RuntimeException("It's invalid operator")
	            }
	          }
	          case List(affectedVersion,series) => {
	            val operator = affectedVersion.substring(0, affectedVersion.indexOf("=") + 1)
	            val parsedVersion = affectedVersion.substring(affectedVersion.indexOf("=") + 1)
	            operator match {
	              case "<=" => version.startsWith(series) && version.toVersion <= parsedVersion.toVersion
	              case "==" => version.startsWith(series) && version.toVersion == parsedVersion.toVersion
	              case ">=" => version.startsWith(series) && version.toVersion >= parsedVersion.toVersion
	              case _ => throw new RuntimeException("It's invalid operator")
	            }
	          }
	          case _ =>
	            throw new RuntimeException("It's invalid version format")
	        }
	        if(isVulnerable)
	          Option(versionSet)
	        else
	        	Option.empty
	      })
	      VulnerablePart(resultListForEachVersion.filter(versionOpt => versionOpt.isDefined).toList, artifact._1, artifact._2)
	    }).filter(part => part.vulnerableVersionList.size > 0)
	    val vulnerableResult = VulnerableResult(resultTuple)
	    if(vulnerableResult.vulerableList.isEmpty)
	      Option.empty
	    else
	    	Option(vulnerableResult)
	  }else
	    Option.empty
		}
	
	/** Returns a map of groupId + "%" + artifactName to a list of Tuple2[JavaModule, Victim].
	 * It derives VictimsLoader.getLatestCveList.
	 */
	def getLatestCveMap: Map[String, List[Tuple2[JavaModule, Victim]]] = {
	  val cveList = getLatestCveList
	  val artifactToCveList = cveList.flatMap(cve => {
	    cve._2.getAffected.asScala.map(affectedArtifact => {
	      affectedArtifact.getGroupId + "%" + affectedArtifact.getArtifactId -> (affectedArtifact, cve._2)
	    })
	  })
	    
	  val artifactToCveListMap = artifactToCveList.foldLeft(Map[String, List[Tuple2[JavaModule, Victim]]]())(
	    (map: Map[String, List[Tuple2[JavaModule, Victim]]], artifactNameToMainData) => {
	      if(map.contains(artifactNameToMainData._1)){
	        val beforeList = map(artifactNameToMainData._1)
	        map + (artifactNameToMainData._1 -> (artifactNameToMainData._2 :: beforeList))
	      }else{
	        map + (artifactNameToMainData._1 -> List(artifactNameToMainData._2))
       }
    })
	  artifactToCveListMap
	}
	
/*	def scanPomFile(is: InputStream): Result = {
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
  } */

	private[victims] def getContent(repositoryContent: RepositoryContents, dataService: DataService) = {
	  val sha: String = repositoryContent.getSha
	  val blob = dataService.getBlob(repository, sha)
	  val encoding = repositoryContent.getEncoding
    val content: String = blob.getEncoding match {
    	case Blob.ENCODING_BASE64 => 
    	  if(encoding == null) new String(Base64.getMimeDecoder.decode(blob.getContent))
    	  else new String(Base64.getMimeDecoder.decode(blob.getContent), encoding)
      case Blob.ENCODING_UTF8 => blob.getContent
	  }
	  content
	}
	
	private def getAllTreeEntryFromHash(sha: String): List[TreeEntry] = {
			val tree = dataService.getTree(repository, sha)
			val entryList = tree.getTree.asScala.toList
			entryList.flatMap(entry => {
				entry.getType match {
				case TreeEntry.TYPE_TREE => getAllTreeEntryFromHash(entry.getSha)
				case TreeEntry.TYPE_BLOB => List(entry)
				case _ => List()
				}
			})
	}
}