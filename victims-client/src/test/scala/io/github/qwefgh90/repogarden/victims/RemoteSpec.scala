package io.github.qwefgh90.repogarden.victims

import io.github.qwefgh90.repogarden.victims.maven.remote._
import com.typesafe.scalalogging._
import org.scalactic._
import org.scalatest._

class RemoteSpec extends FlatSpec with Matchers{
  private val logger = Logger(classOf[RemoteSpec])

  "Remote" should "get lastest version of artifacts" in {
    val remote = Remote()
    logger.debug(s"new version : ${remote.checkUpdate("","")}")
  }


}
