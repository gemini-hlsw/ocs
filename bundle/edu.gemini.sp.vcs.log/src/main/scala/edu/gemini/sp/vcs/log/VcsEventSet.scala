package edu.gemini.sp.vcs.log

import edu.gemini.spModel.core.SPProgramID
import edu.gemini.util.security.principal.GeminiPrincipal
import scala.collection.immutable.NumericRange

/** A rollup of consecutive `VcsEvent`s performed on the same program by the same set of principals. */
case class VcsEventSet(
  ids: Range.Inclusive,
  ops: Map[VcsOp, Int],
  timestamps: (Long, Long),
  pid: SPProgramID,
  principals: Set[GeminiPrincipal]) {

  // the Range toString is useless
  override def toString =
    s"VcsEventSet(${ids.min} to ${ids.max}, $ops, $timestamps, $pid, $principals)"

}

