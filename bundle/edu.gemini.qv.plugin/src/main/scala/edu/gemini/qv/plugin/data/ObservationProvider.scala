package edu.gemini.qv.plugin.data

import edu.gemini.qpt.shared.sp.{Prog, Obs}
import edu.gemini.qv.plugin.filter.core.Filter
import edu.gemini.qv.plugin.{ReferenceDateChanged, QvContext}
import edu.gemini.qv.plugin.util.ConstraintsCache.ConstraintCalculationEnd
import edu.gemini.qv.plugin.util.{SolutionProvider, NonSiderealCache}

import scala.collection.JavaConversions._
import scala.swing.event.Event
import scala.swing.{Swing, Publisher}

import scalaz._, Scalaz._

/** The observations data changed. */
object DataChanged extends Event
object ForceRepaint extends Event
case class FilterChanged(source: ObservationProvider, added: Set[Obs], removed: Set[Obs]) extends Event

trait FilteringObservationProvider extends ObservationProvider {

  def base: ObservationProvider
  def emptyFilterSelection: Set[Obs]

  protected var _filter: Option[Filter] = None
  private var _filtered: Set[Obs] = emptyFilterSelection

  override def observations = _filtered

  def filter_=(f: Option[Filter]): Unit = {
    val preChange = _filtered
    _filter = f
    update()
    if (preChange != _filtered) {
      publish(FilterChanged(this, _filtered -- preChange, preChange -- _filtered))
    }
  }
  def filter = _filter

  deafTo(this)
  listenTo(base)
  reactions += {
    case e =>
      update()
      publish(e)
  }

  protected def update(): Unit = {
    _filtered = _filter.map(f => base.observations.filter(f.predicate)).getOrElse(emptyFilterSelection)
  }

}

case class FilterProvider(base: ObservationProvider) extends FilteringObservationProvider {
  def emptyFilterSelection = base.observations
}

case class SelectionProvider(base: ObservationProvider) extends FilteringObservationProvider {
  def emptyFilterSelection = Set()
}

object FoldedTargetsProvider {

  def filter(base: Set[Obs]): Set[Filter] = {
    base.groupBy(foldedObsKey).map({ case (key, groupedObs) => {
      val headObs = groupedObs.head
      val prog = headObs.getProg
      Filter.ObservationSet(groupedObs + foldedObs(groupedObs), foldedId(prog, groupedObs))
    }}).toSet

  }

  def observations(base: Set[Obs]): Set[Obs] = {
    base.groupBy(foldedObsKey).map({ case (key, groupedObs) => {
      foldedObs(groupedObs)
    }}).toSet
  }

  def observationsMap(base: Set[Obs]): Map[Obs, Set[Obs]] = {
    base.groupBy(foldedObsKey).map({ case (key, groupedObs) => {
      foldedObs(groupedObs) -> groupedObs
    }})
  }

  private def foldedObs(obs: Set[Obs]): Obs = {
    if (obs.size == 1)
      obs.head
    else {
      // fold all observations for a program with same position and instrument and conditions into one
      // artificial observation; use the first observation in the set as the "model" for the combined values,
      // other values are calculated from all values in the set of observations
      val headObs = obs.head
      val prog = headObs.getProg

      new Obs(
        headObs.getProg,
        headObs.getGroup,
        -headObs.getObsNumber,  // careful: equals is defined as (o1.prog==o2.prog && o1.obsNumber==o2.obsNumber) keep unique obs number, use negative value to be sure it's different from any "real" observation!
        foldedId(prog, obs),
        headObs.getTitle,
        headObs.getPriority,
        headObs.getTooPriority,
        headObs.getObsStatus,
        headObs.getObsClass,
        headObs.getTargetEnvironment,
        headObs.getInstruments.map(o => o.getSpType),
        headObs.getOptions,
        headObs.getCustomMask,
        headObs.getCentralWavelength,
        headObs.getSteps,
        headObs.getPiPlannedTime,
        headObs.getExecPlannedTime,
        headObs.getElapsedTime,
        headObs.getSiteQuality,
        headObs.getLGS,
        headObs.getAO,
        headObs.usesMeanParallacticAngle,
        headObs.getAgsAnalysis,
        headObs.getSchedulingBlock
      )
    }
  }

  /**
   * Poor man's comparison/grouping key for site qualities that can be considered as equal when it comes to the
   * constraints in the visibility charts. This are all observations with identical sky brightness, elevation and
   * time window constraints.
   */
  private def foldedObsKey(o: Obs) = {
    val sq = o.getSiteQuality
    (
      o.getProg,
      o.getRa,
      o.getDec,
      o.getInstruments.headOption,
      sq.getSkyBackground,
      sq.getElevationConstraintType,
      sq.getElevationConstraintMin,
      sq.getElevationConstraintMax,
      sq.getTimingWindows.map(_.toString).sorted.mkString(",")
    )
  }

  /** Folded observation id, which shows all observation numbers of all observations in this group. */
  private def foldedId(prog: Prog, obs: Set[Obs]): String =
    prog.getStructuredProgramId.getShortName +
      " [" +
      obs.toSeq.
        sortBy(_.getObsNumber).
        map(_.getObsNumber).mkString(",") +
      "]"
}

/**
 * Observation provider which overrides the RA/Dec coordinates of all non-sidereal observations with
 * their position at the currently selected reference time.
 * @param ctx
 * @param base
 */
case class PositionProvider(ctx: QvContext, base: ObservationProvider) extends ObservationProvider {

  private var _observations: Set[Obs] = Set()

  override def observations = _observations

  deafTo(this)
  listenTo(ctx, base, SolutionProvider(ctx))
  reactions += {
    case DataChanged =>
      updatePos()
      publish(DataChanged)
    case ReferenceDateChanged =>
      updatePos()
      publish(DataChanged)
    case ConstraintCalculationEnd(_, _) =>
      updatePos()
      publish(DataChanged)
  }

  // initial first update on construction
  updatePos()

  private def updatePos(): Unit = {
    _observations = base.observations.map(o => {
      if (NonSiderealCache.isHorizonsTarget(o)) {
        val pos = NonSiderealCache.get(ctx.site, ctx.referenceDate, o)

        // The check above ensures that this is a nonsidereal observation, which means it must have
        // a single-target asterism. If this is not the case it means the model has changed.
        val newTarget = o
          .getTargetEnvironment
          .getAsterism
          .getNonSiderealSpTarget
          .getOrElse(sys.error("The asterism is not a single nonsidereal target."))

        newTarget.setRaDecDegrees(pos.getRaDeg, pos.getDecDeg)
        new Obs(
          o.getProg,
          o.getGroup,
          o.getObsNumber,
          o.getObsId,
          o.getTitle,
          o.getPriority,
          o.getTooPriority,
          o.getObsStatus,
          o.getObsClass,
          o.getTargetEnvironment.setBasePosition(newTarget),
          o.getInstruments.map(o => o.getSpType),
          o.getOptions,
          o.getCustomMask,
          o.getCentralWavelength,
          o.getSteps,
          o.getPiPlannedTime,
          o.getExecPlannedTime,
          o.getElapsedTime,
          o.getSiteQuality,
          o.getLGS,
          o.getAO,
          o.usesMeanParallacticAngle,
          o.getAgsAnalysis,
          o.getSchedulingBlock
        )
      }
      else o
    })
  }
}

/**
 * A provider for observations which serves as a common facade to different sources for observations.
 * Clients can be notified of the available observations change (i.e. due to a refresh, different filter
 * settings etc).
 */
trait ObservationProvider extends Publisher {

  /** Gets the current filter. */
  private var _observations: Set[Obs] = Set()

  /**
   * Sets a new set of observations.
   * @param newObservations
   */
  def observations_= (newObservations: Set[Obs]): Unit = synchronized {
    _observations = newObservations
    // data updates can happen asynchronously, make sure GUI update is synchronized with Swing EDT
    Swing.onEDT(publish(DataChanged))
  }
  def observations = _observations

  /**
   * Gets the values of a given type that are present in the current data.
   * @param collector
   * @tparam A
   * @return
   */
  def presentValues[A](collector: Obs => Set[A]): Set[A] = {
    observations.map(collector).flatten
  }

  /**
   * Gets the values of a given type that are present in the current data including a count.
   * @param collector
   * @tparam A
   * @return
   */
  def presentValuesWithCount[A](collector: Obs => Set[A]): Map[A, Int] = {
    observations.toSeq.map(collector(_).toSeq).flatten.groupBy(v => v).map(s => (s._1, s._2.size))
  }

}
