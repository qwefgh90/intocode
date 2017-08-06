package io.github.qwefgh90.repogarden.web.model

case class TypoStat(id: Option[Long], repositoryId: String, commitSha: String, completeTime: Long, message: String)

case class Typo(parentId: Long, path: String, treeSha: String, issueCount: Int, spellCheckResult: String, highlight: String)

