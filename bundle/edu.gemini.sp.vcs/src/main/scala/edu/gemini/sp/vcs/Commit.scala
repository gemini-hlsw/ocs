package edu.gemini.sp.vcs

import edu.gemini.pot.sp.{ISPStaffOnlyFieldProtected, ISPNode, ISPProgram}
import edu.gemini.pot.sp.version._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.sp.vcs.VcsLocking.MergeOp
import edu.gemini.sp.vcs.OldVcsFailure._
import edu.gemini.spModel.rich.pot.sp._

import scalaz._
import Scalaz._
import java.security.Principal

/**
 * Commit the changes in the remote program to the local program.  The input
 * program may not have conflicts and the version of all nodes must be newer
 * or the same as the existing program.
 */
object Commit extends MergeOp[VersionMap] {
  private def isUpToDate(remote: ISPProgram, local: ISPProgram): Boolean =
    VersionMap.ordering.lteq(local.getVersions, remote.getVersions)

  private def hasConflicts(remote: ISPNode): Boolean =
    if (remote.hasConflicts) true
    else remote.children.exists(hasConflicts)

  // If you try to edit a staff-only field without the right permissions, you
  // need an update (which will revoke those changes)
  import StaffOnlyFieldProtect._
  private def hasValidStaffOnlyFields(db: IDBDatabaseService, remote: ISPProgram, local: ISPProgram, user: Set[Principal]): Boolean =
    if (canChangeStaffOnlyFields(db, remote, local, user)) true
    else {
      protectedNodes(remote, local).forall { case (remoteNode, localOpt) =>
        val dataObject = remoteNode.getDataObject.asInstanceOf[ISPStaffOnlyFieldProtected]
        localOpt.fold(dataObject.staffOnlyFieldsDefaulted) { localNode =>
          dataObject.staffOnlyFieldsEqual(localNode.getDataObject.asInstanceOf[ISPStaffOnlyFieldProtected])
        }
      }
    }

  private def validate(db: IDBDatabaseService, remote: ISPProgram, local: ISPProgram, user: Set[Principal]): TryVcs[Unit] =
    if (!isUpToDate(remote, local)) NeedsUpdate.left
    else if (!hasValidStaffOnlyFields(db, remote, local, user)) NeedsUpdate.left
    else if (hasConflicts(remote)) HasConflict.left
    else ().right

  def apply(odb: IDBDatabaseService, remote: ISPProgram, local: ISPProgram, user: Set[Principal]): TryVcs[VersionMap] = {
    for {
      _ <- validate(odb, remote, local, user)
      _ <- Merge(odb, remote, local, user)
    } yield local.getVersions
  }
}
