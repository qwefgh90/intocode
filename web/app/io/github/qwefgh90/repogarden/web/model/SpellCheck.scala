package io.github.qwefgh90.repogarden.web.model

case class SpellCheckResult(sentence: String, positionList: List[TypoPosition])
case class TypoPosition(text: String, offset: Int, length: Int, suggestedList: List[String])
