package io.github.qwefgh90.repogarden.extractor

import org.scalatest._
import org.scalactic._
import Matchers._
import scala.collection.mutable.ArrayBuffer

import java.net._
import java.io.File

import io.github.qwefgh90.repogarden.extractor.Parser._
import io.github.qwefgh90.repogarden.extractor.Extractor._
import io.github.qwefgh90.repogarden.bp.Boilerplate._

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
  
  "Results of extracting java.java" should "be equal to 5th, 6th comment" in {
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

  "Results of extracting py.py" should "be equal to 5th, 6th comment" in {
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

  "Results of extracting c.c" should "be equal to 14th, 15th comment" in {
    val cUri = getClass.getResource("/c.c").toURI
    val cList = extractComments(cUri, "c.c").get
    (cList(13) shouldEqual """ Rotate the list removing the tail node and inserting it to the head. """) (after being ignoreSpecialChars)
    (cList(16) should equal (""" Test a EOF Comment""")) (after being ignoreSpecialChars)
  }
  "Results of extracting h.h" should "be equal to 2th, 3th comment" in {
    val hUri = getClass.getResource("/h.h").toURI
    val hList = extractComments(hUri, "h.h").get
    (hList(1) shouldEqual """This comment is for test """) (after being ignoreSpecialChars)
    (hList(2) should equal (""" Node, List, and Iterator are the only data structures used currently.""")) (after being ignoreSpecialChars)
  }

  "Results of extracting scala.scala" should "be equal to 1th, 3th comment" in {
    val scalaUri = getClass.getResource("/scala.scala").toURI
    val hList = extractComments(scalaUri, "scala.scala").get
    (hList(0) shouldEqual """* * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com> """) (after being ignoreSpecialChars)
    (hList(12) should equal ("""look at you""")) (after being ignoreSpecialChars)
  }

  "Results of extracting rb.rb" should "be equal to 1th, 2th comment" in {
    val rbUri = getClass.getResource("/rb.rb").toURI
    val rbList = extractComments(rbUri, "rb.rb").get
    (rbList(0) shouldEqual """ Returns the version of the currently loaded Rails as a <tt>Gem::Version</tt> """) (after being ignoreSpecialChars)
    (rbList(1) should equal ("""can you see it? mutiline comment""")) (after being ignoreSpecialChars)
  }

  "Results of extracting go.go" should "be equal to 5th, 6th comment" in {
    val goUri = getClass.getResource("/go.go").toURI
    val goList = extractComments(goUri, "go.go").get
    (goList(4) shouldEqual """ e.g. it matches what we generate with Sign() """) (after being ignoreSpecialChars)
    (goList(5) should equal (""" It's a traditional comment.It's a traditional comment.""")) (after being ignoreSpecialChars)
  }

  "Results of extracting js.js" should "be equal to 1th, 16th comment" in {
    val jsUri = getClass.getResource("/js.js").toURI
    val jsList = extractComments(jsUri, "js.js").get
    (jsList(0) shouldEqual """eslint-disable no-unused-vars""") (after being ignoreSpecialChars)
    (jsList(15) should equal ("""  build.js inserts compiled jQuery here""")) (after being ignoreSpecialChars)
  }

  "Results of extracting html.html" should "be equal to 1th, 2th comment" in {
    val htmlUri = getClass.getResource("/html.html").toURI
    val htmlList = extractComments(htmlUri, "html.html").get
    (htmlList(0) shouldEqual """<script type='text/javascript' src='http://getfirebug.com/releases/lite/1.2/firebug-lite-compressed.js'></script>""") (after being ignoreSpecialChars)
    (htmlList(1) should equal (""" app title """)) (after being ignoreSpecialChars)
  }

  "Results of extracting bat.bat" should "be equal to 1th, 18th comment" in {
    val batUri = getClass.getResource("/bat.bat").toURI
    val batList = extractComments(batUri, "bat.bat").get
    (batList(0) shouldEqual """Licensed to the Apache Software Foundation (ASF) under one or more""") (after being ignoreSpecialChars)
    (batList(17) should equal ("""Get remaining unshifted command line arguments and save them in the""")) (after being ignoreSpecialChars)
  }

  "Results of extracting sh.sh" should "be equal to 1th, 22th comment" in {
    val shUri = getClass.getResource("/sh.sh").toURI
    val shList = extractComments(shUri, "sh.sh").get
    (shList(0) shouldEqual """!/bin/sh""") (after being ignoreSpecialChars)
    (shList(21) should equal (""" -x will Only work on the os400 if the files are:""")) (after being ignoreSpecialChars)
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

    val cStream = getClass.getResourceAsStream("/c.c")
    val cList = parseCType(cStream)
    cList should not be None
    cList.get.size shouldEqual 17
    cStream.close()
    
    val hStream = getClass.getResourceAsStream("/h.h")
    val hList = parseCType(hStream)
    hList should not be None
    hList.get.size shouldEqual 7
    hStream.close()

    val scalaStream = getClass.getResourceAsStream("/scala.scala")
    val scalaList = parseScalaType(scalaStream)
    scalaList should not be None
    scalaList.get.size shouldEqual 13
    scalaStream.close()

    val rubyStream = getClass.getResourceAsStream("/rb.rb")
    val rubyList = parseRubyType(rubyStream)
    rubyList should not be None
    rubyList.get.size shouldEqual 2
    rubyStream.close()

    val goStream = getClass.getResourceAsStream("/go.go")
    val goList = parseGoType(goStream)
    goList should not be None
    goList.get.size shouldEqual 6
    goStream.close()

    val jsStream = getClass.getResourceAsStream("/js.js")
    val jsList = parseJsType(jsStream)
    jsList should not be None
    jsList.get.size shouldEqual 16
    jsStream.close()

    val htmlStream = getClass.getResourceAsStream("/html.html")
    val htmlList = parseHtmlType(htmlStream)
    htmlList should not be None
    htmlList.get.size shouldEqual 9
    htmlStream.close()

    val batStream = getClass.getResourceAsStream("/bat.bat")
    val batList = parseBatType(batStream)
    batList should not be None
    batList.get.size shouldEqual 18
    batStream.close()

    val shStream = getClass.getResourceAsStream("/sh.sh")
    val shList = parseShType(shStream)
    shList should not be None
    shList.get.size shouldEqual 25
    shStream.close()

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
