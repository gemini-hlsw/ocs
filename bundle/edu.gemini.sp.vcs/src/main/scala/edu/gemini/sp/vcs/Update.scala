package edu.gemini.sp.vcs

import edu.gemini.sp.vcs.VcsLocking.MergeOp
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.pot.sp.{ISPStaffOnlyFieldProtected, ISPProgram}
import edu.gemini.sp.vcs.VcsFailure.TryVcs
import java.security.Principal

/**
 * Update the local program taking any changes from the remote program.  If
 * the local program has made any edits to staff protected fields, they
 * will be reverted.
 */
object Update extends MergeOp[ISPProgram] {

  // This will replace staff-only fields in existing nodes that the user does
  // not have permission to edit to the values in the incoming node.
  import StaffOnlyFieldProtect._
  private def repairStaffOnlyFields(db: IDBDatabaseService, remote: ISPProgram, local: ISPProgram, user: Set[Principal]): Unit =
    if (!canChangeStaffOnlyFields(db, local, remote, user)) {
      protectedNodes(local, remote).foreach { case (localNode, remoteOpt) =>
        val key = localNode.getNodeKey
        val origVv = local.getVersions(key)
        val dataObject = localNode.getDataObject.asInstanceOf[ISPStaffOnlyFieldProtected]
        remoteOpt.fold(dataObject.resetStaffOnlyFieldsToDefaults()) { remoteNode =>
          dataObject.setStaffOnlyFieldsFrom(remoteNode.getDataObject.asInstanceOf[ISPStaffOnlyFieldProtected])
        }
        localNode.setDataObject(dataObject)
        local.setVersions(key, origVv) // as if it never happened....
      }
    }

  def apply(odb: IDBDatabaseService, remote: ISPProgram, local: ISPProgram, user: Set[Principal]): TryVcs[ISPProgram] = {
    repairStaffOnlyFields(odb, remote, local, user)
    Merge(odb, remote, local, user)
  }
}
