package edu.gemini.sp.vcs2

import scalaz._

sealed trait ProgramLocation {
  def fold[A](loc: => A, rem: => A): A
}

sealed trait ProgramLocationSet {
  def +(loc: ProgramLocation): ProgramLocationSet
}

object ProgramLocation {
  case object Local  extends ProgramLocation {
    def fold[A](loc: => A, rem: => A): A = loc
  }

  case object Remote extends ProgramLocation {
    def fold[A](loc: => A, rem: => A): A = rem
  }

  implicit def ProgramLocationEqual: Equal[ProgramLocation] = Equal.equalA

  case object Neither    extends ProgramLocationSet {
    def +(loc: ProgramLocation): ProgramLocationSet = loc.fold(LocalOnly, RemoteOnly)
  }

  case object LocalOnly  extends ProgramLocationSet {
    def +(loc: ProgramLocation): ProgramLocationSet = loc.fold(this, Both)
  }

  case object RemoteOnly extends ProgramLocationSet {
    def +(loc: ProgramLocation): ProgramLocationSet = loc.fold(Both, this)
  }

  case object Both       extends ProgramLocationSet {
    def +(loc: ProgramLocation): ProgramLocationSet = this
  }

  implicit def ProgramLocationSetEqual: Equal[ProgramLocationSet] = Equal.equalA
}
