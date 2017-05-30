package edu.gemini.qv.plugin.data

import java.util.Date

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.spModel.core.{ProgramType, Semester, Site}
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.obsclass.ObsClass

import scala.concurrent.Future
import scala.swing.event.Event

/** Notify listeners that we are loading data from this data source. */
case object DataSourceRefreshStart extends Event
/** Notify listeners that we are finished loading. */
case class DataSourceRefreshEnd(observations: Set[Obs]) extends Event

/**
 * Base class for data sources that provide observations to the QV tool.
 * Currently the only supported source is the ODB but it might be interesting to hook up QV to other data
 * sources like for example ITAC.
 */
trait DataSource extends ObservationProvider {

  def site: Site

  // starting from the current semester (as defined by current date) create some prev and the next semester
  private val currentSemester = new Semester(site, new Date)
  private val nextSemester = currentSemester.next
  private val prevSemester = currentSemester.prev
  private val pPrevSemester = prevSemester.prev
  private val ppPrevSemester = pPrevSemester.prev
  private val pppPrevSemester = ppPrevSemester.prev

  // set available data
  var includeCompletedPrograms = false
  var includeInactivePrograms = false
  val availableSemesters = Set(pppPrevSemester, ppPrevSemester, pPrevSemester, prevSemester, currentSemester, nextSemester)
  val availableTypes = ProgramType.All.toSet
  val availableClasses = ObsClass.values.toSet - ObsClass.DAY_CAL // All day calibrations are "invalid" because they don't have a condition, ignore them.
  val availableStatuses = ObservationStatus.values.toSet

  // set default selected data
  var selectedSemesters = Set(pPrevSemester, prevSemester, currentSemester, nextSemester)
  var selectedTypes: Set[ProgramType] = Set(ProgramType.Classical, ProgramType.Queue, ProgramType.DirectorsTime, ProgramType.LargeProgram, ProgramType.FastTurnaround)
  var selectedClasses = Set(ObsClass.SCIENCE)
  var selectedStatuses = Set(ObservationStatus.READY, ObservationStatus.ONGOING)

  /**
   * Refresh the data in this observation provider (e.g. by re-reading observations from the ODB).
   * Implementations should initiate a series of events which listeners can use to react appropriately.
   */
  def refresh: Future[Set[Obs]]

}

object DataSource {

  /** An empty data source. */
  def empty(s: Site): DataSource =
    new DataSource {
      def refresh() = Future.successful(Set.empty)
      def site = s
    }

}