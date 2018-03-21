package edu.gemini.sp.vcs2

import edu.gemini.shared.util.VersionComparison.{Same, Older, Newer, Conflicting}
import edu.gemini.sp.vcs2.MergeCorrection._
import edu.gemini.sp.vcs2.ObsEdit.{ObsDelete, ObsUpdate, ObsCreate}
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.obscomp._
import edu.gemini.spModel.gemini.obscomp.SPProgram.Active
import edu.gemini.spModel.gemini.security._
import edu.gemini.spModel.gemini.security.UserRolePrivileges._
import edu.gemini.spModel.obs._
import edu.gemini.spModel.obs.ObservationStatus._
import edu.gemini.spModel.too._
import edu.gemini.util.security.permission.{PiPermission, NgoPermission, StaffPermission}

import java.security.Permission

import scalaz._
import Scalaz._

// NOTE: extracted from (and will replace):
// edu.gemini.util.security.policy.MergeValidator

/**
 * Evaluates whether a local observation edit should be considered legal.
 */
class ObsEditValidator(privs: UserRolePrivileges, too: TooType, active: Active) {
  import ObsEditValidator._

  def isLegal(edit: ObsEdit): Boolean = {
    def canEdit(o: ObservationStatus): Boolean   = LegalEdit(privs).contains(o)
    def canSwitch(o: ObservationStatus): Boolean = LegalSwitch(privs).contains(o)

    // PIs can transition between ON_HOLD and READY for ToO observations.
    def isPiTooUpdate(local: ObsEdit.Obs, remote: Option[ObsEdit.Obs]): Boolean = {
      def isTooSwitch(os: ObservationStatus): Boolean = os === ON_HOLD || os === READY

      (   privs === PI
       && too =/= TooType.none
       && active === Active.YES
       && isTooSwitch(local.status)
       && remote.forall(o => isTooSwitch(o.status))
      )
    }

    // NGOs, and of course staff and everyone except PI, should be allowed to
    // transition from ON_HOLD to anything below for non-ToO.
    def isNgoMaskCheck(up: ObsUpdate): Boolean =
      (   privs =/= PI
       && too === TooType.none
       && up.local.status < ON_HOLD
       && up.remote.exists(_.status === ON_HOLD)
      )

    // A status change needs to be done with the assurance that nobody has
    // slipped in any changes you haven't seen.  For that reason, if the update
    // and the remote observation have both been edited, they must have the
    // same status and they must be the lower "can edit" status.  Otherwise if
    // you have a newer version of the observation you can make a status change
    // so long as you're switching between values you have permission to
    // twiddle in the OT.
    def normalObsEdit(up: ObsUpdate): Boolean =
      up.comparison.obsOnly match {
        case Same | Older => true
        case Newer        => canSwitch(up.local.status) && up.remote.forall(ro => canSwitch(ro.status))
        case Conflicting  => canEdit(up.local.status) && up.remote.forall(_.status === up.local.status)
      }

    def legalObsLogEdit(up: ObsUpdate): Boolean =
      privs === STAFF || (up.comparison.logOnly match {
        case Same | Older        => true
        case Newer | Conflicting => false
      })

    edit match {
      case ObsCreate(_, lo)           =>
        canSwitch(lo.status) || isPiTooUpdate(lo, None)

      case up@ObsUpdate(_, lo, ro, _) =>
        legalObsLogEdit(up) && (normalObsEdit(up) || isPiTooUpdate(lo, ro) || isNgoMaskCheck(up))

      case ObsDelete(_, ro)           =>
        canSwitch(ro.status)
    }
  }
}


object ObsEditValidator {

  def apply(pid: SPProgramID, hasPermission: PermissionCheck, too: TooType, active: Active): VcsAction[ObsEditValidator] =
    userRolePrivileges(pid, hasPermission).map(new ObsEditValidator(_, too, active))

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

  private[vcs2] val LegalEdit = Map(
    PI    -> Set(PHASE2),
    NGO   -> Set(PHASE2, FOR_REVIEW, IN_REVIEW),
    EXC   -> Set(PHASE2, FOR_REVIEW, IN_REVIEW),
    STAFF -> (ObservationStatus.values().toSet - OBSERVED)
  ).withDefaultValue(Set.empty)

  private[vcs2] val LegalSwitch = Map(
    PI    -> (LegalEdit(PI)    + FOR_REVIEW),
    NGO   -> (LegalEdit(NGO)   + FOR_ACTIVATION),
    EXC   -> (LegalEdit(EXC)   + FOR_ACTIVATION),
    STAFF -> (LegalEdit(STAFF) + OBSERVED)
  ).withDefaultValue(Set.empty)
}
