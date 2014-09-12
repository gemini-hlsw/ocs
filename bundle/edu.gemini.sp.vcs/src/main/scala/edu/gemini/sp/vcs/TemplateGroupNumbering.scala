package edu.gemini.sp.vcs

import edu.gemini.pot.sp.{ISPTemplateGroup, SPNodeKey, ISPProgram}
import edu.gemini.spModel.rich.pot.sp.templateGroupWrapper
import edu.gemini.spModel.util.{VersionTokenUtil, VersionToken}

import scala.collection.JavaConverters._

/**
 * Renumbers template group VersionTokens to eliminate duplicates created by
 * the merge.
 */
private[vcs] object TemplateGroupNumbering {
  // Renumbers the merged program to match the committed program's template
  // group numbering and eliminate duplicates.
  def renumber(merged: ISPProgram, committed: ISPProgram): Unit = {
    val mergedGroups = templateGroups(merged)
    val committedMap = mapVersionTokens(committed)

    val (existingGroups, createdGroups) = mergedGroups.partition { tg =>
      committedMap.contains(tg.getNodeKey)
    }

    def versionTokens(groups: List[ISPTemplateGroup]): List[(VersionToken, SPNodeKey)] =
      groups.map { tg => tg.versionToken -> tg.getNodeKey }

    import VersionTokenUtil._
    val existingTokens = normalize(versionTokens(existingGroups))
    val createdTokens  = normalize(versionTokens(createdGroups))

    merge(existingTokens, createdTokens).foreach { case (token, key) =>
      mergedGroups.find(_.getNodeKey == key).foreach { _.versionToken = token }
    }
  }

  private def templateGroups(p: ISPProgram): List[ISPTemplateGroup] =
    Option(p.getTemplateFolder).toList.flatMap { _.getTemplateGroups.asScala.toList }

  private def mapVersionTokens(p: ISPProgram): Map[SPNodeKey, VersionToken] =
    templateGroups(p).map { tg => tg.getNodeKey -> tg.versionToken }.toMap
}
