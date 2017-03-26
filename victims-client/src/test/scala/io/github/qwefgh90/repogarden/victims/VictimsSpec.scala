package io.github.qwefgh90.repogarden.victims

import org.scalatest._
import org.scalactic._

import io.github.qwefgh90.repograden.bp.Boilerplate._
import com.typesafe.scalalogging._

class VictimsSpec extends FlatSpec with Matchers{
  private val logger = Logger(classOf[VictimsSpec])
  
  
  "A VictimsClient" should "return 2 items" in {
    val client = VictimsClient()
    logger.info(client.count.toString)
    client.synchronize()
    logger.info(client.count.toString)
    val pomUri = getClass().getResource("/pom.xml").toURI()
    val result = client.scanPomFile(pomUri)
    result match {
      case success: Success => logger.info(success.cveList.size.toString())
      case _ => logger.error("error")
    }

  }
  
  
}