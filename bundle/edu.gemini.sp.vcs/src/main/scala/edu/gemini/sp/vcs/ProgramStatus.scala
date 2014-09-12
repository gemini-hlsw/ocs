package edu.gemini.sp.vcs

import edu.gemini.pot.sp.version._

/**
 * The relative status of one program to another with respect to
 * VCS.
 */

sealed trait ProgramStatus extends Ordered[ProgramStatus] {
  def order: Int

  def compare(that: ProgramStatus): Int = order - that.order

  def isLocallyModified: Boolean = false
  def isRemotelyModified: Boolean = false
  def needsSync: Boolean = isLocallyModified || isRemotelyModified
}

object ProgramStatus {
  case object Unknown        extends ProgramStatus {
    val order: Int = 0
  }

  case object PendingSync    extends ProgramStatus {
    val order: Int = 1
    override def isLocallyModified  = true
    override def isRemotelyModified = true
  }

  case object PendingUpdate  extends ProgramStatus {
    val order: Int = 2
    override def isRemotelyModified = true
  }

  case object PendingCheckIn extends ProgramStatus {
    val order: Int = 3
    override def isLocallyModified = true
  }

  case object UpToDate       extends ProgramStatus {
    val order: Int = 4
  }

  val all: List[ProgramStatus] = List(Unknown, PendingSync, PendingUpdate, PendingCheckIn, UpToDate )

  def apply(local: VersionMap, remote: VersionMap): ProgramStatus =
    VersionMap.tryCompare(local, remote) match {
      case Some(0)          => UpToDate
      case Some(i) if i < 0 => PendingUpdate
      case Some(i) if i > 0 => PendingCheckIn
      case None             => PendingSync
    }
}

