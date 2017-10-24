package edu.gemini.phase2.template.factory.impl.niri

import edu.gemini.spModel.gemini.niri.blueprint.SpNiriBlueprint
import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import scala.collection.JavaConverters._
import edu.gemini.spModel.gemini.niri.Niri.{Filter, Camera}
import edu.gemini.spModel.gemini.niri.Niri.Filter._
import edu.gemini.spModel.gemini.altair.AltairParams.Mode
import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.rich.pot.sp._

case class Niri(blueprint:SpNiriBlueprint) extends NiriBase {
  import blueprint._

  // Local imports
  import Camera.{F6, F14, F32}

  // Some groupings
  private var acq, sci, std, day = Seq.empty[Int]

  // If CAMERA == F/6
  //    INCLUDE {1,2,3,4} IN target-specific Scheduling Group
  //    SCI={1},{2}
  //    STD={3}
  //    DAY={4}

  if (camera == F6) {
    include(1, 2, 3, 4) in TargetGroup
    sci = Seq(1, 2)
    std = Seq(3)
    day = Seq(4)
  }

  // IF CAMERA == F/14
  //    INCLUDE {5,6,7,8} IN target-specific Scheduling Group
  //    SCI={5},{6}
  //    STD={7}
  //    DAY={8}

  if (camera == F14) {
    include(5, 6, 7, 8) in TargetGroup
    sci = Seq(5, 6)
    std = Seq(7)
    day = Seq(8)
  }

  // IF CAMERA = F/32
  //    INCLUDE {9,10,11,12,13} IN target-specific Scheduling Group
  //    ACQ={9}
  //    SCI={10}{11}
  //    STD={12}
  //    DAY={13}

  if (camera == F32) {
    include(9, 10, 11, 12, 13) in TargetGroup
    acq = Seq(9)
    sci = Seq(10, 11)
    std = Seq(12)
    day = Seq(13)
  }

  // # AO Mode
  // # In NGS mode target and standards use the same Altair guide mode.
  // # In LGS mode the target uses the mode from PI, standards use NGS+FieldsLens
  // IF AO mode != None AND NOT DAY ({4}, {8}, {13})
  //     ADD Altair Adaptive Optics component AND
  //     SET Guide Star Type based on:
  //       IF AO in PI includes "Natural Guide Star" (NGS mode) THEN SET
  //       for ALL in the group:
  //         AO=Altair Natural Guidestar => Natural Guide Star
  //         AO=Altair Natural Guidestar w/ Field Lens => Laser Guide Star
  // 	with Field Lens
  //       IF AO in PI includes "Laser Guide Star" (LGS mode) THEN SET for
  //       ACQ and SCI:
  //           AO=Altair Laser Guidestar => Laser Guide Star + AOWFS
  //           AO=Altair Laser Guidestar w/ PWFS1 => Laser Guide Star + PWFS1
  //         AND SET for STD:
  //           SET Guide Star Type=Natural Guide Star with Field Lens

  val excludeFromAltair = Seq(4, 8, 13).map(_.toString)

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
      forObs(acq ++ sci: _*)(
        addAltair(m))
      forObs(std: _*)(
        addAltair(Mode.NGS_FL))
    }

  }


  // # Filters
  // FOR SCI, STD, OBSERVATIONS:
  // SET FILTERS(S) from Phase-I in top-level (first) NIRI iterator.

  forObs(sci ++ std : _*)(setFilters fromPI)

  // FOR DAY OBSERVATIONS:
  // IF WAVE(FILTER) < 3um, SET FILTERS(S) from Phase-I in top-level
  // (first) NIRI iterator.
  // #In DAY observations the any iterators containing the Dark exposure must not
  // iterate over filters.

  val shortFilters = filters.asScala.filter(_.getWavelength < 3.0)
  if (!shortFilters.isEmpty)
    forObs(day : _*)(
      setFilters(shortFilters))

  // FOR ACQ OBSERVATIONS:
  // SET first FILTER from Phase-I in NIRI static component.
  filters.asScala.headOption.foreach { f =>
    forObs(acq:_*)(
      setFilter(f))
  }

  // # Exposure times and coadds
  // FOR SCI observations:
  // SET EXPOSURE TIME and COADDS in Iterator/Static component from
  // spreadsheet NIRI_exptimes.xls.
  // FOR STD and ACQ, DON'T set EXPOSURE TIME, it will be taken from the template.

  forObs(sci: _*)(setExposuresAndCoadds(expMap))

  lazy val expMap:Map[Filter, Exposure] = Map(
    BBF_Y           -> Exposure((60.0, 1), (60.0, 1), (60.0, 1)),
    J_CONTINUUM_106 -> Exposure((120.0, 1), (120.0, 1), (120.0, 1)),
    NBF_HEI         -> Exposure((120.0, 1), (120.0, 1), (120.0, 1)),
    NBF_PAGAMMA     -> Exposure((120.0, 1), (120.0, 1), (120.0, 1)),
    J_CONTINUUM_122 -> Exposure((120.0, 1), (120.0, 1), (120.0, 1)),
    NBF_H           -> Exposure((120.0, 1), (120.0, 1), (120.0, 1)),
    BBF_J           -> Exposure((60.0, 1), (60.0, 1), (60.0, 1)),
    NBF_PABETA      -> Exposure((60.0, 1), (60.0, 1), (60.0, 1)),
    NBF_HCONT       -> Exposure((60.0, 1), (60.0, 1), (60.0, 1)),
    NBF_CH4SHORT    -> Exposure((20.0, 3), (60.0, 1), (60.0, 1)),
    NBF_FEII        -> Exposure((60.0, 1), (60.0, 1), (60.0, 1)),
    BBF_H           -> Exposure((15.0, 4), (60.0, 1), (60.0, 1)),
    NBF_CH4LONG     -> Exposure((20.0, 3), (60.0, 1), (60.0, 1)), // updated
    NBF_H2O_2045    -> Exposure((30.0, 2), (60.0, 1), (60.0, 1)),
    NBF_HE12P2S     -> Exposure((60.0, 1), (60.0, 1), (60.0, 1)),
    NBF_KCONT1      -> Exposure((60.0, 1), (60.0, 1), (60.0, 1)),
    BBF_KPRIME      -> Exposure((30.0, 2), (60.0, 1), (60.0, 1)),
    NBF_H210        -> Exposure((60.0, 1), (60.0, 1), (60.0, 1)),
    BBF_KSHORT      -> Exposure((30.0, 2), (60.0, 1), (60.0, 1)),
    NBF_BRGAMMA     -> Exposure((60.0, 1), (60.0, 1), (60.0, 1)),
    BBF_K           -> Exposure((30.0, 2), (60.0, 1), (60.0, 1)),
    NBF_H221        -> Exposure((60.0, 1), (60.0, 1), (60.0, 1)),
    NBF_KCONT2      -> Exposure((60.0, 1), (60.0, 1), (60.0, 1)),
    NBF_CH4ICE      -> Exposure((60.0, 1), (60.0, 1), (60.0, 1)),
    NBF_CO20        -> Exposure((60.0, 1), (60.0, 1), (60.0, 1)),
    NBF_H2O         -> Exposure((1.0, 30), (5.0, 6), (30.0, 1)),
    NBF_HC          -> Exposure((1.0, 30), (7.0, 4), (30.0, 1)),
    BBF_LPRIME      -> Exposure((-1.0, -1), (0.2, 150), (1, 30)), // updated
    NBF_BRACONT     -> Exposure((0.3, 100), (2.0, 15), (10.0, 3)),
    NBF_BRA         -> Exposure((0.15, 200), (1.0, 30), (5.0, 6)),
    BBF_MPRIME      -> Exposure((-1.0, -1), (0.06, 500), (0.4, 75))
  )

  // # Dark exposures
  // IN DAY OBSERVATIONS:
  // FOR EACH UNIQUE COMBINATION OF EXPOSURE TIME AND COADD in SCI,
  // create a NIRI Sequence with a Manual Dark beneath. The EXPOSURE TIME
  // and COADDs are set in the Dark component. The READ MODE is set in the
  // iterator, see below, based on the exposure time. One iterator is
  // present in the BP libraries, more may be added.

  forObs(day : _*)(createDarkSequences(sci))

  // # Read Mode
  // IN ALL ITERATORS AND STATIC COMPONENTS EXCEPT the FLATS sequences in
  // DAY observations:
  // IF Exptime >= 45s:   SET  Read Mode = Low Background
  // ELSEIF Exptime >= 1.0s:  SET Read Mode = Medium Background
  // ELSE:                 SET Read Mode = High Background

  // *** RCN: we handle this in setExposuresAndCoadds as well as createDarkSequences;
  // *** as these are the only places we set the exposure, we're covered. Everything else
  // *** should be pre-set in the template library.

  // # Well depth
  // IF WAVE (see NIRI_exptimes.xls) > 3micron (equally FILTER(S) includes H20
  // Ice, hydrocarbon, L(prime), Br(alpha) cont, Br(alpha), M(prime)): SET
  // Deep well (3-5 um)
  // ELSE SET Shallow Well (1-2.5 um)  (default)

  forGroup(TargetGroup)(setWellDepth)

  // # DO NOT ADD 'OVERVIEW' NOTE UNTIL WE CAN PUT IT AT THE TOP-LEVEL
  // #Top-level note 'OVERVIEW' should be included in all programs
  //
  // Note: on instantiation, do not add a target component to the STD observation.
  // TODO

}
