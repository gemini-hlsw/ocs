package edu.gemini.util.security.policy

import edu.gemini.pot.sp.{SPNodeKey, DataObjectBlob, ISPNode, ISPObservation}
import edu.gemini.spModel.gemini.security.UserRolePrivileges
import edu.gemini.spModel.gemini.security.UserRolePrivileges._
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.obs.ObservationStatus._
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.too.Too

import scalaz._
import Scalaz._


object MergeValidator {
  implicit def pimpObsStatus(s: ObservationStatus) = new {
    def <(o: ObservationStatus) = s.isLessThan(o)
  }

  implicit def pimpObs(o: ISPObservation) = new {
    def isToo: Boolean = Too.isToo(o)
  }

  implicit object ObservationStatusInstances extends Equal[ObservationStatus] {
    def equal(a1: ObservationStatus, a2: ObservationStatus): Boolean = a1 == a2
  }

  implicit object UserRolePrivilegesInstances extends Equal[UserRolePrivileges] {
    def equal(a1: UserRolePrivileges, a2: UserRolePrivileges): Boolean = a1 == a2
  }

  private val LegalEdit = Map(
    PI -> Set(PHASE2),
    NGO -> Set(PHASE2, FOR_REVIEW, IN_REVIEW),
    EXC -> Set(PHASE2, FOR_REVIEW, IN_REVIEW),
    STAFF -> (ObservationStatus.values().toSet - OBSERVED)
  ).withDefaultValue(Set.empty)

  private val LegalSwitch = Map(
    PI -> (LegalEdit(PI) + FOR_REVIEW),
    NGO -> (LegalEdit(NGO) + FOR_ACTIVATION),
    EXC -> (LegalEdit(EXC) + FOR_ACTIVATION),
    STAFF -> (LegalEdit(STAFF) + OBSERVED)
  ).withDefaultValue(Set.empty)
}

/**
 * Validate that we can legally merge the obs with our changes in update.  If
 * you think of the OT getting merging changes from an ODB, the "obs" would be
 * the current database version and the update would be the local, potentially
 * modified version.
 */

case class MergeValidator(privs: UserRolePrivileges, obs: Option[ISPObservation], update: Option[ISPObservation]) {
  // At least one of them exists and if they both exist they have the same key
  assert(obs.orElse(update).isDefined)
  assert(obs.map(_.getNodeKey).forall(key => update.forall(_.getNodeKey.equals(key))))

  import MergeValidator._

  val obsStatus = obs.map(ObservationStatus.computeFor)
  val updateStatus = update.map(ObservationStatus.computeFor)

  private def canEdit(o: ObservationStatus): Boolean = LegalEdit(privs).contains(o)

  private def canSwitchTo(o: ObservationStatus): Boolean = LegalSwitch(privs).contains(o)

  def isValid: Boolean =
    (obs, update) match {
      // Create
      case (None, Some(_)) => updateStatus.exists(canSwitchTo) || isPiTooUpdate

      // Update
      case (Some(o), Some(u)) =>
        isLegalNormalUpdate(o, u) || isPiTooUpdate || isNgoMaskCheck(o, u)

      // Delete
      case (Some(_), None) => obsStatus.exists(canSwitchTo)

      // Unexpected
      case _ => false
    }

  private def isPiTooUpdate: Boolean =
    privs === PI && obs.forall(_.isToo) && update.forall(_.isToo) && {
      // Okay, this is a PI trying to update a TOO observation.
      // In addition to the normal rules, which will have already been
      // checked by this point, the PI can transition between On Hold
      // and Ready.
      def isReadyOrOnHold(s: ObservationStatus) = s === ON_HOLD || s === READY

      // We can only flip between ON_HOLD and READY
      obsStatus.forall(isReadyOrOnHold) && updateStatus.forall(isReadyOrOnHold)
    }

  // NGOs, and of course staff and everything but PI, should be allowed
  // to transition from ON_HOLD to anything below for non-TOO.
  private def isNgoMaskCheck(o: ISPObservation, u: ISPObservation): Boolean =
    privs =/= PI && !o.isToo && !u.isToo &&
      updateStatus.exists(_ < ON_HOLD) && obsStatus.exists(_ === ON_HOLD)

  private def isLegalNormalUpdate(obs: ISPObservation, update: ISPObservation): Boolean = {
    import edu.gemini.pot.sp.version._

    // Set of all node keys in either observation.  We must account for
    // deletions and moves in/out of one observation or the other.
    val allKeys: Set[SPNodeKey] = {
      def keySet(s: Set[SPNodeKey], n: ISPNode): Set[SPNodeKey] =
        ((s + n.getNodeKey) /: n.children) {
          (s0, c) => keySet(s0, c)
        }
      keySet(Set.empty, obs) ++ keySet(Set.empty, update)
    }

    // partition the version map rooted at the given observation based upon
    // whether the key corresponds to an obslog node.
    def partitionVm(ks: Set[SPNodeKey], o: ISPObservation): (VersionMap, VersionMap) = {
      val olKeys = (Option(o.getObsQaLog).toSet ++ Option(o.getObsExecLog).toSet).map(_.getNodeKey)

      // o.getProgram.getVersions.filter(ks.contains) wouldn't add empty
      // nodeVersions for missing keys
      val vm0 = o.getProgram.getVersions
      val vm = ks.toList.map(k => k -> nodeVersions(vm0, k)).toMap

      vm.partition {
        case (k, _) => olKeys.contains(k)
      }
    }

    val (obsLogVm, obsNoLogVm) = partitionVm(allKeys, obs)
    val (updLogVm, updNoLogVm) = partitionVm(allKeys, update)

    // Are the two nodes (and all their children) identical?  This check is used
    // to allow version information to differ for what would otherwise be an
    // illegal edit.  In particular, this is necessary for imports from pre-2014A
    // XML where no version information is present.
    def sameDataObject(n1: ISPNode, n2: ISPNode): Boolean =
      n1.getNodeKey == n2.getNodeKey &&
        DataObjectBlob.same(n1.getDataObject, n2.getDataObject)

    def same(n1: ISPNode, n2: ISPNode): Boolean =
      sameDataObject(n1, n2) &&
        n1.children.size == n2.children.size &&
        n1.children.zip(n2.children).forall {
          case (c1, c2) => same(c1, c2)
        }

    // OCSINF-363
    // Ongoing in the database but still ready in the OT.  Is the observer
    // tweaking the observation before it starts?  If so the local observation
    // will be READY, the remote ONGOING, and the only "edit" in the remote
    // observation will be the addition of the obslog nodes to the observation.
    // This *will* reject changes the observer might make to the observation
    // node itself.
    // TODO: remove when OCSINF-80 (observation:obslog - 1:1) is implemented.
    def observerTweakingOngoingObservation(obsStatus: ObservationStatus, updStatus: ObservationStatus): Boolean =
      (obsStatus == ONGOING) && (updStatus == READY) && canEdit(ONGOING) && {
        val ovm = obsNoLogVm - obs.getNodeKey
        val uvm = updNoLogVm - update.getNodeKey
        VersionMap.tryCompare(ovm, uvm).exists(_ <= 0) && sameDataObject(obs, update)
      }

    // A status change needs to be done with the assurance that nobody has
    // slipped in any changes you haven't seen. For that reason, if the update
    // and the existing observation have both been edited, they must have the
    // same status and they must be the lower "can edit" status.  Otherwise if
    // you have a newer version of the observation you can make a status change
    // so long as you're switching between values you have permission to
    // twiddle in the OT.
    def legalObsEdit = {
      lazy val s1 = ObservationStatus.computeFor(obs)
      lazy val s2 = ObservationStatus.computeFor(update)
      (VersionMap.tryCompare(obsNoLogVm, updNoLogVm) match {
        case Some(i) if i >= 0 => true // "update" same or older
        case Some(i) if i < 0 => canSwitchTo(s1) && canSwitchTo(s2) // "update" newer
        case _ => ((s1 == s2) && canEdit(s1)) || observerTweakingOngoingObservation(s1, s2) // both edited
      }) || same(obs, update)
    }

    def sameObsLog(o1: ISPObservation, o2: ISPObservation): Boolean = {
      def s(f: ISPObservation => ISPNode): Boolean = {
        val n1Opt = Option(f(o1))
        val n2Opt = Option(f(o2))
        (n1Opt.isDefined == n2Opt.isDefined) &&
          n1Opt.forall(n1 => n2Opt.forall(n2 => sameDataObject(n1, n2)))
      }
      s(_.getObsQaLog) && s(_.getObsExecLog)
    }

    def legalObsLogEdit =
      privs === STAFF || VersionMap.tryCompare(obsLogVm, updLogVm).exists(_ >= 0) || sameObsLog(obs, update)

    legalObsEdit && legalObsLogEdit
  }
}