package edu.gemini.sp.vcs.diff

import java.security.Permission

import edu.gemini.pot.sp.{ISPProgram, SPNodeKey}
import edu.gemini.pot.sp.version.{EmptyNodeVersions, NodeVersions, LifespanId}
import edu.gemini.shared.util._
import edu.gemini.shared.util.VersionComparison.{Newer, Conflicting}
import edu.gemini.sp.vcs.diff.MergeCorrection._
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.obs.ObservationStatus._
import edu.gemini.spModel.obs.{ObservationStatus, ObservationStatusOrder}
import edu.gemini.spModel.gemini.security.UserRolePrivileges._
import edu.gemini.spModel.gemini.security.{UserRolePrivileges, UserRolePrivilegesEqual}
import edu.gemini.util.security.permission.{PiPermission, NgoPermission, StaffPermission}

import scala.collection.breakOut

import scalaz._
import Scalaz._

// To replace:
// * edu.gemini.util.security.policy.MergeValidator
// * edu.gemini.util.security.policy.ImplicitPolicy that deals with ObsMergePermission
// * edu.gemini.util.security.permission.ObsMergePermission


class ObsPermissionCorrection(
        local: ISPProgram,
        remoteDiffs: ProgramDiff) extends CorrectionAction {

  import ObsPermissionCorrection._

  def apply(mp: MergePlan, hasPermission: PermissionCheck): VcsAction[MergePlan] = {
    ???
  }

  def correct(mp: MergePlan, privs: UserRolePrivileges): MergePlan = ???
}

object ObsPermissionCorrection {
  def apply(mc: MergeContext): ObsPermissionCorrection =
    new ObsPermissionCorrection(mc.local.prog, mc.remote.diff)

}
