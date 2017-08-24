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
import io.github.qwefgh90.repogarden.bp.github.Implicits._

trait Tree2 {
  private val logger = Logger(classOf[Tree2])

  val TYPE_TREE = TreeEntry.TYPE_TREE
  val TYPE_BLOB = TreeEntry.TYPE_BLOB

  trait Visitor[A,B] {
    var acc: B
    def enter(a:A, stack:List[A])
    def leave(a:A)
  }

  /*
   * A sub class of org.eclipse.egit.github.core.TreeEntry
   * Since TreeEntry class does not provide a content of blob,
   * It provide getContent(), getBytes(), syncContent() method.
   */
  case class TreeEntryEx(seq: Int, level: Int, name: String, entry: TreeEntry) {
    private var content: String = ""
    private var encoding: String = ""
    private var bytes: Array[Byte] = Array()
    private var sync: Boolean = false

    def getContent: Option[String] = {
      if(!sync)
        throw new IllegalAccessException("The content does not loaded. please call syncContent()")
      if(encoding == Blob.ENCODING_UTF8 || encoding == Blob.ENCODING_BASE64){
        Option(content)
      }else{
        Option.empty
      }
    }

    def getBytes = {
      if(!sync)
        throw new IllegalAccessException("The content does not loaded. please call syncContent()")
      bytes
    }

    def isSync = sync

    def syncContent(repository: Repository, dataService: DataService){
      if(!sync){
  	    val sha: String = entry.getSha// this.getSha
  	    val blob = dataService.getBlob(repository, sha)
        blob.getEncoding match {
      	  case Blob.ENCODING_BASE64 =>
            bytes = Base64.getMimeDecoder.decode(blob.getContent)
          case Blob.ENCODING_UTF8 =>
            bytes = blob.getContent.getBytes
  	    }
        encoding = blob.getEncoding
        content = new String(bytes, "utf-8")
        sync = true
      }
    }
  }

  object TreeEx {
    def apply(list: List[TreeEntry]): TreeEx = {
      val newList = list.zipWithIndex.map(e => {
        val level = e._1.getPath.count(_ == '/')
        val lastIndex = (e._1.getPath.lastIndexOf("/"))
        new TreeEntryEx(e._2
          , level
          , e._1.getPath.substring(if(lastIndex == -1) 0 else lastIndex+1)
          , e._1)
      })
      new TreeEx(newList)
    }

    def apply(tree: org.eclipse.egit.github.core.Tree): TreeEx = {
      apply(tree.getTree.asScala.toList)
    }
  }

  /*
   * GitTree is a controller of a list of TreeEntry.
   * Also, It add some operations like file I/O, functional operations to manipulate tree structure.
   */
  class TreeEx private (val list: List[TreeEntryEx]) {

    /*
     * It removes empty trees.
     * @return a new instance
     */
    def trimEmpty(): TreeEx = {
      def go(list: List[TreeEntryEx]): List[TreeEntryEx] = {
        list match {
          case Nil => Nil
          case head::tail => {
            val tailAcc = go(tail)
            val headOptOfTail = tailAcc.headOption
            if(head.entry.getType == TYPE_TREE) {
              if(headOptOfTail.isEmpty){
                logger.debug(s"${head.entry.getPath} is trimed")
                tailAcc  //remove head
              }else{
                if(head.level + 1 == headOptOfTail.get.level)
                  head::tailAcc //keeping
                else{
                  logger.debug(s"${head.entry.getPath} is trimed")
                  tailAcc  //remove head
                }
              }
            }else
               head::tailAcc //keeping
          }
        }
      }
      new TreeEx(go(list))
    }

    /*
     * It only applied to blob entry.
     * @param f filter to be used
     * @return a new tree
     */
    def filterBlob(f:TreeEntryEx => Boolean): TreeEx = {
      new TreeEx(list.filter(e => { e.entry.getType match {
        case TYPE_TREE => true
        case TYPE_BLOB => f(e)
      }})).trimEmpty()
    }

    /*
     * It iterates entries using traverse().
     * @param a visitor which is a anonymous function
     * @return Unit
     */
    def foreachEnter(f:(TreeEntryEx, List[TreeEntryEx]) => Unit) {
      traverse(new Visitor[TreeEntryEx, Unit]{
        override var acc: Unit = Unit
        override def enter(e: TreeEntryEx, list: List[TreeEntryEx]){
          f(e,list)
        }
        override def leave(e: TreeEntryEx){ }
      })
    }

    /*
     * It shorten a height of tree.
     * A new tree whose a list contains only path component as prefix is created.
     * @param pathComponents prefix of tree or blob
     * @return a new tree
     */
    def shorten(pathComponents: Array[String]) = {
      val newLevel = pathComponents.length
      val pathString = pathComponents.mkString("/")
      val reducedTree = list.filter(e => e.entry.getPath.startsWith(pathString) && !e.entry.getPath.equals(pathString))
        .zipWithIndex
        .map(tuple => new TreeEntryEx(tuple._2, tuple._1.level - newLevel, tuple._1.name, tuple._1.entry))
      new TreeEx(reducedTree)
    }

    /*
     * It uses DFS algorithm.
     * @param visitor
     * @return a result to be accumlated
     */
    def traverse[B](visitor: Visitor[TreeEntryEx,B]): B = {
      val levelIterator = list.toIterator
      @annotation.tailrec
      def go(seq: Int, iterator: Iterator[TreeEntryEx], stack: List[TreeEntryEx]) {
        if(iterator.hasNext){
          val selected = iterator.next
          val TreeEntryEx(seq, level, name, line) = selected
          val nextStack = if(stack.nonEmpty){
            val TreeEntryEx(seq, lastLevel, name, lastLine) = stack.head
            val removed = if(lastLevel >= level)
              stack.take(lastLevel - level + 1)
            else
              Nil

            removed.foreach(e => {visitor.leave(e)}) //leave
            visitor.enter(selected, stack.drop(removed.length))//enter

            selected :: stack.drop(removed.length)
          }else{
            visitor.enter(selected, stack)//enter
            val newStack = selected :: Nil
            newStack
          }
          go(seq+1, iterator, nextStack)
        }else{
          stack.foreach(v => visitor.leave(v))//leave remainings
        }
      }
      go(1, levelIterator, Nil)
      visitor.acc
    }

    /*
     * It synchronize contents of TreeEntryEx of TreeEx.
     * @param repository where contents are located
     * @param dataService where contents are located
     * @return Unit
     */
    def syncContents(repository: Repository, dataService: DataService): Unit = {
      val visitor = new Visitor[TreeEntryEx, Unit]{
        override var acc: Unit = Unit
        override def enter(node: TreeEntryEx, stack: List[TreeEntryEx]) =
          node.entry.getType match {
            case TreeEntry.TYPE_BLOB =>
              node.syncContent(repository, dataService)
            case _ =>
          }
        override def leave(node: TreeEntryEx){
        }
      }
      traverse(visitor)
    }

    def writeToFileSystem(dir: Path, filter: TreeEntryEx => Boolean = e => true): Path = {
      val visitor = new Visitor[TreeEntryEx, Path]{
        override var acc: Path = dir
        override def enter(node: TreeEntryEx, stack: List[TreeEntryEx]){
          val parentPath = dir.resolve(stack.reverse.map(node => node.name).mkString(java.io.File.separator))

          node.entry.getType match {
            case TreeEntry.TYPE_TREE => {
              if(filter(node)){
                val directory = parentPath.resolve(node.name)
                if(!Files.exists(directory)){
                  Files.createDirectories(directory)
                }
              }else
                 logger.debug(s"in ${node.entry.getPath.toString}, ${node.entry.getType}, ${node.name} is filtered.")
            }
            case TreeEntry.TYPE_BLOB => {
              if(filter(node)){
                if(Files.exists(parentPath)){
                  val file = parentPath.resolve(node.name)
                  if(!Files.exists(file)){
                    val out = new BufferedOutputStream(Files.newOutputStream(file, CREATE, APPEND))
                    try{
                      out.write(node.getBytes, 0, node.getBytes.length);
                    } finally{
                      out.close()
                    }
                  }
                }
              }else
                 logger.debug(s"in ${node.entry.getPath.toString}, ${node.entry.getType}, ${node.name} is filtered.")
            }
          }
        }
        override def leave(node: TreeEntryEx){}
      }
      traverse(visitor)
    }
  }
}

