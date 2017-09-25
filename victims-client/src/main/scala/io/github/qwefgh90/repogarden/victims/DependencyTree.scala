package io.github.qwefgh90.repogarden.victims.maven.deps

import io.github.qwefgh90.repogarden.victims
import java.nio.file._
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.collection.CollectResult
import io.github.qwefgh90.repogarden.victims.util.Booter
import io.github.qwefgh90.repogarden.victims.util.ConsoleDependencyGraphDumper
import com.typesafe.scalalogging._
import org.eclipse.aether.graph.DependencyVisitor
import scala.collection.JavaConverters._
import org.apache.maven.model.building._
import org.eclipse.aether.graph._
import org.eclipse.aether.util.artifact.SubArtifact
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.collection.CollectResult
import io.github.qwefgh90.repogarden.victims.util.Booter
import io.github.qwefgh90.repogarden.victims.util.ConsoleDependencyGraphDumper
import org.eclipse.aether.resolution.ArtifactDescriptorRequest
import org.eclipse.aether.resolution.ArtifactDescriptorResult
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils
import org.eclipse.aether.util.graph.transformer.ConflictResolver
import scala.concurrent._
import scala.concurrent.blocking
import scala.io._
import java.io._
import rx.lang.scala._
import rx.lang.scala.observables._
import rx.lang.scala.observers._
import rx.lang.scala.schedulers._
import rx.lang.scala.subjects._
import rx.lang.scala.subscriptions._
import scala.collection._
import scala.annotation.tailrec

object DependencyTree {
  private val logger = Logger("DependencyTree")
  def apply(mvnOutputPath: Path) = {
    val result = new DependencyTree(createLevelLineFromMvnOutput(mvnOutputPath))
    logger.debug(s"The dependency tree is constructed for ${mvnOutputPath}")
    result
  }

  def apply(mvnOutput: List[String]) = {
    new DependencyTree(createLevelLineFromMvnOutput(mvnOutput))
//    logger.debug(s"The dependency tree is constructed for ${mvnOutputPath}")
  }

  def createLevelLineFromMvnOutput(list: List[String]): List[LevelLine] = {
    val headLevelPattern = (ch: Char) => (ch == ' ' || ch == '+' || ch == '-' || ch == '|' || ch == '\\')
    list.zipWithIndex.map(zip => LevelLine(zip._2 + 1, zip._1.takeWhile(headLevelPattern).length()/3, zip._1.dropWhile(headLevelPattern))).toList
  }

  def createLevelLineFromMvnOutput(path: Path): List[LevelLine] = {
    val source = Source.fromFile(path.toFile)
    try{
      createLevelLineFromMvnOutput(source.getLines.toList)
    }finally{
      source.close()
    }
  }
}

class DependencyTree private (val levelLines: List[LevelLine]) {
  def findParents(contain: LevelLine => Boolean): List[Found] = {
    traverseLevelLines(new Visitor[LevelLine, List[Found]] {
      override var acc: List[Found] = Nil
      override def enter(levelLine: LevelLine, stack: List[LevelLine]){
        if(contain(levelLine))
          acc = (Found(levelLine, stack) :: acc)
      }
      override def leave(levelLine: LevelLine){}
    })
  }

  def traverseLevelLines[B](visitor: Visitor[LevelLine,B]): B = {
    val levelIterator = levelLines.toIterator
    @tailrec def go(seq: Int, iterator: Iterator[LevelLine], stack: List[LevelLine]) {
      if(iterator.hasNext){
        val selected = iterator.next
        val LevelLine(seq, level, line) = selected
        val nextStack = if(stack.nonEmpty){
          val LevelLine(seq, lastLevel, lastLine) = stack.head
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

}
case class Found(line: LevelLine, parents: List[LevelLine])

trait Visitor[A,B] {
  var acc: B
  def enter(e: A, stack: List[A])
  def leave(e: A)
}
case class LevelLine(seq: Int, level: Int, line: String){
  def artifact: Artifact = {
    val keys = List("groupId", "artifactId", "extension", "version", "scope")
    val kv = keys.zipAll(line.split(":"),"","").toMap
    new DefaultArtifact(kv("groupId"), kv("artifactId"), kv("extension"), kv("version"))
  }
}
