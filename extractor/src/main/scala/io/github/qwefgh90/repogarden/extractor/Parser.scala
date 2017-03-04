package io.github.qwefgh90.repogarden.extractor

import java.io._
import scala.collection.mutable._
import scala.util.matching.Regex
import com.typesafe.scalalogging._

/** Object contains various comment parsers. */
object Parser{
  val logger = Logger(Parser.getClass)

  def readBytes(stream: InputStream)(op: Byte => Unit) = {
    var currentByte = stream.read()
    while(currentByte != -1){
      op(currentByte)
      currentByte = stream.read()
    }
  }

  implicit def intToByte(input: Int): Byte = {
    input.toByte
  }

  implicit def bufferToWrapper(array: ArrayBuffer[Byte]) = {
    new WrapperArrayByffer(array)
  }

  class WrapperArrayByffer(array: ArrayBuffer[Byte]){
    def findFromLast(ch: Char, allowFrontBackslash: Boolean):Boolean = {
      findFromLast(ch.toString, allowFrontBackslash)
    }
    /** Find string at end of array
      * 
      * @param str string to find
      * @return if string exists in array return true, if not false.
      */
    def findFromLast(str: String, allowFrontBackslash: Boolean): Boolean = {
      val bytesToFind = str.getBytes()
      val rightPart = array.takeRight(bytesToFind.length)
      val existBackslash = if(array.length > bytesToFind.length) array(array.length - bytesToFind.length - 1) == '\\' else false
      if(rightPart.lengthCompare(bytesToFind.length) != 0)
        false
      else if(!allowFrontBackslash && existBackslash)
        false
      else
        rightPart.sameElements(bytesToFind)
    }
  }

  /** Parse comments from a byte stream of .java
    * 
    * @param stream a stream to parse
    * @return a list of byte arrays
    */
  def parseJavaType(stream: InputStream): Option[List[Array[Byte]]] = {
    //https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.7
    val listBuffer = new ListBuffer[Array[Byte]]()
    val lexicalBuf = new ArrayBuffer[Byte]()
    val NOP = -1
    val START_TRANDITIONAL_COMMENT = 0
    val START_EOL_COMMENT = 1
    val START_STRING = 2

    var state: Int = NOP
    var currentByte = stream.read() //read a first byte
    while(currentByte != -1){
      //FSM
      lexicalBuf += currentByte
      state match {
        case NOP => {
          if(lexicalBuf.findFromLast('"', true)){
            state = START_STRING
          }
          else if(lexicalBuf.findFromLast("/*", true)){
            lexicalBuf.clear()
            state = START_TRANDITIONAL_COMMENT
          }
          else if(lexicalBuf.findFromLast("//", true)){
            lexicalBuf.clear()
            state = START_EOL_COMMENT
          }
        }
        case START_TRANDITIONAL_COMMENT =>{
          if(lexicalBuf.findFromLast("*/", true)){
            lexicalBuf.trimEnd(2)
            listBuffer += lexicalBuf.toArray
            state = NOP
          }
        }
        case START_EOL_COMMENT => {
          if(lexicalBuf.findFromLast("\r\n", true)){
            lexicalBuf.trimEnd(2)
            listBuffer += lexicalBuf.toArray
            state = NOP
          }else if(lexicalBuf.findFromLast("\n", true)){
            lexicalBuf.trimEnd(1)
            listBuffer += lexicalBuf.toArray
            state = NOP
          }
        }
        case START_STRING => {
          if(lexicalBuf.findFromLast('"', false)){
            state = NOP
          }
        }
      }
      currentByte = stream.read() //read a next byte
    }
    Option(listBuffer.toList)
  }
  def parsePyType(stream: InputStream): Option[List[Array[Byte]]] = {
    val listBuffer = new ListBuffer[Array[Byte]]()
    val lexicalBuf = new ArrayBuffer[Byte]()
    val NOP = -1
    val START_SHARP = 0
    val START_TRIPLE = 1
    val START_STRING_SINGLE = 2
    val START_STRING_DOUBLE = 3
    val START_STRING_TRIPLE = 4
    val NOT_BYTE = 1000000
    var state: Int = NOP
    readBytes(stream){ currentByte: Byte => {
      //https://docs.python.org/3/reference/lexical_analysis.html#literals
      //https://docs.python.org/2/reference/lexical_analysis.html#string-literals

      lexicalBuf += currentByte
      state match {
        case NOP => {
          if(lexicalBuf.findFromLast("'", true)){
            state = START_STRING_SINGLE
          }else if(lexicalBuf.findFromLast('"', true)){
            state = START_STRING_DOUBLE
          }else if(lexicalBuf.findFromLast('#', true)){
            lexicalBuf.clear()
            state = START_SHARP
          }
        }
        case START_STRING_SINGLE => {
          if(lexicalBuf.findFromLast("'''", false)){
            lexicalBuf.clear()
            state = START_STRING_TRIPLE
          }
          else if(lexicalBuf.dropRight(1).findFromLast("''", false)){
            state = NOP
          }
          else if(lexicalBuf.findFromLast("''", false)){
            //pass
          }
          else if(lexicalBuf.findFromLast("'", false)){
            state = NOP
          }
        }
        case START_STRING_DOUBLE => {
          if(lexicalBuf.findFromLast("\"\"\"", false)){
            lexicalBuf.clear()
            state = START_TRIPLE
          }
          else if(lexicalBuf.dropRight(1).findFromLast("\"\"", false)){
            state = NOP
          }
          else if(lexicalBuf.findFromLast("\"\"", false)){
            //pass
          }
          else if(lexicalBuf.findFromLast("\"", false)){
            state = NOP
          }
        }
        case START_STRING_TRIPLE => {
          if(lexicalBuf.findFromLast("'''", false)){
            state = NOP
          }
        }
        case START_TRIPLE => {
          if(lexicalBuf.findFromLast("\"\"\"", false)){
            lexicalBuf.trimEnd(3)
            listBuffer += lexicalBuf.toArray
            state = NOP
          }
        }
        case START_SHARP => {
          if(lexicalBuf.findFromLast("\n", true)){
            lexicalBuf.trimEnd(1)
            listBuffer += lexicalBuf.toArray
            state = NOP
          }else if(lexicalBuf.findFromLast("\r\n", true)){
            lexicalBuf.trimEnd(2)
            listBuffer += lexicalBuf.toArray
            state = NOP
          }
        }
      }
    }
    }
    Option(listBuffer.toList)
  }
}
