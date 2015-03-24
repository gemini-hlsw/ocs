package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.version.LifespanId
import edu.gemini.pot.sp.{ISPStaffOnlyFieldProtected, SPNodeKey}
import edu.gemini.shared.util.VersionComparison.Newer
import edu.gemini.sp.vcs.diff.MergeCorrection._
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.rich.pot.sp._

import java.security.Permission

import edu.gemini.util.security.permission.{VisitorPermission, StaffPermission}

import scalaz._
import Scalaz._

/** Corrections required to reset any data object fields to which the user does
  * not have permission.  Some data, for example time accounting information or
  * private notes, may only be edited by staff members. If the user manages to
  * edit these fields either via an OT bug or malicious editing of an exported
  * program, they must be reset when synchronized with the remote database. */
class StaffOnlyFieldCorrection(lifespanId: LifespanId, key: SPNodeKey, pid: SPProgramID, remote: SPNodeKey => Option[Modified]) extends CorrectionAction {

  import StaffOnlyFieldCorrection._

  private def resetFields(mp: MergePlan): MergePlan = {
    def corrected(m: Modified, dob: StaffProtected, correct: StaffProtected => Unit): MergeNode =
      m.copy(nv = m.nv.incr(lifespanId), dob = dob.copy <| correct)

    def correctedNew(m: Modified, dob: StaffProtected): MergeNode =
      if (dob.staffOnlyFieldsDefaulted()) m
      else corrected(m, dob, _.resetStaffOnlyFieldsToDefaults())

    def correctedExisting(m: Modified, dob: StaffProtected, existing: ISPDataObject): MergeNode =
      if (dob.staffOnlyFieldsEqual(existing)) m
      else corrected(m, dob, _.setStaffOnlyFieldsFrom(existing))

    def correctedNode(mn: MergeNode): MergeNode =
      mn match {
        case m@Modified(k, nv, dob: StaffProtected, _) =>
          remote(k).fold(correctedNew(m, dob)) { remoteMod =>
            nv.compare(remoteMod.nv) match {
              case Newer => correctedExisting(m, dob, remoteMod.dob)
              case _     => mn
            }
          }

        case _ => mn
      }

    mp.copy(update = mp.update.map(correctedNode))
  }

  def apply(mp: MergePlan, hasPermission: PermissionCheck): VcsAction[MergePlan] = {
    // Regular staff permission is determined by program contact information. To
    // prevent changing the contact and gaining staff permission we have to check
    // that contacts aren't edited unless you have super-staff permission.
    val contactsMatch = contact(mp.update.rootLabel) == remote(key).flatMap(contact)

    canSetStaffFields(pid, contactsMatch, hasPermission).map { canSet =>
      if (canSet) mp else resetFields(mp)
    }
  }
}

object StaffOnlyFieldCorrection {
  type StaffProtected = ISPDataObject with ISPStaffOnlyFieldProtected

  private def contact(mn: MergeNode): Option[String] = {
    def contact(sp: SPProgram) = Option(sp.getContactPerson).map(_.trim.toLowerCase)

    mn match {
      case Modified(_, _, sp: SPProgram, _) => contact(sp)
      case _                                => none
    }
  }

  private def canSetStaffFields(pid: SPProgramID, contactsMatch: Boolean, hasPermission: PermissionCheck): VcsAction[Boolean] = {
    def eitherPermission(p0: Permission, p1: Permission): VcsAction[Boolean] =
      for {
        b0 <- hasPermission(p0)
        b1 <- hasPermission(p1)
      } yield b0 || b1

    val isStaff = eitherPermission(new StaffPermission(pid), new VisitorPermission(pid))
    val isSuper = eitherPermission(new StaffPermission(), new VisitorPermission())

    for {
      sup   <- isSuper
      staff <- isStaff
    } yield sup || (contactsMatch && staff)
  }

  def apply(mc: MergeContext): StaffOnlyFieldCorrection = {
    val remote = (key: SPNodeKey) => {
      mc.remote.get(key).flatMap { t =>
        t.rootLabel match {
          case m: Modified => Some(m)
          case _           => None
        }
      }
    }

    new StaffOnlyFieldCorrection(mc.local.prog.getLifespanId, mc.local.prog.key, mc.local.prog.getProgramID, remote)
  }
}