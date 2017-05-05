package io.github.qwerfgh90.repogarden.bp

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import com.typesafe.scalalogging.Logger
import io.github.qwefgh90.repogarden.bp.github.Implicits._

class BpSpec extends FlatSpec with Matchers{
  private val logger = Logger(classOf[BpSpec])
  
  "A version string" should "be correctly compared with other version string" in {
    val version0 = "1"
    val targetVersion0 = "2"
    assert(version0.toVersion < targetVersion0.toVersion)
    assert(version0.toVersion <= targetVersion0.toVersion)
    val version1 = "1.1.1.2"
    val targetVersion1 = "1.1.1.3"
    assert(version1.toVersion < targetVersion1.toVersion)
    assert(version1.toVersion <= targetVersion1.toVersion)
    val version2 = "1.1.1.2"
    val targetVersion2 = "1.1.2.3"
    assert(version2.toVersion < targetVersion2.toVersion)
    assert(version2.toVersion <= targetVersion2.toVersion)
    val version3 = "1.1.4.5"
    val targetVersion3 = "1.1.4.4"
    assert(version3.toVersion > targetVersion3.toVersion)
    assert(version3.toVersion >= targetVersion3.toVersion)
    val version4 = "1.1.4.5"
    val targetVersion4 = "1.1.4.5"
    assert(version4.toVersion == targetVersion4.toVersion)
    val version5 = "9.0.0.M7"
    val targetVersion5 = "9.0.0.M20"
    assert(version5.toVersion == targetVersion5.toVersion)
    val version6 = "9.0"
    val targetVersion6 = "9.0.1"
    assert(version6.toVersion <= targetVersion6.toVersion)
  }
}