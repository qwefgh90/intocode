package io.github.qwefgh90.repogarden.victims

import java.nio.file._
import scala.io._
import com.typesafe.scalalogging._

class DependencyTreeWrapper(val path: Path){
  private val logger = Logger(classOf[DependencyTreeWrapper])
  lazy val dependenciesList: List[String] = {
    val source = Source.fromFile(path.toFile)
    val result = source.getLines.toList
    source.close()
    result
  }

  def getDependenciesList = dependenciesList

/*  def getParentList(artifactId: String, groupId: String, verison: String){

  }*/
}
