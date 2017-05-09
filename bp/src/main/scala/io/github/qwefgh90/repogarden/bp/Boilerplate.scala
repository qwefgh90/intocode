package io.github.qwefgh90.repogarden.bp

import java.io._
import java.net._
import java.nio.charset.Charset
import org.mozilla.universalchardet.UniversalDetector
import scala.collection.mutable._
import java.security.InvalidParameterException


object Boilerplate {
  
  val fileScheme = "file"
  val httpsScheme = "https"
  val httpScheme = "http"
  
  /**
   * @param input Integer value to be converted to Byte
   */
  implicit def intToByte(input: Int): Byte = {
    input.toByte
  }
  
  /**
   * @param byteArray Byte array to be properly decoded and converted to String  
   */
  implicit def byteArrayToString(byteArray: Array[Byte]): String = {
    val detector = new UniversalDetector(null);
  	detector.handleData(byteArray, 0, byteArray.length)
  	detector.dataEnd()
  	val detectedCharset = detector.getDetectedCharset();
  	new String(byteArray, Charset.forName(if(detectedCharset == null) "UTF-8" else detectedCharset));
  }

  /**
   * @param stream stream which bytes connect with
   */
  implicit def streamToArray(stream: InputStreamReader): Array[Char] = {
    val buf = new ArrayBuffer[Char](50000) //50KB
    readStream(stream)(buf+=_)
    buf.toArray
  }

  /** Automatically close the stream after run op().
    * 
    * @param inputStream a inputstream to close
    * @param op a operation to execute
    * @return A return value of op()
    */
  def autoClose(inputStream: InputStream)(op: InputStream => Any): Any = {
    try{
      op(inputStream)
    }finally{
      if(inputStream!=null)
        inputStream.close()
    }
  }

  /**
   * Read all bytes from stream
   * 
   * @param stream A stream to read
   */
  def readStream(stream: InputStreamReader)(op: Char => Unit) = {
    var currentByte = stream.read()
    while(currentByte != -1){
      op(currentByte.toChar)
      currentByte = stream.read()
    }
  }
  
  /**
   * Process uri as inputStream
   * 
   * @param uri a uri to be converted to stream
   * @return A return value of process
   */
  def readUri(uri: URI)(process : InputStream => Any): Any = {
    val tempFile = File.createTempFile("will be deleted", "will be deleted");
    
    uri.getScheme() match {
      case `fileScheme` => {
        val file = new File(uri) //take a file part. A file object avoids error of leading slash
        val is = new FileInputStream(file)
        autoClose(is){is =>
          process(is)
        }
      }
      case `httpsScheme` => {
        val url = uri.toURL
        val is = url.openStream()
        process(is)
      }
      case `httpScheme` => {
        val url = uri.toURL
        val is = url.openStream()
       process(is)
      }
      case _ => {
        //can't handle it
        throw new InvalidParameterException(s"There isn't a handler for ${uri.toString()}")
      }
    }
  }
}