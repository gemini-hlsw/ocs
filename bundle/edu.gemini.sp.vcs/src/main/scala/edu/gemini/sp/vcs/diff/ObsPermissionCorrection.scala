package edu.gemini.sp.vcs.diff

import java.security.Permission

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.pot.sp.version.LifespanId
import edu.gemini.sp.vcs.diff.MergeCorrection._
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.obs.ObservationStatus._
import edu.gemini.spModel.obs.{ObservationStatus, ObservationStatusOrder}
import edu.gemini.spModel.gemini.security.UserRolePrivileges._
import edu.gemini.spModel.gemini.security.{UserRolePrivileges, UserRolePrivilegesEqual}
import edu.gemini.util.security.permission.{PiPermission, NgoPermission, StaffPermission}


// To replace:
// * edu.gemini.util.security.policy.MergeValidator
// * edu.gemini.util.security.policy.ImplicitPolicy that deals with ObsMergePermission
// * edu.gemini.util.security.permission.ObsMergePermission


class ObsPermissionCorrection(lifespanId: LifespanId, pid: SPProgramID, remoteStatus: SPNodeKey => Option[ObservationStatus], localStatus: SPNodeKey => Option[ObservationStatus]) extends CorrectionAction {
  import ObsPermissionCorrection._

  def correct(mp: MergePlan, urps: UserRolePrivileges): MergePlan = ???

  def apply(mp: MergePlan, hasPermission: PermissionCheck): VcsAction[MergePlan] = {
    userRolePrivileges(pid, hasPermission).map(correct(mp, _))
  }
}

object ObsPermissionCorrection {

  private def userRolePrivileges(pid: SPProgramID, hasPermission: PermissionCheck): VcsAction[UserRolePrivileges] = {
    val somePid = Some(pid)

    val perms = List(
      StaffPermission(somePid) -> STAFF,
      NgoPermission(somePid)   -> NGO,
      PiPermission(somePid)    -> PI)

    def go(rem: List[(Permission, UserRolePrivileges)]): VcsAction[UserRolePrivileges] =
      rem match {
        case Nil            => VcsAction(NOUSER)
        case ((p, u) :: ps) => hasPermission(p).flatMap(has => if (has) VcsAction(u) else go(ps))
      }

    go(perms)
  }

  private val LegalEdit = Map(
    PI    -> Set(PHASE2),
    NGO   -> Set(PHASE2, FOR_REVIEW, IN_REVIEW),
    EXC   -> Set(PHASE2, FOR_REVIEW, IN_REVIEW),
    STAFF -> (ObservationStatus.values().toSet - OBSERVED)
  ).withDefaultValue(Set.empty)

  private val LegalSwitch = Map(
    PI    -> (LegalEdit(PI)    + FOR_REVIEW),
    NGO   -> (LegalEdit(NGO)   + FOR_ACTIVATION),
    EXC   -> (LegalEdit(EXC)   + FOR_ACTIVATION),
    STAFF -> (LegalEdit(STAFF) + OBSERVED)
  ).withDefaultValue(Set.empty)

}
