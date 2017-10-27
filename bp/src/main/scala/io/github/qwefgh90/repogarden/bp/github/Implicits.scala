package io.github.qwefgh90.repogarden.bp.github

import scala.collection.JavaConverters._

import org.eclipse.egit.github.core._
import org.eclipse.egit.github.core.service._
import java.util.Base64
import scala.concurrent.Future
import scala.util.Try
import org.eclipse.aether.util.version._
import org.eclipse.aether.version._
import com.typesafe.scalalogging._
import java.nio.file._
import java.nio.file.StandardOpenOption._
import java.io._
import com.typesafe.scalalogging._

object Implicits extends Tree { 

  private val logger = Logger(Implicits.getClass)
  
  /**
	* A converter from ContentsService to ContentsServiceEx
	* A converter from ContentsService to StringEx
	*/
  implicit def extendContentsService(service: ContentsService) = new ContentsServiceEx(service)
  implicit def extendString(str: String) = new StringEx(str)

  /*
   * A extension of org.eclipse.egit.github.core.service.ContentsService
   * It adds some recursive functions
   * And a special function to return simple git object tree of remote a repository.
   * You should import io.github.qwefgh90.repogarden.bp.github.Implicit which is a object
   */
  class ContentsServiceEx(contentsService: ContentsService){
    private val logger = Logger(classOf[ContentsServiceEx])
	/**
	  * @param repoProvider repository provider
	  * @param path if path is null, It iterates contents from root. Otherwise, It iterates contents from path
      * @param ref It's sha or a head
	  * @param recursive whether to iterate all sub directories
	  * @return a list of contents
	  */
	def getContents(repoProvider: IRepositoryIdProvider, path: String, ref: String, recursive: Boolean): List[RepositoryContentsExtend] = {
	  val contentList = contentsService.getContents(repoProvider, path, ref).asScala.toList
	  contentList.flatMap{content =>
		if(content.getType == RepositoryContents.TYPE_DIR && recursive == true)
		  getContents(repoProvider, content.getPath, ref, true)
		else
		  List(content)
	  }.map(content => new RepositoryContentsExtend(content))
	}
    
	def getContents(repoProvider: IRepositoryIdProvider, path: String, ref: String, recursive: Boolean, containingTree: Boolean): List[RepositoryContentsExtend] = {
	  val contentList = contentsService.getContents(repoProvider, path, ref).asScala.toList
	  contentList.flatMap{content =>
		if(content.getType == RepositoryContents.TYPE_DIR && recursive == true)
		  content :: getContents(repoProvider, content.getPath, ref, true)
		else
		  List(content)
	  }.map(content => new RepositoryContentsExtend(content))
	}
  }

  /*
   * A sub class of org.eclipse.egit.github.core.RepositoryContents.
   * Basically, RepositoryContents does not provide a content field.
   * So, it adds a function for synchrizing a content of remote, which take a long time.
   * To construct a instance, a instance of RepositoryContents is necessary.
   */
  class RepositoryContentsExtend(base: RepositoryContents) extends RepositoryContents{
    this.setSha(base.getSha)
    this.setType(base.getType)
    this.setPath(base.getPath)
    this.setName(base.getName)
    this.setSize(base.getSize)
    this.setEncoding(base.getEncoding)
    this.setContent("")

    def this() = this(new RepositoryContents)

    private var bytes: Array[Byte] = Array()
    def getBytes() = bytes
    def setBytes(arr: Array[Byte]) = {
      bytes = arr
    }

    def syncContent(repository: Repository, dataService: DataService){
      if(getBytes.length == 0 && getContent.length == 0){
  	    val sha: String = this.getSha
  	    val blob = dataService.getBlob(repository, sha)
  	    val encoding = this.getEncoding
        val content: String = blob.getEncoding match {
      	  case Blob.ENCODING_BASE64 =>
      	    if(encoding == null) new String(Base64.getMimeDecoder.decode(blob.getContent))
      	    else new String(Base64.getMimeDecoder.decode(blob.getContent), encoding)
          case Blob.ENCODING_UTF8 => blob.getContent
  	    }
        this.setContent(content)
        this.setBytes(content.getBytes)
      }
    }
  }

  val scheme = new GenericVersionScheme()

  class VersionWrapper(versionStr: String) extends Ordered[VersionWrapper]{
	val version: Version = scheme.parseVersion(versionStr)
	def compare(that: VersionWrapper) = {
      version.compareTo(that.version)
	}
	
	override def equals(other: Any) = {
	  other match {
	    case that: VersionWrapper => {
          version.equals(that.version)
	    }
	    case _ => false
	  }

	}
	
	override def hashCode: Int = this.version.hashCode
	
	def canEqual(other: Any): Boolean = {
	  other.isInstanceOf[VersionWrapper]
	}
  }


  class StringEx(str: String){
	def toVersion = { new VersionWrapper(str) }
  }
}
