package edu.gemini.sp.vcs.diff

import edu.gemini.sp.vcs.diff.MergeCorrection._
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.security.UserRolePrivileges
import edu.gemini.spModel.gemini.security.UserRolePrivileges._
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.obs.ObservationStatus._
import edu.gemini.util.security.permission.{PiPermission, NgoPermission, StaffPermission}

import java.security.Permission

import ObsEditValidator._

class ObsEditValidator(privs: UserRolePrivileges) {

  def canEdit(o: ObservationStatus): Boolean   = LegalEdit(privs).contains(o)
  def canSwitch(o: ObservationStatus): Boolean = LegalSwitch(privs).contains(o)

}


object ObsEditValidator {

  def apply(pid: SPProgramID, hasPermission: PermissionCheck): VcsAction[ObsEditValidator] =
    userRolePrivileges(pid, hasPermission).map(new ObsEditValidator(_))

  def userRolePrivileges(pid: SPProgramID, hasPermission: PermissionCheck): VcsAction[UserRolePrivileges] = {
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
