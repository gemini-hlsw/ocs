package edu.gemini.qv.plugin

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.data._
import edu.gemini.qv.plugin.filter.core.Filter
import edu.gemini.qv.plugin.QvContext.{ChartType, HistogramChartType}
import edu.gemini.qv.plugin.selector.TimeRangeSelector.{RangeType, Year}
import edu.gemini.qv.plugin.util.SolutionProvider.ConstraintType
import edu.gemini.qv.plugin.util.{ConstraintsCache, LstTimeZone, ScheduleCache, SolutionProvider}
import edu.gemini.spModel.core.Peer
import edu.gemini.util.skycalc.Night
import java.util.TimeZone

import edu.gemini.shared.util.DateTimeUtils

import scala.concurrent.duration._
import scala.swing.Publisher
import scala.swing.event.Event


object ReferenceDateChanged extends Event

trait TimeChanged extends Event
object TimeRangeChanged extends TimeChanged
object TimeValueChanged extends TimeChanged
object TimeZoneChanged extends TimeChanged

object ConstraintsChanged extends Event

object QvContext {

  sealed trait ChartType
  case object HistogramChartType extends ChartType
  case object TableChartType extends ChartType
  case object BarChartType extends ChartType

}

/**
 * The model that contains all relevant data and information about filters for a set of QV windows.
 * This includes the site and the peer (for services accesses like e.g. Horizons service) and general
 * settings like the non-sidereal reference date etc.
 * @param peer
 */
case class QvContext(peer: Peer, dataSource: DataSource, source: ObservationProvider) extends Publisher {

  def site = peer.site
  def timezone = site.timezone()

  // ===== Data providers / filters used in this context ====
  private lazy val _nonSidProvider = new PositionProvider(this, source)
  private lazy val _mainFilter = FilterProvider(this, _nonSidProvider)
  private lazy val _tableFilter = FilterProvider(this, _mainFilter)
  private lazy val _selection = SelectionProvider(this, _tableFilter)

  def mainFilterProvider = _mainFilter
  def mainFilter_=(f: Filter) = _mainFilter.filter = Some(f)
  def mainFilter_=(f: Option[Filter]) = _mainFilter.filter = f
  def mainFilter: Option[Filter] = _mainFilter.filter

  def tableFilterProvider = _tableFilter
  def tableFilter_=(f: Filter) = _tableFilter.filter = Some(f)
  def tableFilter_=(f: Option[Filter]) = _tableFilter.filter = f
  def tableFilter: Option[Filter] = _tableFilter.filter

  def selectionFilterProvider = _selection
  def selectionFilter_=(f: Filter) = _selection.filter = Some(f)
  def selectionFilter_=(f: Option[Filter]) = _selection.filter = f
  def selectionFilter: Option[Filter] = _selection.filter

  def observations: Set[Obs] = _mainFilter.observations
  def filtered: Set[Obs] = _tableFilter.observations
  def selected: Set[Obs] = _selection.observations

  // =====  currently selected constraints
  private var _selectedConstraints: Set[ConstraintType] = ScheduleCache.Constraints ++ ConstraintsCache.Constraints
  def selectedConstraints_=(constraints: Set[ConstraintType]) = {
    _selectedConstraints = constraints
    publish(ConstraintsChanged)
  }
  def selectedConstraints = _selectedConstraints


  // ===
  // Folded providers
  def foldedMap = FoldedTargetsProvider.observationsMap(dataSource.observations, this)
  def selectedFoldedObs = foldedMap.map({case (f, obs) => if (!obs.intersect(selected).isEmpty) Some(f) else None}).flatten.toSet
  def foldedFilters = FoldedTargetsProvider.filter(dataSource.observations, this)


  // ===
  // subselection origin
  private var _subselectionOrigin: ChartType = HistogramChartType
  def subselectionOrigin = _subselectionOrigin
  def subselectionOrigin_=(origin: ChartType): Unit = _subselectionOrigin = origin

  // ====
  // Non-sidereal objects reference date.
  // This is the date for which RA/Dec positions are displayed for non-sidereal objects in the table.

  private var _referenceDate: Long = Night(site, System.currentTimeMillis()).middleNightTime

  def referenceDate: Long = _referenceDate

  def referenceDate_=(t: Long): Unit = {
    _referenceDate = Night(site, t).middleNightTime
    publish(ReferenceDateChanged)
  }

  // the overall range, this is the range covered by the solution provider
  def nights = SolutionProvider(site).nights
  def range = SolutionProvider(site).range


  // === Time Zone

  private var _timeZone: TimeZone = timezone

  def selectedTimeZone = _timeZone

  def selectedTimeZone_=(tz: TimeZone): Unit = {
    _timeZone = tz
    publish(TimeZoneChanged)
  }

  // === Time Range Type

  private var _rangeType: RangeType = Year

  def rangeType = _rangeType

  def rangeType_=(rt: RangeType): Unit = {
    _rangeType = rt
    publish(TimeRangeChanged)
  }

  // === Time Range Value

  private var _rangeValue: Int = 0

  def rangeValue = _rangeValue

  def rangeValue_=(value: Int): Unit = {
    _rangeValue = value
    publish(TimeValueChanged)
  }

  // === Custom Range

  /** Gets start time of current custom time range. */
  var customStart: Long = DateTimeUtils.startOfDayInMs(System.currentTimeMillis(), timezone.toZoneId)
  /** Gets end time of current custom time range. */
  var customEnd: Long = customStart + 1.day.toMillis


  // === LstTimeZone

  /** Helper method to get appropriate lst time zone. */
  def lstTimezone = LstTimeZone(site)

}
