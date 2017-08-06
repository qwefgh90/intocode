package io.github.qwefgh90.repogarden.web.dao

import org.languagetool.JLanguageTool
import org.languagetool.Language
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.JavaConverters._
import io.github.qwefgh90.repogarden.web.model._

class SpellCheckService {
  val langTool = new JLanguageTool(Language.AMERICAN_ENGLISH);
  langTool.activateDefaultPatternRules();

  def check(sentence: String): SpellCheckResult = {
    val matchesJava = langTool.check(sentence)
    val matches: List[org.languagetool.rules.RuleMatch] = 
      if(matchesJava == null)
        List()
      else
        matchesJava.asScala.toList
    
    SpellCheckResult(sentence, matches.map(res => TypoPosition(sentence.substring(res.getFromPos, res.getToPos), res.getFromPos, res.getToPos, res.getSuggestedReplacements().asScala.toList)))
  }
}
