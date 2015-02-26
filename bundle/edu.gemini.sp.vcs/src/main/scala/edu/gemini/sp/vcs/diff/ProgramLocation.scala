package edu.gemini.sp.vcs.diff

import scalaz._

sealed trait ProgramLocation

sealed trait ProgramLocationSet {
  def +(loc: ProgramLocation): ProgramLocationSet
}

object ProgramLocation {
  case object Local  extends ProgramLocation
  case object Remote extends ProgramLocation

  implicit def ProgramLocationEqual: Equal[ProgramLocation] = Equal.equalA

  case object Neither    extends ProgramLocationSet {
    def +(loc: ProgramLocation): ProgramLocationSet = loc match {
      case Local  => LocalOnly
      case Remote => RemoteOnly
    }

  }
  case object LocalOnly  extends ProgramLocationSet {
    def +(loc: ProgramLocation): ProgramLocationSet = loc match {
      case Remote => Both
      case _      => this
    }
  }

  case object RemoteOnly extends ProgramLocationSet {
    def +(loc: ProgramLocation): ProgramLocationSet = loc match {
      case Local => Both
      case _     => this
    }
  }
  case object Both       extends ProgramLocationSet {
    def +(loc: ProgramLocation): ProgramLocationSet = this
  }

  implicit def ProgramLocationSetEqual: Equal[ProgramLocationSet] = Equal.equalA
}
