package io.github.qwefgh90.repogarden.extractor

import java.io._
import scala.collection.mutable._
import scala.util.matching.Regex

import io.github.qwefgh90.bp.Boilerplate._
import com.typesafe.scalalogging._

/** Object contains various comment parsers. */
object Parser{
  val logger = Logger(Parser.getClass)

  implicit def bufferToWrapper(array: ArrayBuffer[Byte]) = {
    new WrapperArrayByffer(array)
  }

  /**
    * @constructor create new wrapper for finding sequence
    * @param array an array to be wrapped
    */
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
    def findFromFirst(str: String): Boolean = {
      val bytesToFind = str.getBytes()
      val leftPart = array.take(bytesToFind.length)
      if(leftPart.lengthCompare(bytesToFind.length) != 0)
        false
      else
        leftPart.sameElements(bytesToFind)
    }
    def findFromFirstIgnoreCase(str: String): Boolean = {
      val bytesToFind = str.toLowerCase.getBytes()
      val leftPart = array.take(bytesToFind.length).map(b => b.toChar.toLower.toByte)
      if(leftPart.lengthCompare(bytesToFind.length) != 0)
        false
      else
        leftPart.sameElements(bytesToFind)
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

    var state: Int = NOP
    readStream(stream){ currentByte: Byte => {
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
  def parseCType(stream: InputStream): Option[List[Array[Byte]]] = 
  {
    //http://www.open-std.org/jtc1/sc22/wg14/www/docs/n1124.pdf
    //http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2014/n4296.pdf
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

def parseScalaType(stream: InputStream): Option[List[Array[Byte]]] = {
    val listBuffer = new ListBuffer[Array[Byte]]()
    val lexicalBuf = new ArrayBuffer[Byte]()
    val NOP = -1
    val START_TRANDITIONAL_COMMENT = 1
    val START_EOL_COMMENT = 2
    val START_STRING = 3
    val START_MULTI_STRING = 4
    var state: Int = NOP
    readStream(stream){ currentByte: Byte => {
      //https://www.scala-lang.org/files/archive/spec/2.11/01-lexical-syntax.html#string-literals
      lexicalBuf += currentByte
      state match {
        case NOP => {
          if(lexicalBuf.findFromLast("\"", false)){
            state = START_STRING
          }else if(lexicalBuf.findFromLast("/*", false)){
            lexicalBuf.clear()
            state = START_TRANDITIONAL_COMMENT
          }else if(lexicalBuf.findFromLast("//", false)){
            lexicalBuf.clear()
            state = START_EOL_COMMENT
          }
        }
        case START_STRING => {
          if(lexicalBuf.findFromLast("\"\"\"", false)){
            lexicalBuf.clear()
            state = START_MULTI_STRING
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
        case START_MULTI_STRING => {
          if(lexicalBuf.findFromLast("\"\"\"", false)){
            state = NOP
          }
        }
        case START_TRANDITIONAL_COMMENT => {
          if(lexicalBuf.findFromLast("*/", true)){
            lexicalBuf.trimEnd(2)
            listBuffer += lexicalBuf.toArray
            state = NOP
          }
        }
        case START_EOL_COMMENT => {
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
def parseRubyType(stream: InputStream): Option[List[Array[Byte]]] = {
    val listBuffer = new ListBuffer[Array[Byte]]()
    val lexicalBuf = new ArrayBuffer[Byte]()
    val NOP = -1
    val START_COMMENT_SINGLE = 1
    val START_COMMENT_MULTI = 2
    val START_STRING_SINGLE = 4
    val START_STRING_DOUBLE = 5
    val START_STRING_NON_EXPANDED = 6
    val START_STRING_EXPANDED = 7
    val START_HERE_DOC = 8
    val START_HERE_DOC_SINGLE_QUOTED = 9
    val START_HERE_DOC_DOUBLE_QUOTED = 10
    val START_HERE_DOC_COMMAND_QUOTED = 11
   def IS_IDENTIFIER_CHARS(byte: Byte) = {
     if(('a' <= byte && byte <= 'z') || ('A' <= byte && byte <= 'Z') || byte == '_' || ('0' <= byte && byte <= '9')) true else false
   }
  val BEGINING_DELIMITER_LIST = List('{','(','[','<')
  val ENDING_DELIMITER_LIST = List('}',')',']','>')
  var beginingDelimiter = 0
    var signifierDelimiter = ""
    var state: Int = NOP
    readStream(stream){ currentByte: Byte => {
      //http://www.ipa.go.jp/files/000011432.pdf
      lexicalBuf += currentByte
      state match {
        case NOP => {
          if(lexicalBuf.findFromLast("'", false)){
            lexicalBuf.clear()
            state = START_STRING_SINGLE
          }else if(lexicalBuf.findFromLast("\"", false)){
            lexicalBuf.clear()
            state = START_STRING_DOUBLE
          }else if(lexicalBuf.findFromLast("#", false)){
            lexicalBuf.clear()
            state = START_COMMENT_SINGLE
          }else if(lexicalBuf.findFromLast("=begin", false)){
            lexicalBuf.clear()
            state = START_COMMENT_MULTI
          }else if(lexicalBuf.dropRight(1).findFromLast("%q", false)){
            if(BEGINING_DELIMITER_LIST.exists(lexicalBuf.findFromLast(_,false))){
              beginingDelimiter = lexicalBuf.last
              lexicalBuf.clear()
              state = START_STRING_NON_EXPANDED
            }
          }else if(lexicalBuf.dropRight(1).findFromLast("%Q", false)){
            if(BEGINING_DELIMITER_LIST.exists(lexicalBuf.findFromLast(_,false))){
              beginingDelimiter = lexicalBuf.last
              lexicalBuf.clear()
              state = START_STRING_EXPANDED
            }
          }
        }
        case START_STRING_SINGLE => {
          if(lexicalBuf.findFromLast("'", false)){
            lexicalBuf.clear()
            state = NOP
          }
        }
        case START_STRING_DOUBLE => {
          if(lexicalBuf.findFromLast("\"", false)){
            lexicalBuf.clear()
            state = NOP
          }
        }
        case START_COMMENT_SINGLE => {
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
        case START_COMMENT_MULTI => {
          if(lexicalBuf.findFromLast("=end", true)){
            lexicalBuf.trimEnd(4)
            listBuffer += lexicalBuf.toArray
            state = NOP
          }
        }
        case START_STRING_NON_EXPANDED => {
          if(lexicalBuf.findFromLast(ENDING_DELIMITER_LIST(BEGINING_DELIMITER_LIST.indexOf(beginingDelimiter)), false)){
            lexicalBuf.clear()
            state = NOP
          }
        }
        case START_STRING_EXPANDED => {
          if(lexicalBuf.findFromLast(ENDING_DELIMITER_LIST(BEGINING_DELIMITER_LIST.indexOf(beginingDelimiter)), false)){
            lexicalBuf.clear()
            state = NOP
          }
        }
      }
    }
    }
    Option(listBuffer.toList)
  }

def parseGoType(stream: InputStream): Option[List[Array[Byte]]] = 
  {
    //https://golang.org/ref/spec#Comments
    val listBuffer = new ListBuffer[Array[Byte]]()
    val lexicalBuf = new ArrayBuffer[Byte]()
    val NOP = -1
    val START_TRANDITIONAL_COMMENT = 0
    val START_EOL_COMMENT = 1
    val START_STRING = 2
    val START_RAW_STRING = 3

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
          else if(lexicalBuf.findFromLast("`", true)){
            lexicalBuf.clear()
            state = START_RAW_STRING
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
        case START_RAW_STRING => {
          if(lexicalBuf.findFromLast('`', false)){
            state = NOP
          }
        }
      }
      currentByte = stream.read() //read a next byte
    }
    Option(listBuffer.toList)
  }


  /** Parse comments from a byte stream of .js
    * 
    * @param stream a stream to parse
    * @return a list of byte arrays
    */
  def parseJsType(stream: InputStream): Option[List[Array[Byte]]] = {
    //https://www.ecma-international.org/publications/files/ECMA-ST/Ecma-262.pdf
    val listBuffer = new ListBuffer[Array[Byte]]()
    val lexicalBuf = new ArrayBuffer[Byte]()
    val NOP = -1
    val START_TRANDITIONAL_COMMENT = 0
    val START_EOL_COMMENT = 1
    val START_DOUBLE_STRING = 2
    val START_SINGLE_STRING = 3

    var state: Int = NOP
    var currentByte = stream.read() //read a first byte
    while(currentByte != -1){
      //FSM
      lexicalBuf += currentByte
      state match {
        case NOP => {
          if(lexicalBuf.findFromLast('"', true)){
            state = START_DOUBLE_STRING
          }
          else if(lexicalBuf.findFromLast('\'', true)){
            state = START_SINGLE_STRING
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
        case START_DOUBLE_STRING => {
          if(lexicalBuf.findFromLast('"', false)){
            state = NOP
          }
        }
        case START_SINGLE_STRING => {
          if(lexicalBuf.findFromLast('\'', false)){
            state = NOP
          }
        }
      }
      currentByte = stream.read() //read a next byte
    }
    Option(listBuffer.toList)
  }
  /** Parse comments from a byte stream of .html
    * 
    * @param stream a stream to parse
    * @return a list of byte arrays
    */
  def parseHtmlType(stream: InputStream): Option[List[Array[Byte]]] = {
    //https://www.w3.org/TR/html4/intro/sgmltut.html#h-3.2.4
    val listBuffer = new ListBuffer[Array[Byte]]()
    val lexicalBuf = new ArrayBuffer[Byte]()
    val NOP = -1
    val START_COMMENT = 0
    val START_COMMENT_DOUBLE_DASH = 1

    var state: Int = NOP
    var currentByte = stream.read() //read a first byte
    while(currentByte != -1){
      //FSM
      lexicalBuf += currentByte
      state match {
        case NOP => {
          if(lexicalBuf.findFromLast("<!--", true)){
            lexicalBuf.clear()
            state = START_COMMENT
          }
        }
        case START_COMMENT =>{
          if(lexicalBuf.findFromLast("--", true)){
            state = START_COMMENT_DOUBLE_DASH
          }
        }
        case START_COMMENT_DOUBLE_DASH => {
          if(lexicalBuf.findFromLast(">", true)){
            lexicalBuf.trimEnd(3)
            listBuffer += lexicalBuf.toArray
            state = NOP
          }else if(lexicalBuf.findFromLast(" ", true)){
            lexicalBuf.trimEnd(1)
          }else{
            state = START_COMMENT
          }
        }
      }
      currentByte = stream.read() //read a next byte
    }
    Option(listBuffer.toList)
  }

  /** Parse comments from a byte stream of .bat
    * 
    * @param stream a stream to parse
    * @return a list of byte arrays
    */
  def parseBatType(stream: InputStream): Option[List[Array[Byte]]] = {
    //
    val listBuffer = new ListBuffer[Array[Byte]]()
    val lexicalBuf = new ArrayBuffer[Byte]()
    val NOP = -1
    val START_COMMENT = 0

    var state: Int = NOP
    var currentByte = stream.read() //read a first byte
    while(currentByte != -1){
      //FSM
      lexicalBuf += currentByte
      state match {
        case NOP => {
          if(currentByte == '\n')
            lexicalBuf.clear()
          else if(lexicalBuf.findFromFirstIgnoreCase("rem ")){
            lexicalBuf.clear()
            state = START_COMMENT
          }
          else if(currentByte == ' ')
            lexicalBuf.trimEnd(1)
        }
        case START_COMMENT =>{
          if(lexicalBuf.findFromLast("\r\n", false)){
            lexicalBuf.trimEnd(2)
            listBuffer += lexicalBuf.toArray
            lexicalBuf.clear()
            state = NOP
          }
          else if(lexicalBuf.findFromLast("\n", false)){
            lexicalBuf.trimEnd(1)
            listBuffer += lexicalBuf.toArray
            lexicalBuf.clear()
            state = NOP
          }
        }
      }
      currentByte = stream.read() //read a next byte
    }
    Option(listBuffer.toList)
  }



  /** Parse comments from a byte stream of .sh
    * 
    * @param stream a stream to parse
    * @return a list of byte arrays
    */
  def parseShType(stream: InputStream): Option[List[Array[Byte]]] = {

    //http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html
    val listBuffer = new ListBuffer[Array[Byte]]()
    val lexicalBuf = new ArrayBuffer[Byte]()
    val NOP = -1
    val START_COMMENT = 0
    val START_STRING_SINGLE = 1
    val START_STRING_DOUBLE = 2

    var state: Int = NOP
    var currentByte = stream.read() //read a first byte
    while(currentByte != -1){
      //FSM
      lexicalBuf += currentByte
      state match {
        case NOP => {
          if(currentByte == '\''){
            lexicalBuf.clear()
            state = START_STRING_SINGLE
          }
          else if(currentByte == '\"'){
            lexicalBuf.clear()
            state = START_STRING_DOUBLE
          }
          else if(currentByte == '#'){
            lexicalBuf.clear()
            state = START_COMMENT
          }
        }
        case START_STRING_SINGLE => {
          if(lexicalBuf.findFromLast('\'', false)){
            lexicalBuf.clear()
            state = NOP
          }
        }
        case START_STRING_DOUBLE =>{
          if(lexicalBuf.findFromLast('"', false)){
            lexicalBuf.clear()
            state = NOP
          }
        }
        case START_COMMENT => {
          if(lexicalBuf.findFromLast("\r\n", false)){
            lexicalBuf.trimEnd(2)
            listBuffer += lexicalBuf.toArray
            lexicalBuf.clear()
            state = NOP
          }
          else if(lexicalBuf.findFromLast('\n', false)){
            lexicalBuf.trimEnd(1)
            listBuffer += lexicalBuf.toArray
            lexicalBuf.clear()
            state = NOP
          }
        }
      }
      currentByte = stream.read() //read a next byte
    }
    Option(listBuffer.toList)
  }

//https://www.w3.org/XML/Core/#Publications
}
