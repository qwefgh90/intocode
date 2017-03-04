package example

import org.scalatest._
import org.scalactic._
import Matchers._
import scala.collection.mutable.ArrayBuffer

import java.net._
import java.io.File

import io.github.qwefgh90.repogarden.extractor.Parser._
import io.github.qwefgh90.repogarden.extractor.Extractor._

import org.mozilla.universalchardet.UniversalDetector
import io.github.qwefgh90.jsearch._

import com.typesafe.scalalogging._

class ExtractorSpec extends FlatSpec with Matchers {
  val logger = Logger(classOf[ExtractorSpec])

  val ignoreSpecialChars =
    new Uniformity[String] {
      def normalized(s: String) = s.replaceAll("[\t\n\r ]", "").toLowerCase
      def normalizedCanHandle(o: Any) = o.isInstanceOf[String]
      def normalizedOrSame(o: Any): Any =
        o match {
          case str: String => normalized(str)
          case _ => o
        }
    }

  implicit def intToByte(input: Int): Byte = {
    input.toByte
  }

  "Results of java extractor" should "be equal to 5th, 6th comment" in {
    val javaUri = getClass.getResource("/java.java").toURI
    val javaList = extractComments(javaUri, "java.java").get
    (javaList(5) shouldEqual """*
	 * factory class of <b>TikaMimeXmlObject</b>
	 * 
	 * @author choechangwon
	 *
	 """) (after being ignoreSpecialChars)
    (javaList(6) should equal (""" load from property file""")) (after being ignoreSpecialChars)
  }

  "Results of py extractor" should "be equal to a part of 5th comment, 6th" in {
    val pyUri = getClass.getResource("/py.py").toURI
    val pyList = extractComments(pyUri, "py.py").get
    (pyList(5) should startWith ("""
Requests HTTP library
~~~~~~~~~~~~~~~~~~~~~

Requests is an HTTP library, written in Python, for human beings. Basic GET
usage:"""))

    logger.info(pyList(6).toString)

    (pyList(6).toString should equal (""" Attempt to enable urllib3's SNI support, if possible""")) (after being ignoreSpecialChars)
  }


  "Parser" should "return a count of comments we excect" in {
    val javaStream = getClass.getResourceAsStream("/java.java")
    val javaList = parseJavaType(javaStream)
    javaList should not be None
    javaList.get.size shouldEqual 11
    javaStream.close()

    val pyStream = getClass.getResourceAsStream("/py.py")
    val pyList = parsePyType(pyStream)
    pyList should not be None
    pyList.get.size shouldEqual 11
    pyStream.close()
  }

  "URI Scheme" should "http or file" in {
    var uri = new URI("file://c:/한글")
    assert(uri.getScheme equalsIgnoreCase "file")
    (uri.getSchemeSpecificPart shouldEqual "//c:/한글") (after being ignoreSpecialChars)
    uri = new URI("http://google.com")
    assert(uri.getScheme equalsIgnoreCase "http")
    (uri.getSchemeSpecificPart shouldEqual "//google.com") (after being ignoreSpecialChars)
  }

  "UniversalDetector" should "detect currect encoding" in {
    val uri = new URI("https://raw.githubusercontent.com/qwefgh90/test/master/euc_kr.txt")
    val is = uri.toURL.openStream()
    val buf = new ArrayBuffer[Byte]()

    var byte = is.read()
    while(byte != -1){
      buf += byte
      byte = is.read()
    }
    val immuBuf = buf.toArray
    val detector = new UniversalDetector(null);
	detector.handleData(immuBuf, 0, immuBuf.length);
	detector.dataEnd();
	val detectedCharset = detector.getDetectedCharset();
    assert(detectedCharset equals "EUC-KR")
    is.close()
  }

  "getContentType() " should "print types" in {
    val tempFile = File.createTempFile("will be deleted", "will be deleted");
    tempFile.deleteOnExit()
    //Languages work on System
    logger.info(JSearch.getContentType(tempFile, "hello.java").toString);
    logger.info(JSearch.getContentType(tempFile, "hello.py").toString);
    logger.info(JSearch.getContentType(tempFile, "hello.c").toString);
    logger.info(JSearch.getContentType(tempFile, "hello.cpp").toString);
    logger.info(JSearch.getContentType(tempFile, "hello.h").toString);
    logger.info(JSearch.getContentType(tempFile, "hello.hpp").toString);
    logger.info(JSearch.getContentType(tempFile, "hello.scala").toString);
    logger.info(JSearch.getContentType(tempFile, "hello.rb").toString);
    logger.info(JSearch.getContentType(tempFile, "hello.go").toString);

    //Languages work on Browser
    logger.info(JSearch.getContentType(tempFile, "hello.js").toString);
    logger.info(JSearch.getContentType(tempFile, "hello.html").toString);
    logger.info(JSearch.getContentType(tempFile, "hello.css").toString);

    //Languages work on Shell
    logger.info(JSearch.getContentType(tempFile, "hello.bat").toString);
    logger.info(JSearch.getContentType(tempFile, "hello.sh").toString);

    //Text format
    logger.info(JSearch.getContentType(tempFile, "hello.xml").toString);
    logger.info(JSearch.getContentType(tempFile, "hello.properties").toString);
    logger.info(JSearch.getContentType(tempFile, "hello.txt").toString);

    //ETC format
    logger.info(JSearch.getContentType(tempFile, "hello.md").toString);
    logger.info(JSearch.getContentType(tempFile, "hello").toString);

  }
}
