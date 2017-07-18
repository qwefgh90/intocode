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

object Implicits {

  private val logger = Logger(Implicits.getClass)
  
  /**
	* A converter from ContentsService to ContentsServiceEx
	*/
  implicit def extendContentsService(service: ContentsService) = new ContentsServiceEx(service)
  implicit def extendString(str: String) = new StringEx(str)

  trait Visitor[A,B] {
    var acc: B
    def enter(a:A, stack:List[A])
    def leave(a:A)
  }

  abstract class Node(val get: RepositoryContentsExtend)
  object NilNode extends Node(new RepositoryContentsExtend)
  case class TreeNode(override val get: RepositoryContentsExtend, children: List[Node]) extends Node(get)
  case class TerminalNode(override val get: RepositoryContentsExtend) extends Node(get)
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

    def syncContents(repository: Repository, dataService: DataService) = {
      val visitor = new Visitor[Node, Unit]{
        override var acc: Unit = Unit
        override def enter(node: Node, stack: List[Node]) =
          node match {
            case node: TerminalNode =>
              node.get.syncContent(repository, dataService)
            case _ =>
          }
        override def leave(node: Node){
        }
      }
      traverse(visitor)
    }

    def writeToFileSystem(dir: Path, filter: Option[Node => Boolean]): Path = {
      val visitor = new Visitor[Node, Path]{
        override var acc: Path = dir
        override def enter(node: Node, stack: List[Node]){
          val parentPath = dir.resolve(stack.reverse.map(node => node.get.getName).mkString(java.io.File.separator))
          if(filter.isDefined && filter.get(node)){
            logger.debug(s"in ${node.get.getPath}, ${node.get.getName} is filtered.")
          }else{
            node match {
              case node: TreeNode => {
                val directory = parentPath.resolve(node.get.getName)
                if(!Files.exists(directory)){
                  Files.createDirectories(directory)
                }
              }
              case node: TerminalNode => {
                if(!Files.exists(parentPath)){
                  Files.createDirectories(parentPath)
                }
                val exNode = node.get
                val file = parentPath.resolve(node.get.getName)
                if(!Files.exists(file)){
                  val out = new BufferedOutputStream(Files.newOutputStream(file, CREATE, APPEND))
                  try{
                    out.write(exNode.getBytes, 0, exNode.getBytes.length);
                  } finally{
                    out.close()
                  }
                }
              }
            }
          }
        }
        override def leave(node: Node){}
      }
      traverse(visitor)
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

	def getContentsTree(repoProvider: IRepositoryIdProvider, rootPath: String, ref: String): Tree = {
      def go(path: String): List[Node] = {
	    val contentList = contentsService.getContents(repoProvider, path, ref).asScala.toList
	    val wrappedList = contentList
          .map(content => new RepositoryContentsExtend(content))
          .map{content =>
		  if(content.getType == RepositoryContents.TYPE_DIR)
		    TreeNode(content, go(content.getPath))
		  else{
            TerminalNode(content)
          }
	    }
        wrappedList
      }
      Tree(go(rootPath))
	}
  }

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
