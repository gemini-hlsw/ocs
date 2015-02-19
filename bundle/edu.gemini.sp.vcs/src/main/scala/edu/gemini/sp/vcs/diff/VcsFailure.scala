package edu.gemini.sp.vcs.diff

import scalaz.\/

sealed trait VcsFailure

object VcsFailure {
  type TryVcs[A] = VcsFailure \/ A

  /** Indicates that the local program cannot be merged with the remote
    * program.  For example, because it contains executed observations that
    * would be renumbered. */
  case class Unmergeable(msg: String) extends VcsFailure

  /** Indicates an unexpected problem while performing a vcs operation. */
  case class Unexpected(msg: String) extends VcsFailure

  /** Exception thrown while performing a vcs operation. */
  case class VcsException(ex: Throwable) extends VcsFailure
}
