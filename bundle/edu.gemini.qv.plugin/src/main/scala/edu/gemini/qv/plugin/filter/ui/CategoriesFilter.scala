package edu.gemini.qv.plugin.filter.ui

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.qpt.shared.sp.Band
import edu.gemini.qv.plugin.filter.core.Filter._
import edu.gemini.qv.plugin.filter.core.{AoUsage, ConfigurationFilter, Filter}
import edu.gemini.qv.plugin.QvContext
import edu.gemini.spModel.core.ProgramType
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.{GmosCommonType, GmosSouthType, GmosNorthType}
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.nici.NICIParams
import edu.gemini.spModel.gemini.nifs.NIFSParams
import edu.gemini.spModel.gemini.niri.Niri
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{SkyBackground, WaterVapor, CloudCover, ImageQuality}
import edu.gemini.spModel.gemini.texes.TexesParams
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.obs.SPObservation.Priority
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.too.TooType

/**
 * UI element used for editing categories.
 * The main differences to the main filter are: there is no immediate reaction to changes in UI filter elements
 * and the default selection for options filters is the empty set while in the main filter the default for
 * the selection is always all values.
 */
class CategoriesFilter(ctx: QvContext, init: Set[Filter]) extends PagedFilter(ctx, init, showAvailableOnly = false, showCounts = false) {

  // "Main"
  protected def defaultMainFilters = Seq(
    ObsId(""),
    RA(),
    Dec(),
    Partners(Set[Partner]()),
    Types(Set[ProgramType]()),
    Ao(Set[AoUsage]()),
    Statuses(Set[ObservationStatus]()),
    Classes(Set[ObsClass]()),
    Instruments(Set[SPComponentType]()),
    TOOs(Set[TooType]())
  )

  // "Conditions"
  protected def defaultConditionFilters = Seq(
    IQs(Set[ImageQuality]()),
    CCs(Set[CloudCover]()),
    WVs(Set[WaterVapor]()),
    SBs(Set[SkyBackground]())
  )

  // "Priorities"
  protected def defaultPriorityFilters = Seq(
    Bands(Set[Band]()),
    Priorities(Set[Priority]())
  )

  // "Configurations" (instrument specific filters)
  protected def defaultConfigFilters = Set[ConfigurationFilter[_]](
    F2.Dispersers(Set[Flamingos2.Disperser]()),
    F2.Filters(Set[Flamingos2.Filter]()),
    F2.FocalPlanes(Set[Flamingos2.FPUnit]()),
    GmosN.Dispersers(Set[GmosNorthType.DisperserNorth]()),
    GmosN.Filters(Set[GmosNorthType.FilterNorth]()),
    GmosN.FocalPlanes(Set[GmosNorthType.FPUnitNorth]()),
    GmosN.CcdManufacturers(Set[GmosCommonType.DetectorManufacturer]()),
    GmosS.Dispersers(Set[GmosSouthType.DisperserSouth]()),
    GmosS.Filters(Set[GmosSouthType.FilterSouth]()),
    GmosS.FocalPlanes(Set[GmosSouthType.FPUnitSouth]()),
    GmosS.CcdManufacturers(Set[GmosCommonType.DetectorManufacturer]()),
    GNIRS.Dispersers(Set[GNIRSParams.Disperser]()),
    GNIRS.Filters(Set[GNIRSParams.Filter]()),
    GNIRS.CrossDispersers(Set[GNIRSParams.CrossDispersed]()),
    GNIRS.Cameras(Set[GNIRSParams.Camera]()),
    GNIRS.FocalPlanes(Set[GNIRSParams.SlitWidth]()),
    GSAOI.Filters(Set[Gsaoi.Filter]()),
    NICI.FocalPlanes(Set[NICIParams.FocalPlaneMask]()),
    NICI.DichroicWheels(Set[NICIParams.DichroicWheel]()),
    NICI.RedFilters(Set[NICIParams.Channel1FW]()),
    NICI.BlueFilters(Set[NICIParams.Channel2FW]()),
    NIFS.Dispersers(Set[NIFSParams.Disperser]()),
    NIFS.Filters(Set[NIFSParams.Filter]()),
    NIFS.Masks(Set[NIFSParams.Mask]()),
    NIRI.Cameras(Set[Niri.Camera]()),
    NIRI.Dispersers(Set[Niri.Disperser]()),
    NIRI.Filters(Set[Niri.Filter]()),
    NIRI.Masks(Set[Niri.Mask]()),
    Texes.Dispersers(Set[TexesParams.Disperser]())
  )


}
