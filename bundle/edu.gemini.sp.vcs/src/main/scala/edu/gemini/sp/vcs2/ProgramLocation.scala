package edu.gemini.sp.vcs2

import scalaz._

sealed trait ProgramLocation {
  def fold[A](local: => A, remote: => A): A
}

object ProgramLocation {
  case object Local extends ProgramLocation {
    def fold[A](local: => A, remote: => A): A = local
  }

  case object Remote extends ProgramLocation {
    def fold[A](local: => A, remote: => A): A = remote
  }

  implicit val EqualProgramLocation: Equal[ProgramLocation] = Equal.equalA
}


sealed trait ProgramLocationSet {
  def +(loc: ProgramLocation): ProgramLocationSet
  def toSet: Set[ProgramLocation]
}

sealed trait PullResult extends ProgramLocationSet
sealed trait PushResult extends ProgramLocationSet

object ProgramLocationSet {
  import edu.gemini.sp.vcs2.ProgramLocation.{Local, Remote}

  case object Neither extends PullResult with PushResult {
    def +(loc: ProgramLocation): ProgramLocationSet = loc.fold(LocalOnly, RemoteOnly)
    def toSet: Set[ProgramLocation] = Set.empty
  }

  case object LocalOnly extends PullResult {
    def +(loc: ProgramLocation): ProgramLocationSet = loc.fold(this, Both)
    def toSet: Set[ProgramLocation] = Set(Local)
  }

  case object RemoteOnly extends PushResult {
    def +(loc: ProgramLocation): ProgramLocationSet = loc.fold(Both, this)
    def toSet: Set[ProgramLocation] = Set(Remote)
  }

  case object Both extends ProgramLocationSet {
    def +(loc: ProgramLocation): ProgramLocationSet = this
    def toSet: Set[ProgramLocation] = Set(Local, Remote)
  }


  implicit val EqualProgramLocationSet: Equal[ProgramLocationSet] = Equal.equalA

  implicit val EqualPullResult: Equal[PullResult] = Equal.equalA
  implicit val EqualPushResult: Equal[PushResult] = Equal.equalA
}
