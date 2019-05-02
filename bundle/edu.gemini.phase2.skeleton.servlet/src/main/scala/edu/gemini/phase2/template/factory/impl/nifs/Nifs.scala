package edu.gemini.phase2.template.factory.impl.nifs

import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.nifs.blueprint.SpNifsBlueprint

import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.gemini.nifs.NIFSParams
import NIFSParams._

case class Nifs(blueprint:SpNifsBlueprint, exampleTarget: Option[SPTarget]) extends NifsBase[SpNifsBlueprint] {
  import blueprint._

  // N.B. This is the same as NifsAo but without altair or occulting disk

  val tb = exampleTarget.flatMap(t => t.getMagnitude(MagnitudeBand.K)).map(_.value).map(TargetBrightness(_))

  // These two notes should be included at the top of every NIFS program
  addNote("Phase II Requirements: General Information", "Phase II  \"BEFORE Submission\" Checklist") in TopLevel

  // # Select acquisition and science observation
  //   IF target information contains a K magnitude
  //     IF BT  then ACQ={3}  # Bright Object
  //     IF MT  then ACQ={4}  # Medium Object
  //     IF FT  then ACQ={5}  # Faint Object
  //     IF BAT then ACQ={23}  # Blind offset
  //   ELSE
  //     ACQ={2,3,4,5}
  //   SCI={6}

  val (acq, sci) =
    (tb.collect {
      case BT  => List(3)
      case MT  => List(4)
      case FT  => List(5)
      case BAT => List(23)
    }.getOrElse(List(3, 4, 5, 23)),
      6)

  // ### Target Group
  // INCLUDE {1},{2},ACQ,SCI,{7},{8} in target-specific Scheduling Group

  include(List(1, 2) ++ acq ++ List(sci, 7, 8): _*) in TargetGroup

  // # Disperser
  //   SET DISPERSER from PI (all observations)
  //   IF DISPERSER = Z
  //         SET FILTER = ZJ (Same as Disperser displayed in the OT)
  //         SET CENTRAL WAVELENGTH = 1.05
  //   IF DISPERSER = J
  //         SET FILTER = ZJ (Same as Disperser displayed in the OT)
  //         SET CENTRAL WAVELENGTH = 1.25
  //   IF DISPERSER = H
  //         SET FILTER = JH (Same as Disperser displayed in the OT)
  //         SET CENTRAL WAVELENGTH = 1.65
  //   IF DISPERSER = K
  //         SET FILTER = HK (Same as Disperser displayed in the OT)
  //         SET CENTRAL WAVELENGTH = 2.20

  forGroup(TargetGroup)(
    setDisperser(disperser)) // above logic is in setDisperser

  // # Read mode and Exposure time for science observation SCI
  //   FOR OBSERVATION DERIVED FROM SCI
  //      IF BT THEN SET Read Mode = Bright Object, Exposure Time=10
  //      IF MT THEN SET Read Mode = Medium Object, Exposure Time=80
  //      IF FT THEN SET Read Mode = Faint Object, Exposure Time=600

  defaults match {
    case (rm, ex) =>
      forObs(sci)(
        setReadMode(rm),
        setExposure(ex))
  }

  // ### DARKS to match science - taken the morning after science
  // ### observations
  // ### Put one in each template group/target group
  //  IF BT THEN INCLUDE {15} in target-specific Scheduling Group
  //  ELSE IF FT OR BAT THEN INCLUDE {16} in target-specific Scheduling Group
  //  ELSE INCLUDE {9} (MT) in target-specific Scheduling Group
  val dark = tb.collect {
    case BT => 15
    case FT => 16
    case BAT => 16
  }.getOrElse(9)
  include(dark) in TargetGroup

  // ### GENERAL DAYTIME - but taken after science or in the morning
  // ### One goes in each template group/target group
  //       IF DISPERSER = Z THEN INCLUDE {19} in target-specific Scheduling Group
  //       IF DISPERSER = J THEN INCLUDE {18} in target-specific Scheduling Group
  //       IF DISPERSER = H THEN INCLUDE {17} in target-specific Scheduling Group
  //       IF DISPERSER = K THEN INCLUDE {10} in target-specific Scheduling Group

  Option(blueprint.disperser).collect {
    case Disperser.Z => 19
    case Disperser.J => 18
    case Disperser.H => 17
    case Disperser.K => 10
  }.foreach { cal =>
    include(cal) in TargetGroup
  }

  // IF BAT (Blind acquisition target), add an empty User target to the
  // Target Component on instantiation.
  // TODO: not for 2012B
}
