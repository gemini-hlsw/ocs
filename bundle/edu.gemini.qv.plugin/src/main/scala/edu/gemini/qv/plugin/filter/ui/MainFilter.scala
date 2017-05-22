package edu.gemini.qv.plugin.filter.ui

import edu.gemini.qv.plugin.filter.core.Filter._
import edu.gemini.qv.plugin.filter.core._
import edu.gemini.qv.plugin.filter.ui.FilterElement.FilterElementChanged2
import edu.gemini.qv.plugin.QvContext

/**
 * The main filter UI element that is displayed on the left side of the QV window.
 */
class MainFilter(ctx: QvContext, init: Set[Filter]) extends PagedFilter(ctx, init) {

  // main filter is directly coupled to main filter in data provider
  // changes in the main filter elements should be immediately reflected
  reactions += {
    case FilterElementChanged2 => ctx.mainFilter = activeFilter
  }

  // "Main"
  protected def defaultMainFilters = Seq(
    ObsId(),
    ProgPi(),
    ProgContact(),
    RA(),
    Dec(),
    HasNonSidereal(),
    IsActive(),
    IsCompleted(),
    IsRollover(),
    HasTimingConstraints(),
    HasElevationConstraints(),
    HasPreImaging(),
    Semester(),
    Partners(),
    Types(),
    Ao(),
    Statuses(),
    Classes(),
    Instruments(),
    TOOs()
  )

  // "Priorities"
  protected def defaultPriorityFilters = Seq(
    RemainingNights(ctx),
    RemainingHours(ctx),
    RemainingHoursFraction(ctx),
    SetTime(ctx),
    Bands(),
    Priorities()
  )

  // "Conditions"
  protected def defaultConditionFilters = Seq(
    IQs(),
    CCs(),
    WVs(),
    SBs()
  )

  // "Configurations" (instrument specific filters)
  protected def defaultConfigFilters = Set[ConfigurationFilter[_]](
    F2.FocalPlanes(),
    F2.Filters(),
    F2.FocalPlanes(),
    GmosN.Dispersers(),
    GmosN.Filters(),
    GmosN.FocalPlanes(),
    GmosN.CcdManufacturers(),
    GmosS.Dispersers(),
    GmosS.Filters(),
    GmosS.FocalPlanes(),
    GmosS.CcdManufacturers(),
    GNIRS.Dispersers(),
    GNIRS.Filters(),
    GNIRS.CrossDispersers(),
    GNIRS.Cameras(),
    GNIRS.FocalPlanes(),
    GSAOI.Filters(),
    NICI.FocalPlanes(),
    NICI.DichroicWheels(),
    NICI.RedFilters(),
    NICI.BlueFilters(),
    NIFS.Dispersers(),
    NIFS.Filters(),
    NIFS.Masks(),
    NIRI.Cameras(),
    NIRI.Dispersers(),
    NIRI.Filters(),
    NIRI.Masks(),
    Texes.Dispersers()
  )


}
