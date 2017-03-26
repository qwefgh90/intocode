package io.github.qwefgh90.repogarden.extractor

import java.net._
import java.io._
import java.nio.file._
import Types._
import Parser._

import scala.collection.mutable._

import io.github.qwefgh90.bp.Boilerplate._

import org.apache.tika.mime._
import io.github.qwefgh90.jsearch._
import com.typesafe.scalalogging._


/** Object to extract comments. */
object Extractor {
  val logger = Logger(Extractor.getClass)

  val dummy = File.createTempFile("will be deleted", "will be deleted");
  dummy.deleteOnExit()
  
  /** extract a list of comments from uri.
    * 
    * @param uri a uri to create stream from
    * @param fileName a file name to detect a media type
    * @return a list of comments
    */
  def extractComments(uri: URI, fileName: String): Option[List[String]] = {
	  val tempFile = File.createTempFile("will be deleted", "will be deleted");
  	  tempFile.deleteOnExit()
  	  readUri(uri){is =>
    	  val result = extractComments(is, JSearch.getContentType(tempFile, fileName))
    	  result.map{list => {
    		  for(byteArray <- list; str = byteArrayToString(byteArray))
    			  yield str
    	  }
  	  }
		}.asInstanceOf[Option[List[String]]]
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
      case GO_TYPE => {
        parseGoType(stream)
      }
      case JS_TYPE => {
        parseJsType(stream)
      }
      case HTML_TYPE => {
        parseHtmlType(stream)
      }
      case BAT_TYPE => {
        parseBatType(stream)
      }
      case SH_TYPE => {
        parseShType(stream)
      }

    }
  }
}
