package edu.gemini.phase2.template.factory.impl.gnirs

import edu.gemini.spModel.gemini.gnirs.blueprint.SpGnirsBlueprintSpectroscopy
import edu.gemini.spModel.target.SPTarget

import edu.gemini.spModel.gemini.gnirs.GNIRSParams._
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.{SlitWidth => FPU, Decker}
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.altair.AltairParams.Mode._
import edu.gemini.spModel.gemini.altair.AltairParams.Mode
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.rich.pot.sp._

case class GnirsSpectroscopy(blueprint:SpGnirsBlueprintSpectroscopy, exampleTarget: Option[SPTarget])
  extends GnirsBase[SpGnirsBlueprintSpectroscopy] {

  // Local imports

  import blueprint._
  import PixelScale.{PS_005, PS_015}
  import Disperser.D_111
  import CrossDispersed.{SXD, LXD}
  import SlitWidth.{PINHOLE_1, PINHOLE_3}
  import WellDepth.DEEP
  import MagnitudeBand.H
  import edu.gemini.spModel.obscomp.InstConstants.EXPOSURE_TIME_PROP

  // Some aliases, just to match the P-Code
  val xd = crossDisperser

  // **** IF INSTRUMENT MODE == SPECTROSCOPY ****
  // #For science, tellurics and daytime calibration.
  // # The ordering of observations in the scheduling group should be:
  //   Notes
  //   Before standard acq
  //   Before standard spec
  //   SCI acq(s)
  //   SCI spec
  //   After standard acq
  //   After standard spec
  //   Daytime pinhole obs (only when CROSS-DISPERSED == SXD OR CROSS-DISPERSED == LXD)
  //
  // INCLUDE {5}, {6}, {12}-{14} in a target-specific Scheduling Group
  //         FOR spec observations: {12}, {6}, {14}
  //             SET CONDITIONS FROM PI
  //             SET PIXEL SCALE FROM PI
  //             SET FPU from PI
  //             SET DISPERSER FROM PI
  //             SET CROSS-DISPERSED FROM PI
  //                 IF CROSS-DISPERSED == SXD OR CROSS-DISPERSED == LXD SET Central Wavelength (FILTER) == Cross-dispersed
  //             SET DISPERSER FROM PI
  // # Ignore central wavelength for 12B
  // #           SET FILTER (== central wavelength) FROM PI #12B not possible to be set automatically.
  // #           IF FILTER FROM PI == L or M (central wavelength > 2.5um) SET Well depth == Deep

  include(5, 6, 12, 13, 14) in TargetGroup

  forObs(12, 6, 14)(
    setPixelScale(pixelScale),
    setFPU(fpu),
    setDisperser(disperser),
    setCrossDispersed(crossDisperser),
    ifTrue(crossDisperser == SXD || crossDisperser == LXD)(
      setFilter(Filter.X_DISPERSED),
      mutateSeq(mapStepsByKey(PARAM_FILTER) {
        case _ => Filter.X_DISPERSED
      })
    ))

  // # Change Offsets for non-cross-dispersed spec observations
  // IF CROSS-DISPERSED == no AND PI Central Wavelength < 2.5um
  //         SET Q-OFFSET to -1, 5, 5, -1 respectively IN ITERATOR CALLED 'ABBA offset pattern' for {12}
  //         SET Q-OFFSET to -5, 1, 1, -5 respectively IN ITERATOR CALLED 'ABBA offset pattern' for {6}, {14}

  if ((crossDisperser == CrossDispersed.NO) && !wavelengthGe2_5) {
    forObs(12)(
      mutateOffsets.withTitle("ABBA offset sequence")(
        setQ(-1, 5, 5, -1)))
    forObs(6, 14)(
      mutateOffsets.withTitle("ABBA offset sequence")(
        setQ(-5, 1, 1, -5)))
  }

  // # ACQ for science to target Scheduling Group
  // IF TARGET H-MAGNITUDE < 7 INCLUDE {22}                   # ND filter
  // IF 7 <= TARGET H-MAGNITUDE < 11.5 INCLUDE {7}            # very bright
  // IF 11.5 <= TARGET H-MAGNITUDE < 16 INCLUDE {8}           # bright
  // IF 16 <= TARGET H-MAGNITUDE < 20 INCLUDE {9}, {11}, {23} # faint, faint extended & re-acquisition
  // IF TARGET H-MAGNITUDE >= 20 INCLUDE {10}, {23}           # blind offset & re-acquisition
  // ELSE INCLUDE {7} - {11}, {22}, {23}                      # no H-magnitude provided so put them all

  val otherAcq = exampleTarget.flatMap(t => t.getMagnitude(H)).map(_.value) match {
    case Some(h) =>
      if (h < 7) Seq(22)
      else if (h < 11.5) Seq(7)
      else if (h < 16.0) Seq(8)
      else if (h < 20.0) Seq(9, 11, 23)
      else Seq(10, 23)
    case None => (7 to 11) ++ Seq(22, 23)
  }
  include(otherAcq:_*) in TargetGroup

  // IF PI Central Wavelength > 2.5um:
  //    SET Well depth == Deep for {5}-{14},{22}
  //    SET Q-OFFSET to -3, 3, 3, -3 respectively IN ITERATOR CALLED 'ABBA offset pattern' for {6},{12},{14} # Science & Tellurics
  if (wavelengthGe2_5) {

    // Update well depth, but only for specific obs if they are already included.
    val included = curIncludes(TargetGroup).toSet
    val all      = Set(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 22)
    forObs((all & included).toSeq: _*)(setWellDepth(DEEP))

    forObs(6,12,14)(
      mutateOffsets.withTitle("ABBA offset sequence")(setQ(-3, 3, 3, -3))
    )
  }

  // #In ALL ACQ
  //        IN acquisition observations: {5}, {7} - {11}, {13}, {23}
  //           SET CONDITIONS FROM PI
  //           SET PIXEL SCALE FROM PI
  //           SET DISPERSER FROM PI
  //           SET CROSS-DISPERSED FROM PI
  //           SET FPU FROM PI IN STATIC COMPONENT AND ITERATORS \
  //                 EXCEPT WHERE FPU == acquisition #in 2nd iterator in ACQ.
  //           IF PIXEL SCALE == 0.05"/pix :
  //                 SET IN FIRST ITERATOR CALLED 'GNIRS: Slit Image' Exposure Time = 15
  //                 IF CROSS-DISPERSED == LXD OR SXD IN ITERATORS SET DECKER = long camera x-disp \
  //                         EXCEPT WHERE FPU == acquisition
  //                 IF CROSS-DISPERSED == None SET DECKER IN ITERATORS = long camera long slit \
  //                         EXCEPT WHERE FPU == acquisition # Second iterator called 'GNIRS: Field images or 'GNIRS: Field images (w/sky offset)'
  //           ELSE IF PIXEL SCALE == 0.15"/pix :
  //                 IF CROSS-DISPERSED == SXD IN ITERATORS SET DECKER = short camera x-disp \
  //                         EXCEPT WHERE FPU == acquisition
  //                 IF CROSS-DISPERSED == NO IN ITERATORS SET DECKER = short camera long slit \
  //                         EXCEPT WHERE FPU == acquisition
  // # Ignore central wavelength for 12B
  // #           SET FILTER (== central wavelength) FROM PI #12B not possible to be set automatically.
  // #           IF FILTER FROM PI == L or M (central wavelength > 2.5um) SET Well depth == Deep

  val acq = Seq(5) ++ otherAcq ++ Seq(13)

  forObs(acq:_*)(

    setPixelScale(pixelScale),
    setDisperser(disperser),
    setCrossDispersed(crossDisperser),

    setFPU(fpu),
    mutateSeq(mapStepsByKey(PARAM_FPU) {
      case FPU.ACQUISITION => FPU.ACQUISITION
      case _ => fpu
    }),

    ifTrue(pixelScale == PS_005)(
      mutateSeq.withTitleIfExists("GNIRS: Slit image")(
        mapSteps(_ + (EXPOSURE_TIME_PROP -> 15.0))),
      ifTrue(xd == SXD || xd == LXD)(
        mutateSeq(
          updateDecker(Decker.LONG_CAM_X_DISP))),
      ifTrue(xd == CrossDispersed.NO)(
        mutateSeq(
          updateDecker(Decker.LONG_CAM_LONG_SLIT)))),

    ifTrue(pixelScale == PS_015)(
      ifTrue(xd == SXD || xd == LXD)(
        mutateSeq(
          updateDecker(Decker.SHORT_CAM_X_DISP))),
      ifTrue(xd == CrossDispersed.NO)(
        mutateSeq(
          updateDecker(Decker.SHORT_CAM_LONG_SLIT)))),

    ifTrue(wavelengthGe2_5)(setWellDepth(DEEP))
  )


  // # AO Mode
  // # In NGS mode target and standards use the same Altair guide mode.
  // # In LGS mode the target uses the mode from PI, standards and daycals use NGS+FieldsLens
  // # An Altair component must not be added to templates derived from {15} (Daytime pinhole below)
  // IF AO mode != None AND NOT {15}
  //     ADD Altair Adaptive Optics component AND SET Guide Star Type based on:
  //       IF AO in PI includes "Natural Guide Star" (NGS mode) THEN SET for ALL in the group:
  //          AO=Altair Natural Guidestar => Natural Guide Star
  //          AO=Altair Natural Guidestar w/ Field Lens => Laser Guide Star with Field Lens
  //       IF AO in PI includes "Laser Guide Star" (LGS mode) THEN:
  //          SET for ACQ and SCI{12}:
  //             AO=Altair Laser Guidestar => Laser Guide Star + AOWFS
  //             AO=Altair Laser Guidestar w/ PWFS1 => Laser Guide Star + PWFS1
  //          SET for {5} {6} {13} {14} (before/after standards):
  //             SET Guide Star Type=Natural Guide Star with Field Lens

  // HACK: override here to prevent altair going into obs 15
  override def addAltair(m: Mode)(o: ISPObservation): Maybe[Unit] =
    if (o.libraryId.forall(_ == 15.toString))
      Right(())
    else
      super.addAltair(m)(o)

  altair.mode.foreach { m =>

    if (m.isNGS)
      forGroup(TargetGroup)(
        addAltair(m))

    if (m.isLGS) {
      forObs(acq ++ Seq(12): _*)(
        addAltair(m))
      forObs(5, 6, 13, 14)(
        addAltair(NGS_FL))
    }

  }

  // #DAYCALS - add to target-specific Scheduling Group
  // IF CROSS-DISPERSED == SXD OR CROSS-DISPERSED == LXD :
  //         INCLUDE {15} in target-specific Scheduling Group and in this:
  //            SET PIXEL SCALE FROM PI
  //                 IF PIXEL SCALE == 0.05"/pix SET FPU = pinhole 0.1
  //                 IF PIXEL SCALE == 0.15"/pix SET FPU = pinhole 0.3
  //            SET DISPERSER FROM PI
  //            SET CROSS-DISPERSED FROM PI

  if (xd == SXD || xd == LXD) {
    include(15) in TargetGroup
    forObs(15)(
      setPixelScale(pixelScale),
      ifTrue(pixelScale == PixelScale.PS_005)(setFPU(PINHOLE_1)),
      ifTrue(pixelScale == PixelScale.PS_015)(setFPU(PINHOLE_3)),
      setDisperser(disperser),
      setCrossDispersed(crossDisperser))
  }

  // #Notes to add to target Scheduling Group
  // In ALL Scheduling group add "ACQ README" note
  // IF DISPERSER == 111 l/mm ADD NOTE1 in target Scheduling Group
  // IF CROSS-DISPERSED == SXD OR CROSS-DISPERSED == LXD:
  //           ADD NOTE2 in target Scheduling Group
  //           ADD NOTE3 in target Scheduling Group

  addNote("ACQ README") in TargetGroup
  if (disperser == D_111)
    addNote("NOTE1: Calibrations for high spectral resolution observations") in TargetGroup
  if (xd == SXD || xd == LXD) {
    addNote("NOTE2: Bad pixels in XD mode") in TargetGroup
    addNote("NOTE3: Differential refraction and XD mode") in TargetGroup
  }
}
