package io.github.qwefgh90.repogarden.victims

import org.apache.maven.model.Dependency

abstract class Result
case class Success(cveList : List[Tuple2[Dependency,List[String]]]) extends Result {
  
}