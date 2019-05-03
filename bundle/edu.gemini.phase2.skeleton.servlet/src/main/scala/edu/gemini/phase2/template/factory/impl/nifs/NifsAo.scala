package edu.gemini.phase2.template.factory.impl.nifs

import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.nifs.blueprint.SpNifsBlueprintAo

import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.gemini.nifs.NIFSParams
import NIFSParams._
import edu.gemini.spModel.gemini.altair.AltairParams
import AltairParams.Mode._
import edu.gemini.spModel.gemini.altair.AltairParams.Mode
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.rich.pot.sp._

case class NifsAo(blueprint: SpNifsBlueprintAo, exampleTarget: Option[SPTarget]) extends NifsBase[SpNifsBlueprintAo] {
  import blueprint._

  val tb = exampleTarget.flatMap(t => t.getMagnitude(MagnitudeBand.K)).map(_.value).map(TargetBrightness(_))

  // These two notes should be included at the top of every NIFS program
  addNote("Phase II Requirements: General Information", "Phase II  \"BEFORE Submission\" Checklist") in TopLevel

  // # Select acquisition and science observation
  // IF OCCULTING DISK == None
  //   IF target information contains a K magnitude
  //     IF BT  then ACQ={3}  # Bright Object
  //     IF MT  then ACQ={4}  # Medium Object
  //     IF FT  then ACQ={5}  # Faint Object
  //     IF BAT then ACQ={23}  # Blind offset
  //   ELSE
  //     ACQ={3,4,5,23}
  //   SCI={6}
  // ELSEIF OCCULTING DISK != None
  //    IF target information contains a K magnitude
  //      IF BT  then ACQ={11}   # Bright Object
  //      IF MT  then ACQ={12}   # Medium Object
  //      IF FT  then ACQ={12}   # Faint Object
  //      IF BAT then ACQ={12}  # Very faint
  //    ELSE
  //      ACQ={11,12}
  //    SCI={13}

  val (acq, sci) = if (!occultingDisk.isOccultingDisk) {
    (tb.collect {
      case BT  => List(3)
      case MT  => List(4)
      case FT  => List(5)
      case BAT => List(23)
    }.getOrElse(List(3, 4, 5, 23)),
      6)
  } else {
    (tb.collect {
      case BT  => List(11)
      case MT  => List(12)
      case FT  => List(12)
      case BAT => List(12)
    }.getOrElse(List(11, 12)),
      13)
  }

  // ### Target Group
  // INCLUDE {1},{2},ACQ,SCI,{7},{8} in target-specific Scheduling Group
  include(List(1, 2) ++ acq ++ List(sci, 7, 8): _*) in TargetGroup

  // # AO Mode
  // # In NGS mode target and standards use the same Altair guide mode.
  // # In LGS mode the target uses the mode from PI, standards use NGS+FieldsLens
  // IF AO mode != None AND NOT {9} - {10}, {14} - {22}
  //     ADD Altair Adaptive Optics component AND
  //     SET Guide Star Type based on:
  //       IF AO in PI includes "Natural Guide Star" (NGS mode) THEN SET for ALL in the group:
  //         AO=Altair Natural Guidestar => Natural Guide Star
  //         AO=Altair Natural Guidestar w/ Field Lens => Laser Guide Star with Field Lens
  //       IF AO in PI includes "Laser Guide Star" (LGS mode) THEN SET for ACQ and SCI:
  //         AO=Altair Laser Guidestar => Laser Guide Star + AOWFS
  //         AO=Altair Laser Guidestar w/ PWFS1 => Laser Guide Star + PWFS1
  //       AND SET for {1} {2} {7} {8}
  //         SET Guide Star Type=Natural Guide Star with Field Lens
  //
  //     IF OCCULTING DISK != None SET FOCAL PLANE MASK FROM OCCULTING DISK IN PI
  //         Note, for ACQ the FPM is set in the second NIFS
  //           iterator, not in the static component/first NIFS iterator.
  //         None => Do not set, will be Clear or a filter for coronagraphy
  //         0.2" => 0.2 arcsec Occulting Disk
  //         0.5" => 0.5 arcsec Occulting Disk

  val excludeFromAltair = ((9 to 10) ++ (14 to 22)).map(_.toString)

  // HACK: override here to prevent altair going into daytime calibrations
  override def addAltair(m: Mode)(o: ISPObservation): Maybe[Unit] =
    if (o.libraryId.forall(excludeFromAltair.contains)) // true for None ... but it won't happen
      Right(())
    else
      super.addAltair(m)(o)


  altair.mode.foreach { m =>

      if (m.isNGS)
        forGroup(TargetGroup)(
          addAltair(m))

      if (m.isLGS) {
        forObs(sci :: acq : _*)(addAltair(m))
        forObs(1, 2, 7, 8)(
          addAltair(NGS_FL))
      }

      if (occultingDisk.isOccultingDisk) {
        forGroup(TargetGroup)(
          setFpmWithAcq(acq, occultingDisk))
      }

  }

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

  forGroup(TargetGroup)(setDisperser(disperser)) // above logic is in setDisperser

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
  // IF OCCULTING DISK from PI == None
  //       IF DISPERSER = Z THEN INCLUDE {19} in target-specific Scheduling Group
  //       IF DISPERSER = J THEN INCLUDE {18} in target-specific Scheduling Group
  //       IF DISPERSER = H THEN INCLUDE {17} in target-specific Scheduling Group
  //       IF DISPERSER = K THEN INCLUDE {10} in target-specific Scheduling Group
  // ELSE
  //       IF DISPERSER = Z THEN INCLUDE {20} in target-specific Scheduling Group
  //       IF DISPERSER = J THEN INCLUDE {21} in target-specific Scheduling Group
  //       IF DISPERSER = H THEN INCLUDE {22} in target-specific Scheduling Group
  //       IF DISPERSER = K THEN INCLUDE {14} in target-specific Scheduling Group
  //       SET MASK in 2nd NIFS iterator (Lamps on/off Flats x5 WITH Coronagraph IN)
  //          0.2" from PI => 0.2 arcsec Occulting Disk
  //          0.5" from PI => 0.5 arcsec Occulting Disk

  if (!occultingDisk.isOccultingDisk) {
    Option(blueprint.disperser).collect {
      case Disperser.Z => 19
      case Disperser.J => 18
      case Disperser.H => 17
      case Disperser.K => 10
    }.foreach { cal =>
      include(cal) in TargetGroup
    }
  } else {
    Option(blueprint.disperser).collect {
      case Disperser.Z => 20
      case Disperser.J => 21
      case Disperser.H => 22
      case Disperser.K => 14
    }.foreach { cal =>
      include(cal) in TargetGroup
      forObs(cal)(
        setMaskInSecondIterator(occultingDisk))
    }
  }

  // IF BAT (Blind acquisition target), add an empty User target to the
  // Target Component on instantiation.
  // TODO: not for 2012B

}
