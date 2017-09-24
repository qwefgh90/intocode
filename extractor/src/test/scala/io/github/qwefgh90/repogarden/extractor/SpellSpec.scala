package io.github.qwefgh90.repogarden.extractor

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import com.typesafe.scalalogging.Logger
import org.languagetool.JLanguageTool
import org.languagetool.Language
import org.languagetool.language.AmericanEnglish
import org.languagetool.rules._
import org.languagetool._
import org.languagetool.rules.spelling.SpellingCheckRule
import scala.collection.JavaConverters._

class SpellSpec extends FlatSpec with Matchers {
  val logger = Logger(classOf[SpellSpec])

  "Language tool" should "find typos or grammar error in text" in {
	val langTool = new JLanguageTool(new AmericanEnglish);

    val rules = langTool.getAllActiveRules().asScala
    rules.foreach{r =>
      logger.debug(s"${r.getId} | ${r.getCategory.toString} | ${r.getDescription}")
    }
    
    val categories = langTool.getCategories()
    categories.keySet.asScala.foreach{e =>
      logger.debug(e.toString)
    }
    
    langTool.getAllRules().asScala.foreach{rule =>
      if (!rule.isDictionaryBasedSpellingRule()) {
        langTool.disableRule(rule.getId());
      }
      if (rule.isInstanceOf[SpellingCheckRule]) {
        val wordsToIgnore = List("@author");
        (rule.asInstanceOf[SpellingCheckRule]).addIgnoreTokens(wordsToIgnore.asJava);
      }
    }
    //		langTool.activateDefaultPatternRules();

	val sentence1 = "Hitchhiker's Guide tot he Galaxy"
	val matches = langTool.check(sentence1);
	matches.asScala.foreach(res => {
	  logger.debug(s"Potential error occurs from ${res.getFromPos} to ${res.getToPos} (${sentence1.substring(res.getFromPos, res.getToPos)})" +
		s"at line ${res.getEndLine()}, column ${res.getColumn()} ${res.getMessage()}")
	  logger.debug("Suggested correction: " +
		res.getSuggestedReplacements());
	})

	val sentence2 = "Hitchhiker's book aer niec one veryvery happy"
	val matches2 = langTool.check(sentence2);
	matches2.asScala.foreach(res => {
	  logger.debug(s"Potential error occurs from ${res.getFromPos} to ${res.getToPos} (${sentence2.substring(res.getFromPos, res.getToPos)})" +
		s"at line ${res.getEndLine()}, column ${res.getColumn()} ${res.getMessage()}")
	  logger.debug("Suggested correction: " +
		res.getSuggestedReplacements());

	})
  }
}
