package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.{ISPProgram, SPNodeKey}
import edu.gemini.pot.sp.version.VersionMap

/** Groups the information required to calculate a
  * [[edu.gemini.sp.vcs2.ProgramDiff]]. */
case class DiffState(progKey: SPNodeKey, vm: VersionMap, removed: Set[SPNodeKey])

object DiffState {
  def apply(p: ISPProgram): DiffState =
    DiffState(p.getProgramKey, p.getVersions, removedKeys(p))
}
