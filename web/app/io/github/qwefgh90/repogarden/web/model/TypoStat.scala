package io.github.qwefgh90.repogarden.web.model

import io.github.qwefgh90.repogarden.web.model.State._

case class TypoStat(id: Option[Long], ownerId: String, repositoryId: String, branchName: String, commitSha: String, startTime: Option[Long], completeTime: Option[Long], message: String, state: String, userId: String)

case class Typo(parentId: Long, path: String, treeSha: String, issueCount: Int, spellCheckResult: String, highlight: String)

