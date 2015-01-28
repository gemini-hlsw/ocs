package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.sp.vcs.diff.Diff.Missing

/** Describes the modifications required for a local program to complete a
  * merge.
  */
case class MergePlan(update: MergeNode, delete: Set[Missing]) {

  /** Accepts a program and edits it according to this merge plan. */
  def merge(p: ISPProgram): Unit = ???
}
