package io.github.qwefgh90.repogarden.extractor

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import com.typesafe.scalalogging.Logger
import org.languagetool.JLanguageTool
import org.languagetool.Language
import scala.collection.JavaConverters._

class SpellSpec extends FlatSpec with Matchers {
  val logger = Logger(classOf[SpellSpec])
  
  "Language tool" should "find typos in text" in {
   val langTool = new JLanguageTool(Language.AMERICAN_ENGLISH);
  langTool.activateDefaultPatternRules();  

  val matches = langTool.check("Hitchhiker's Guide tot he Galaxy");
  matches.asScala.foreach(res => {
    println("Potential error at line " + res.getEndLine() + ", column " +
        res.getColumn() + ": " + res.getMessage());
    println(res.getFromPos + ", " + res.getToPos);
    println("Suggested correction: " +
        res.getSuggestedReplacements());
    })
  }
}