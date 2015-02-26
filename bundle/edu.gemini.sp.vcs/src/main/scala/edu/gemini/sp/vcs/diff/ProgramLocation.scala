package edu.gemini.sp.vcs.diff

import scalaz._

sealed trait ProgramLocation

object ProgramLocation {
  case object Local  extends ProgramLocation
  case object Remote extends ProgramLocation

  implicit def ProgramLocationEqual: Equal[ProgramLocation] = Equal.equalA
}
