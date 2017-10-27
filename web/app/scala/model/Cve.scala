package io.github.qwefgh90.repogarden.web.model

import io.github.qwefgh90.repogarden.bp.github.Implicits.TreeEntryEx
import org.eclipse.aether.artifact.Artifact
import io.github.qwefgh90.repogarden.victims.VulnerableResult

case class CveResult(entry: TreeEntryEx, cves: List[Cve])

case class Cve(artifact: Artifact, vulnerableResult: VulnerableResult)
