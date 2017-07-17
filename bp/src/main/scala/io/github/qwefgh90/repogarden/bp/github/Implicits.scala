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

object Implicits {

  trait Visitor[A,B] {
    var acc: B
    def enter(a:A, stack:List[A])
    def leave(a:A)
  }
  abstract class Node(val get: RepositoryContents)
  object NilNode extends Node(new RepositoryContents)
  case class TreeNode(override val get: RepositoryContents, children: List[Node]) extends Node(get)
  case class TerminalNode(override val get: RepositoryContents) extends Node(get)
  case class Tree(children: List[Node]) {
    def traverse[B](visitor: Visitor[Node, B]): B = {
      def go(node: Node, stack: List[Node]): Unit = node match {
        case terminalNode: TerminalNode => {
          visitor.enter(terminalNode, stack)
          visitor.leave(terminalNode)
        }
        case treeNode: TreeNode => {
          visitor.enter(treeNode, stack)
          treeNode.children.foreach(child => {
            go(child, treeNode::stack)
          })
          visitor.leave(treeNode)
        }
      }
      children.foreach(child => {
        go(child, Nil)
      })
      visitor.acc
    }
  }

  class ContentsServiceEx(contentsService: ContentsService){
    private val logger = Logger(classOf[ContentsServiceEx])
	/**
	  * @param repoProvider repository provider
	  * @param path if path is null, iterate contents from root. Otherwise, iterate contents from path
	  * @param recursive whether to iterate all sub directories
	  * @return a list of contents
	  */
	def getContents(repoProvider: IRepositoryIdProvider, path: String, ref: String, recursive: Boolean): List[RepositoryContents] = {
	  val contentList = contentsService.getContents(repoProvider, path, ref).asScala.toList
	  contentList.flatMap{content =>
		if(content.getType == RepositoryContents.TYPE_DIR && recursive == true)
		  getContents(repoProvider, content.getPath, ref, true)
		else
		  List(content)
	  }
	}
    
	def getContents(repoProvider: IRepositoryIdProvider, path: String, ref: String, recursive: Boolean, containingTree: Boolean): List[RepositoryContents] = {
	  val contentList = contentsService.getContents(repoProvider, path, ref).asScala.toList
	  contentList.flatMap{content =>
		if(content.getType == RepositoryContents.TYPE_DIR && recursive == true)
		  content :: getContents(repoProvider, content.getPath, ref, true)
		else
		  List(content)
	  }
	}

	def getContentsTree(repoProvider: IRepositoryIdProvider, rootPath: String, ref: String): Tree = {
      def go(path: String): List[Node] = {
	    val contentList = contentsService.getContents(repoProvider, path, ref).asScala.toList
	    val wrappedList = contentList.map{content =>
		  if(content.getType == RepositoryContents.TYPE_DIR)
		    TreeNode(content, go(content.getPath))
		  else
		    TerminalNode(content)
	    }
        wrappedList
      }
      Tree(go(rootPath))
	}
  }
  
  class RepositoryContentsEx(repositoryContent: RepositoryContents){
	def getContentSync(repository: Repository, dataService: DataService) = {
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
  
  /**
	* A converter from ContentsService to ContentsServiceEx
	*/
  implicit def extendContentsService(service: ContentsService) = new ContentsServiceEx(service)
  implicit def extendRepositoryContents(content: RepositoryContents) = new RepositoryContentsEx(content)
  implicit def extendString(str: String) = new StringEx(str)
}
