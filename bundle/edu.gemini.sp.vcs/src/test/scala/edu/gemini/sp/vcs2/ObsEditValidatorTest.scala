package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.{Conflicts, SPNodeKey}
import edu.gemini.pot.sp.version._
import edu.gemini.shared.util.VersionComparison
import edu.gemini.shared.util.VersionComparison._
import edu.gemini.sp.vcs2.ObsEdit.{ObsDelete, Comparison, ObsUpdate, ObsCreate}
import edu.gemini.spModel.gemini.obscomp.SPProgram.Active
import edu.gemini.spModel.gemini.security.UserRolePrivileges
import edu.gemini.spModel.gemini.security.UserRolePrivileges._
import edu.gemini.spModel.obs.{SPObservation, ObservationStatus}
import edu.gemini.spModel.obs.ObservationStatus._
import edu.gemini.spModel.too.TooType
import org.scalatest.{FlatSpec, Matchers}

import scalaz._
import Scalaz._

class ObsEditValidatorTest extends FlatSpec with Matchers {
  import ObsEditValidatorTest._

  "All valid updates" should "pass" in {
    every(validTestCase) shouldBe 'Valid
  }

  "All invalid updates" should "fail" in {
    every(allCombinations &~ validTestCase) shouldBe 'Invalid
  }
}

object ObsEditValidatorTest {

  case class TestCase(privs: UserRolePrivileges, too: TooType, active: Active, edit: ObsEdit) {
    def isValid: Boolean = new ObsEditValidator(privs, too, active).isLegal(edit)
    def isInvalid: Boolean = !isValid
  }

  val Key           = new SPNodeKey()
  val DataObject    = new SPObservation()
  val ObsTree       = MergeNode.modified(Key, EmptyNodeVersions, DataObject, NodeDetail.Obs(1), Conflicts.EMPTY).node()
  val AllUrps       = Set(PI, NGO, STAFF)
  val AllToo        = TooType.values.toSet[TooType]
  val AllStat       = ObservationStatus.values.toSet[ObservationStatus]

  val AllComps: Set[VersionComparison]         = Set(Conflicting, Newer, Older, Same)
  val NonStaffLogComps: Set[VersionComparison] = Set(Older, Same)
  def logComps(urps: UserRolePrivileges): Set[VersionComparison] =
    if (urps == STAFF) AllComps else NonStaffLogComps

    // Exhaustive listing of all legal edits
  val validTestCase: Set[TestCase] =
    piTooUpdate ++
    maskCheck ++
    normalCreate ++
    normalDelete ++
    notEditedRemoteDelete ++
    notEdited ++
    newerRemoteDelete ++
    newer ++
    conflictingRemoteDelete ++
    conflicting

  // All possible edit combinations, both valid and invalid.
  val allCombinations: Set[TestCase] =
    allCreate ++ allDelete ++ allUpdateDeleted ++ allUpdate ++ piTooUpdateNotActive

  def mkObs(os: ObservationStatus): ObsEdit.Obs = ObsEdit.Obs(os, ObsTree)

  def piTooUpdate: Set[TestCase] =
    (for {
      too     <- Set(TooType.rapid, TooType.standard)
      stat1   <- Set(ON_HOLD, READY)
      stat2   <- Set(ON_HOLD, READY)
      obsComp <- AllComps
      logComp <- NonStaffLogComps
    } yield {
      Set(
        TestCase(PI, too, Active.YES, ObsCreate(Key, mkObs(stat1))),
        TestCase(PI, too, Active.YES, ObsUpdate(Key, mkObs(stat1), None, Comparison(obsComp, logComp))),
        TestCase(PI, too, Active.YES, ObsUpdate(Key, mkObs(stat1), Some(mkObs(stat2)), Comparison(obsComp, logComp)))
      )
    }).flatten

  def piTooUpdateNotActive: Set[TestCase] =
    (for {
      too     <- Set(TooType.rapid, TooType.standard)
      stat1   <- Set(ON_HOLD, READY)
      stat2   <- Set(ON_HOLD, READY)
    } yield {
      Set(
        TestCase(PI, too, Active.NO, ObsCreate(Key, mkObs(stat1))),
        TestCase(PI, too, Active.NO, ObsUpdate(Key, mkObs(stat1), None, Comparison(Newer, Same))),
        TestCase(PI, too, Active.NO, ObsUpdate(Key, mkObs(stat1), Some(mkObs(stat2)), Comparison(Newer, Same)))
      )
    }).flatten

  def maskCheck: Set[TestCase] =
    for {
      urp     <- Set(NGO, STAFF)
      stat1   <- Set(PHASE2, FOR_REVIEW, IN_REVIEW, FOR_ACTIVATION)
      obsComp <- AllComps
      logComp <- logComps(urp)
    } yield TestCase(urp, TooType.none, Active.YES, ObsUpdate(Key, mkObs(stat1), Some(mkObs(ON_HOLD)), Comparison(obsComp, logComp)))

  def normalCreate: Set[TestCase] =
    for {
      urp  <- AllUrps
      too  <- AllToo
      stat <- ObsEditValidator.LegalSwitch(urp)
    } yield TestCase(urp, too, Active.YES, ObsCreate(Key, mkObs(stat)))

  def normalDelete: Set[TestCase] =
    for {
      urp  <- AllUrps
      too  <- AllToo
      stat <- ObsEditValidator.LegalSwitch(urp)
    } yield TestCase(urp, too, Active.YES, ObsDelete(Key, mkObs(stat)))

  def notEditedRemoteDelete: Set[TestCase] =
    for {
      urp     <- AllUrps
      too     <- AllToo
      stat    <- AllStat
      obsComp <- Set(Same, Older)
      logComp <- logComps(urp)
    } yield TestCase(urp, too, Active.YES, ObsUpdate(Key, mkObs(stat), None, Comparison(obsComp, logComp)))

  def notEdited: Set[TestCase] =
    for {
      urp     <- AllUrps
      too     <- AllToo
      stat1   <- AllStat
      stat2   <- AllStat
      obsComp <- Set(Same, Older)
      logComp <- logComps(urp)
    } yield TestCase(urp, too, Active.YES, ObsUpdate(Key, mkObs(stat1), Some(mkObs(stat2)), Comparison(obsComp, logComp)))

  def newerRemoteDelete: Set[TestCase] =
    for {
      urp     <- AllUrps
      too     <- AllToo
      stat    <- ObsEditValidator.LegalSwitch(urp)
      logComp <- logComps(urp)
    } yield TestCase(urp, too, Active.YES, ObsUpdate(Key, mkObs(stat), None, Comparison(Newer, logComp)))

  def newer: Set[TestCase] =
    for {
      urp     <- AllUrps
      too     <- AllToo
      stat1   <- ObsEditValidator.LegalSwitch(urp)
      stat2   <- ObsEditValidator.LegalSwitch(urp)
      logComp <- logComps(urp)
    } yield TestCase(urp, too, Active.YES, ObsUpdate(Key, mkObs(stat1), Some(mkObs(stat2)), Comparison(Newer, logComp)))

  def conflictingRemoteDelete: Set[TestCase] =
    for {
      urp     <- AllUrps
      too     <- AllToo
      stat    <- ObsEditValidator.LegalEdit(urp)
      logComp <- logComps(urp)
    } yield TestCase(urp, too, Active.YES, ObsUpdate(Key, mkObs(stat), None, Comparison(Conflicting, logComp)))

  def conflicting: Set[TestCase] =
    for {
      urp     <- AllUrps
      too     <- AllToo
      stat    <- ObsEditValidator.LegalEdit(urp)
      logComp <- logComps(urp)
    } yield TestCase(urp, too, Active.YES, ObsUpdate(Key, mkObs(stat), Some(mkObs(stat)), Comparison(Conflicting, logComp)))

  def allCreate: Set[TestCase] =
    for {
      urp  <- AllUrps
      too  <- AllToo
      stat <- AllStat
    } yield TestCase(urp, too, Active.YES, ObsCreate(Key, mkObs(stat)))

  def allDelete: Set[TestCase] =
    for {
      urp  <- AllUrps
      too  <- AllToo
      stat <- AllStat
    } yield TestCase(urp, too, Active.YES, ObsDelete(Key, mkObs(stat)))

  def allUpdateDeleted: Set[TestCase] =
    for {
      urp     <- AllUrps
      too     <- AllToo
      stat    <- AllStat
      obsComp <- AllComps
      logComp <- AllComps
    } yield TestCase(urp, too, Active.YES, ObsUpdate(Key, mkObs(stat), None, Comparison(obsComp, logComp)))

  def allUpdate: Set[TestCase] =
    for {
      urp     <- AllUrps
      too     <- AllToo
      lStat   <- AllStat
      rStat   <- AllStat
      obsComp <- AllComps
      logComp <- AllComps
    } yield TestCase(urp, too, Active.YES, ObsUpdate(Key, mkObs(lStat), Some(mkObs(rStat)), Comparison(obsComp, logComp)))
}
