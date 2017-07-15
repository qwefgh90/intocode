package io.github.qwefgh90.repogarden.victims

import io.github.qwefgh90.repogarden.victims.maven._
import com.typesafe.scalalogging._
import org.scalactic._
import org.scalatest._
import org.eclipse.aether.version.Version;

class RemoteSpec extends FlatSpec with Matchers{
  private val logger = Logger(classOf[RemoteSpec])

  "Remote" should "return lastest version of artifacts" in {
    val remote = Remote()
    val version: Option[Version] = remote.getLastestVersion("io.github.qwefgh90","jsearch")
    assert(version.isDefined, "A artifact must exist.")
    logger.debug(s"new version : ${version}")
    val version2: Option[Version] = remote.getLastestVersion("can't.found","404")
    assert(version2.isEmpty, "A artifact must not exist.")
  }

  "Remote" should "return a list of versions" in {
    val remote = Remote()
    val l1: List[Version] = remote.getVersionList("io.github.qwefgh90","jsearch")
    assert(l1.length >= 2, "A count of version is equals to 2 or more than 2")
    l1.foreach(version => logger.debug(s"version : ${version}"))

    val l2: List[Version] = remote.getVersionList("can't.found","404")
    assert(l2.isEmpty && l2.length == 0, "A count of version is 0")
    l2.foreach(version => logger.debug(s"version : ${version}"))
  }
}
