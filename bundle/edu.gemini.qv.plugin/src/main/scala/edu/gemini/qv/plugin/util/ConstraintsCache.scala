package edu.gemini.qv.plugin.util

import java.time.Instant

import ConstraintsCache._
import edu.gemini.qpt.shared.sp.{Conds, Obs}
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.data.FoldedTargetsProvider
import edu.gemini.qv.plugin.util.ConstraintsCache.ConstraintCalculationEnd
import edu.gemini.qv.plugin.util.ConstraintsCache.ConstraintCalculationProgress
import edu.gemini.qv.plugin.util.ConstraintsCache.ConstraintCalculationStart
import edu.gemini.qv.plugin.util.SolutionProvider.{ConstraintType, ValueType}
import edu.gemini.skycalc.TimeUtils
import edu.gemini.spModel.core.{Coordinates, Peer, Site}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow
import edu.gemini.util.skycalc.calc._
import edu.gemini.util.skycalc.constraint._
import edu.gemini.util.skycalc.Night

import scala.collection.concurrent
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.swing.Swing._
import scala.swing.event.Event
import scala.swing.{Publisher, Swing}


object ConstraintsCache {

  sealed trait CalculationEvent extends Event
  case class ConstraintCalculationStart(constraint: ConstraintType, cnt: Int = 1) extends CalculationEvent
  case class ConstraintCalculationEnd(constraint: ConstraintType, cnt: Int = 1) extends CalculationEvent
  case class ConstraintCalculationProgress(constraint: ConstraintType, cnt: Int = 1) extends CalculationEvent


  sealed trait ConstraintConstraint extends ConstraintType

  object AboveHorizon extends ConstraintConstraint
  object SkyBrightness extends ConstraintConstraint
  object TimingWindows extends ConstraintConstraint
  object Elevation extends ConstraintConstraint
  object MinimumTime extends ConstraintConstraint

  object MinElevation extends ValueType
  object MaxElevation extends ValueType

  val Constraints: Set[ConstraintType] = Set(AboveHorizon, SkyBrightness, TimingWindows, Elevation, MinimumTime)

}

class ConstraintsCache(allNights: Seq[Night]) extends Publisher {

  sealed trait SolutionKey
  case class ObsKey(ct: ConstraintType, obsId: String) extends SolutionKey

  sealed trait ValueKey
  case class ObsValueKey(vt: ValueType, obsId: String) extends ValueKey
  case class AnyValueKey(vt: ValueType) extends ValueKey

  private val solutionMap: concurrent.Map[SolutionKey, Solution] = concurrent.TrieMap()
  private val valueMap: concurrent.Map[ValueKey, Seq[Double]] = concurrent.TrieMap()

  private def add(key: SolutionKey, value: Solution) = {
    solutionMap.put(key, value)
  }

  def value(nights: Seq[Night], valueType: ValueType): Seq[Double] = {
    val key = AnyValueKey(valueType)
    val allValues = valueMap.getOrElse(key, nights.map(_ => 0.0))
    val left  = allNights.indexOf(nights.head)
    val right = allNights.size - allNights.indexOf(nights.last) - 1
    allValues.drop(left).dropRight(right)
  }

  def value(nights: Seq[Night], valueType: ValueType, obs: Obs): Seq[Double] = {
    val key = ObsValueKey(valueType, obs.getObsId)
    val allValues = valueMap.getOrElse(key, nights.map(_ => 0.0))
    val left  = allNights.indexOf(nights.head)
    val right = allNights.size - allNights.indexOf(nights.last) - 1
    allValues.drop(left).dropRight(right)
  }

  def solution(nights: Seq[Night], constraints: Set[ConstraintType], obs: Obs): Solution = {
    require(nights.nonEmpty, "can only produce solutions for at least one night")
    solution(constraints, obs).restrictTo(Interval(nights.head.start, nights.last.end))
  }

  private def solution(constraints: Set[ConstraintType], obs: Obs): Solution =
    constraints.
      filter({
      case c: ConstraintConstraint => true
      case _ => false
    }).
      map(c => solution(c, obs)).
      reduceOption(_ intersect _).
      getOrElse(Solution.Always)

  private def solution(constraint: ConstraintType, obs: Obs): Solution = {
    val key = ObsKey(constraint, obs.getObsId)
    val sOpt = solutionMap.get(key)
    // interesting for debugging, note that if this is called during calcuations are done in the background
    // it is ok when we don't find a solution (not yet calculated); however after calculations are done we
    // should always find a solution
    //    if (sOpt.isEmpty) {
    //      println(s" >> WARN: DID NOT FIND A SOLUTION FOR $constraint FOR ${obs.getObsId}")
    //    }
    sOpt.getOrElse(Solution.Always)
  }


  def clear() = {
    solutionMap.clear()
    valueMap.clear()
  }


  def update(ctx: QvContext, nights: Seq[Night], observations: Set[Obs]): Future[Unit] = Future {
    require(nights.nonEmpty)

    val constraints = Set(AboveHorizon, SkyBrightness, TimingWindows, Elevation)
    val foldedMap = FoldedTargetsProvider.observationsMap(observations, ctx)
    val foldedObs = foldedMap.keys

    onEDT(constraints.foreach(c => {
      publish(ConstraintCalculationStart(c, observations.size * nights.size))
    }))

    // Note: The default execution context is pretty good at keeping the CPU usage at 80% if there are enough
    // tasks around; throwing the semesters and for each semester the folded obs at it seems to work pretty well.

    // force calculation of lazily initialised moon calculator in all nights
    // (it will be needed later and slows UI interaction down too much when only initialised when needed)
    nights.map(_.moonCalculator)

    // do calculations for each "folded" obs, i.e. for each position
    // this can be done in parallel!
    foldedObs.par.foreach(obs => {                                   // work on observation groups in parallel!

      val target = (t: Long) => obs.getTargetEnvironment.getAsterism.basePosition(Some(Instant.ofEpochMilli(t))).getOrElse(Coordinates.zero)
      calculatePosSemester(nights, target, obs, foldedMap(obs))

      // update progress, we've calculated all constraints for obs.size observations for all nights in the semester
      Swing.onEDT({
        constraints.foreach(c => publish(ConstraintCalculationProgress(c, foldedMap(obs).size * nights.size)))
      })

    })

    onEDT(constraints.foreach(c => {
      publish(ConstraintCalculationEnd(c, 0))
    }))

  }


  private def calculatePosSemester(nights: Seq[Night], target: Long => Coordinates, foldedObs: Obs, obs: Set[Obs]): Unit = {

    // calculate all constraints on a per-night basis and then concatenate the results
    val cc = nights.map(n => calculatePosNight(n, target, foldedObs))
    val ah = cc.map(_._1).reduce(_ add _)
    val sb = cc.map(_._2).reduce(_ add _)
    val el = cc.map(_._3).reduce(_ add _)
    val minEl = cc.map(_._4)
    val maxEl = cc.map(_._5)
    // timing windows for an observation are calculated once per semester only, convert windows to immutable sequence
    val tws = Seq(scala.collection.JavaConversions.asScalaBuffer(foldedObs.getTimingWindows):_*)
    val twsSol = TimingWindowConstraint(tws).solve(nights, foldedObs)

    val obsens = if (obs.size > 1) obs + foldedObs else obs
    obsens.foreach(o => {
      add(ObsKey(AboveHorizon, o.getObsId), ah)
      add(ObsKey(SkyBrightness, o.getObsId), sb)
      add(ObsKey(Elevation, o.getObsId), el)
      add(ObsKey(TimingWindows, o.getObsId), twsSol)
      valueMap.put(ObsValueKey(MinElevation, o.getObsId), minEl)
      valueMap.put(ObsValueKey(MaxElevation, o.getObsId), maxEl)
    })

  }

  private def calculatePosNight(night: Night, target: Long => Coordinates, o: Obs): (Solution, Solution, Solution, Double, Double) = {

    import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.ElevationConstraintType._

    // restrict all calculations to time between nautical twilights (science time)
    val bounds = night.scienceTime
    val tc = TargetCalculator(night.site, target, bounds, TimeUtils.minutes(10))

    // return a tuple with all values of interest
    (
      // calculate and return the three constraints for AboveHorizon, SkyBrightness and Elevation
      {
        ElevationConstraint(0, Double.MaxValue, TimeUtils.minutes(3)).solve(bounds, tc)
      },
      {
        val minSb = Conds.getBrightestMagnitude(o.getConditions.getSB)
        SkyBrightnessConstraint(minSb, Double.MaxValue, TimeUtils.minutes(3)).solve(bounds, tc)
      },
      {
        val min = o.getElevationConstraintMin
        val max = o.getElevationConstraintMax
        o.getElevationConstraintType match {
          case NONE       => ElevationConstraint(minElevationFor(night, o), Double.MaxValue, TimeUtils.minutes(3)).solve(bounds, tc)
          case HOUR_ANGLE => HourAngleConstraint(min, max, TimeUtils.minutes(3)).solve(bounds, tc)
          case AIRMASS    => AirmassConstraint(min, max, TimeUtils.minutes(3)).solve(bounds, tc)
        }
      },

      // some additional values that need to be cached..
      tc.minElevation,
      tc.maxElevation
    )
  }

  /**
   * Gets the minimal elevation for an observation to be observable depending on LGS or not.
   * NOTE: This should be done by checking for the actual instrument used by the observation from o.instruments
   * but as part of REL-293 the GeMS component has been removed from the list of instruments in the Obs objects
   * and was replaced by an artificial Canopus component in the QPT; see also ObsQueryFunctor.instrument().
   * Since fixing this properly would need additional work in the QPT this is out of scope for now.
   */
  private def minElevationFor(n: Night, o: Obs) =
    if (o.getLGS && n.site == Site.GS) 45           // lower limit for GeMS (LGS + site = GS): 45 deg
    else if (o.getLGS) 40                           // lower limit for Altair + LGS: 40 deg
    else 30                                         // lower limit for everything else: 30 deg

}

case class TimingWindowConstraint(windows: Seq[TimingWindow]) extends Constraint[AnyRef] {
  protected val solver = DefaultSolver[AnyRef]()

  override def solve(nights: Seq[Night], param: AnyRef): Solution = createSolution(Interval(nights.head.start, nights.last.end))
  override def solve(night: Night, param: AnyRef): Solution = createSolution(night.interval)
  override def solve(interval: Interval, param: AnyRef): Solution = createSolution(interval)

  /* Will never be called because all solve methods are overridden. */
  def metAt(t: Long, dummy: AnyRef): Boolean = createSolution(Interval(t, t+1)).contains(t)

  private def createSolution(interval: Interval): Solution = {
    import TimingWindow._

    if (windows.isEmpty)
      Solution(interval)                 // not restricted by timing windows, always observable in this interval
    else {
      val solutions: Seq[Solution] =
        windows.map(tw => {
          require(tw.getDuration >= -1)                                                                           // sanity checks
          require(tw.getRepeat >= -1)
          if (tw.getDuration == WINDOW_REMAINS_OPEN_FOREVER) {
            Solution(Seq(new Interval(tw.getStart, Long.MaxValue)))                                               // single window, open forever
          } else tw.getRepeat match {
            case REPEAT_NEVER => Solution(Seq(new Interval(tw.getStart, tw.getStart + tw.getDuration)))           // single window
            case REPEAT_FOREVER => Solution(createIntervals(tw.getStart, interval.end, Int.MaxValue, tw, Seq()))  // repeat forever, use interval end as limiter
            case n => Solution(createIntervals(tw.getStart, interval.end, n+1, tw, Seq()))                        // repeat n times
          }
        })
      solutions.
        reduce(_ combine _).    // combine all the different windows, this will simplify overlapping and abutting intervals
        restrictTo(interval)    // only return timing windows which are relevant for this interval
    }
  }

  /** Creates as many intervals as needed, limited either by time or number. */
  private def createIntervals(time: Long, maxTime: Long, remaining: Int, tw: SPSiteQuality.TimingWindow, result: Seq[Interval]): Seq[Interval] = {
    require(tw.getPeriod > 0)
    if (time > maxTime || remaining == 0) result
    else {
      val r = result :+ Interval(time, time + tw.getDuration)
      createIntervals(time + tw.getPeriod, maxTime, remaining - 1, tw, r)
    }
  }

}

