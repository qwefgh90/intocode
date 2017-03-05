package io.github.qwefgh90.repogarden.extractor

import java.net._
import java.io._
import java.nio.file._
import java.nio.charset.Charset;
import Types._
import Parser._

import scala.collection.mutable._

import org.apache.tika.mime._
import io.github.qwefgh90.jsearch._
import org.mozilla.universalchardet.UniversalDetector
import com.typesafe.scalalogging._

/** Object to extract comments. */
object Extractor {
  val logger = Logger(Extractor.getClass)

  val fileScheme = "file"
  val httpsScheme = "https"
  val httpScheme = "http"

  /** Automatically close the stream after run op().
    * 
    * @param inputStream a inputstream to close
    * @param op a operation to execute
    */
  def autoClose(inputStream: InputStream)(op: InputStream => Any) = {
    try{
      op(inputStream)
    }finally{
      if(inputStream!=null)
        inputStream.close()
    }
  }

  val dummy = File.createTempFile("will be deleted", "will be deleted");
  dummy.deleteOnExit()

  implicit def byteArrayToString(byteArray: Array[Byte]): String = {
    val detector = new UniversalDetector(null);
	detector.handleData(byteArray, 0, byteArray.length)
	detector.dataEnd()
	val detectedCharset = detector.getDetectedCharset();
	new String(byteArray, Charset.forName(if(detectedCharset == null) "UTF-8" else detectedCharset));
  }

  implicit def streamToArray(stream: InputStream): Array[Byte] = {
    val buf = new ArrayBuffer[Byte](50000) //50KB
    readBytes(stream)(buf+=_)
    buf.toArray
  }

  def readBytes(stream: InputStream)(op: Byte => Unit) = {
    var currentByte = stream.read()
    while(currentByte != -1){
      op(currentByte)
      currentByte = stream.read()
    }
  }

  /** Extract a list of comments from uri.
    * 
    * @param uri a uri to create stream from
    * @param fileName a file name to detect a media type
    * @return a list of comments
    */
  def extractComments(uri: URI, fileName: String): Option[List[String]] = {
    val tempFile = File.createTempFile("will be deleted", "will be deleted");
    val mediaType = JSearch.getContentType(dummy, fileName)

    uri.getScheme() match {
      case `fileScheme` => {
        val file = new File(uri) //take a file part. A file object avoids error of leading slash
        val is = new FileInputStream(file)
        autoClose(is){is =>
          val result = extractComments(is, JSearch.getContentType(tempFile, fileName))
          result.map{
            list => {
              for(byteArray <- list; str = byteArrayToString(byteArray))
              yield str
            }
          }
        }.asInstanceOf[Option[List[String]]]
      }
      case `httpsScheme` => {
        val url = uri.toURL
        val is = url.openStream()
        Option(List())
      }
      case `httpScheme` => {
        val url = uri.toURL
        val is = url.openStream()
        Option(List())
      }
      case _ => {
        //can't handle it
        Option(List())
      }
    }
  }

  /** Extract a list of byte arrays from a stream.
    * 
    * @param stream a byte stream to extract a text
    * @param mediaType a media type that decide a parser
    * @return a list of byte arrays
    */
  private def extractComments(stream: InputStream, mediaType: MediaType): Option[List[Array[Byte]]] = {
    mediaType match {
      case JAVA_TYPE => { 
        parseJavaType(stream)
      }
      case PY_TYPE => { 
        parsePyType(stream)
      }
      case C_TYPE => {
        parseCType(stream)
      }
      case C_HEADER_TYPE => {
        parseCType(stream)
      }
      case CPP_TYPE => {
        parseCType(stream)
      }
      case CPP_HEADER_TYPE => {
        parseCType(stream)
      }
      case SCALA_TYPE => {
        parseCType(stream)
      }
      case RUBY_TYPE => {
        parseRubyType(stream)
      }
    }
  }
}
