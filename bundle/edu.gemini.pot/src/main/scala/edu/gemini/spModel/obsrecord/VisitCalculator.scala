package edu.gemini.spModel.obsrecord

import java.time.{ Instant, LocalDateTime, Month }

import edu.gemini.pot.sp.Instrument
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.TwilightBoundType.NAUTICAL
import edu.gemini.skycalc.{Interval, ObservingNight, Union}
import edu.gemini.spModel.core.{Semester, Site}
import edu.gemini.spModel.dataset.{ DatasetLabel, DatasetQaState }
import edu.gemini.spModel.dataset.DatasetQaState._
import edu.gemini.spModel.event.{EndDatasetEvent, ObsExecEvent, OverlapEvent, SlewEvent, StartDatasetEvent}
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.syntax.all._
import edu.gemini.spModel.time.ChargeClass
import edu.gemini.spModel.time.ChargeClass._

import scala.collection.JavaConverters._
import scalaz.Scalaz._
import scalaz._

/**
 * Calculates a VisitTimes for a sequence of events corresponding to a visit.
 * VisitTimes describes how to charge time for the visit.
 */
private[obsrecord] sealed trait VisitCalculator {

  /**
   * Returns the Instant at which the calculator is first considered valid.
   * Calculators are valid starting at a specific time per site.  The
   * calculator that applies is looked up using the timestamp of the first
   * event in the sequence and applies for the entire sequence thereafter.
   */
  def validAt(site: Site): Instant

  /**
   * Calculates the VisitTimes for the given sequence of events.  The QA
   * obs class lookup functions are used for determining how to charge each
   * individual dataset.
   */
  def calc(
    events:     VisitEvents,
    instrument: Option[Instrument],
    obsClass:   ObsClass,
    datasetQa:  DatasetLabel => DatasetQaState,
    datasetOc:  DatasetLabel => ObsClass,
    next:       Option[(VisitTimes, VisitEvents)]  // how the next visit is charged
  ): VisitTimes

}

/**
 * Time accounting calculation based on observation events.
 */
private[obsrecord] object VisitCalculator {

  sealed abstract class SemesterVisitCalculator(semester: Semester) extends VisitCalculator {

    override def validAt(s: Site): Instant =
      Instant.ofEpochMilli(semester.getStartDate(s).getTime)
  }

  /**
   * The primordial visit calculator.  Adapts the old Java version to the
   * VisitCalculator trait.
   */
  case object Primordial extends VisitCalculator {

    implicit class Function1Ops[A, B](f: Function[A, B]) {
      def asJava: java.util.function.Function[A, B] =
        new java.util.function.Function[A, B] {
          override def apply(a: A): B = f(a)
        }
    }

    // This was the initial version.
    override def validAt(s: Site): Instant =
      Instant.MIN

    override def calc(
      events:     VisitEvents,
      instrument: Option[Instrument],  // ignored here
      obsClass:   ObsClass,            // ignored here
      datasetQa:  DatasetLabel => DatasetQaState,
      datasetOc:  DatasetLabel => ObsClass,
      nextVisit:  Option[(VisitTimes, VisitEvents)] // ignored here

    ): VisitTimes =
        PrimordialVisitCalculator.instance.calc(
          events.sorted.toList.asJava,
          datasetQa.asJava,
          datasetOc.asJava
        )

  }

  final class IntervalCharges(
    events:     VisitEvents,
    instrument: Option[Instrument],
    obsClass:   ObsClass,
    datasetQa:  DatasetLabel => DatasetQaState,
    datasetOc:  DatasetLabel => ObsClass
  ) {

    private val total: Union[Interval] =
      events.total

    private val dsets: Vector[DatasetInterval] =
      events.datasetIntervals

    object Conditions {
      def isVisitor: Boolean =
        instrument.exists(_.isVisitor)

      def hasChargeableDataset: Boolean =
        dsets.exists { case (lab, _) => datasetQa(lab).isChargeable }

      // REL-3628: many GPI acquisitions don't produce datasets so charge for
      //           them regardless
      def isGpiAcquisition: Boolean =
        instrument.exists(_ == Instrument.Gpi) && (obsClass == ObsClass.ACQ)

      // REL-3674: slews not being counted because they don't produce data
      def isChargeableSlew(
        nextVisit: Option[(VisitTimes, VisitEvents)]
      ): Boolean = {

        def hasSlew(events: Vector[ObsExecEvent]): Boolean =
          events.exists {
            case s: SlewEvent => true
            case _            => false
          }

        nextVisit.exists {
          case (t, e) =>
            hasSlew(events.sorted) && // this visit has a slew
              !hasSlew(e.sorted)   && // the next visit does not have a slew
              t.getChargedTime > 0    // the next visit is charged
        }

      }
    }

    def always: VisitTimes = {
      val chargeable = events.chargeable

      val charge: ChargeClass => Union[Interval] =
        dsets.groupBy { case (label, interval) =>
          if (datasetQa(label).isChargeable) datasetOc(label).getDefaultChargeClass else NONCHARGED
        }.mapValues(v => new Union(v.map(_._2).asJava) ∩ chargeable)
         .withDefaultValue(new Union())

      visitTimes(
        total,
        program    = charge(PROGRAM),
        partner    = charge(PARTNER),
        noncharged = charge(NONCHARGED) + (total - chargeable)
      )
    }

    def when(f: Conditions.type => Boolean): VisitTimes =
      if (f(Conditions)) always else VisitTimes.noncharged(total.sum)


    // Converts a collection of categorized Union[Interval] into VisitTimes.
    private def visitTimes(
      total:      Union[Interval],
      program:    Union[Interval],
      partner:    Union[Interval],
      noncharged: Union[Interval]
    ): VisitTimes = {
      val vt = new VisitTimes()
      vt.addClassifiedTime(PROGRAM,    program.sum)
      vt.addClassifiedTime(PARTNER,    partner.sum)
      vt.addClassifiedTime(NONCHARGED, noncharged.sum)
      // Unclassified is chargeable time that is not yet explicitly program or
      // partner.  Ultimately this time is added to one or the other according
      // to the observation class of the observation as a whole.
      vt.addUnclassifiedTime((total - program - partner - noncharged).sum)
      vt
    }

  }

  def charges(
    events:     VisitEvents,
    instrument: Option[Instrument],
    obsClass:   ObsClass,
    datasetQa:  DatasetLabel => DatasetQaState,
    datasetOc:  DatasetLabel => ObsClass
  ): IntervalCharges =
    new IntervalCharges(events, instrument, obsClass, datasetQa, datasetOc)

  /**
   * The 2019A calculator.  The main change from the previous time accounting
   * result is that visits without a passing dataset are not charged at all,
   * except for visitor instrument observations which are charged regardless.
   * It also simplifies and fixes a number of bugs with the original algorithm:
   *
   * <ul>
   *   <li>Charges for the portion of a dataset after start and before overlap</li>
   *   <li>Handles start/end dataset pairs with intervening events correctly</li>
   *   <li>Doesn't drop time if the first event is start dataset</li>
   * </ul>
   */
  case object Update2019A extends SemesterVisitCalculator(new Semester(2019, Semester.Half.A)) {

    override def calc(
      events:     VisitEvents,
      instrument: Option[Instrument],
      obsClass:   ObsClass,
      datasetQa:  DatasetLabel => DatasetQaState,
      datasetOc:  DatasetLabel => ObsClass,
      nextVisit:  Option[(VisitTimes, VisitEvents)] // ignored here
    ): VisitTimes =
      charges(events, instrument, obsClass, datasetQa, datasetOc).when { c =>
        c.isVisitor || c.hasChargeableDataset || c.isGpiAcquisition
      }

  }

  /**
   * Updates the 2019A visit calculator to charge for slew visits.  The typical
   * pattern is to slew on the science observation, switch to a special
   * acquisition observation, then switch back to the science observation to
   * collect data.  The initial slew was not being charged.
   */
  case object Update2019B extends SemesterVisitCalculator(new Semester(2019, Semester.Half.B)) {

    override def calc(
      events:     VisitEvents,
      instrument: Option[Instrument],
      obsClass:   ObsClass,
      datasetQa:  DatasetLabel => DatasetQaState,
      datasetOc:  DatasetLabel => ObsClass,
      nextVisit:  Option[(VisitTimes, VisitEvents)]
    ): VisitTimes =
      charges(events, instrument, obsClass, datasetQa, datasetOc).when { c =>
        c.isVisitor              ||
          c.hasChargeableDataset ||
          c.isGpiAcquisition     ||
          c.isChargeableSlew(nextVisit)
      }
  }

  // reverse order by valid time (newest first)
  def all: List[VisitCalculator] =
    List(Update2019B, Update2019A, Primordial)

  /**
   * Finds the `VisitCalculator` that corresponds to the given visit.
   */
  def lookup(e: VisitEvents): VisitCalculator =
    (for {
      h <- e.sorted.headOption
      s <- h.site
      c <- all.find(_.validAt(s).isBefore(h.instant))
    } yield c).getOrElse(all.head)


  /**
   * Calculates the `VisitTimes` corresponding to a collection of visits.
   */
  def calc(
    visits:     List[VisitEvents],
    instrument: Option[Instrument],
    obsClass:   ObsClass,
    datasetQa:  DatasetLabel => DatasetQaState,
    datasetOc:  DatasetLabel => ObsClass
  ): List[VisitTimes] =

    visits.foldRight(List.empty[(VisitTimes, VisitEvents)]) { (visit, result) =>
      val times = lookup(visit).calc(visit, instrument, obsClass, datasetQa, datasetOc, result.headOption)
      (times, visit) :: result
    }.unzip._1


  def calcForJava(
    visits:     java.util.List[Array[ObsExecEvent]],
    instrument: GOption[Instrument],
    obsClass:   ObsClass,
    qa:         ObsQaRecord,
    store:      ConfigStore
  ): java.util.List[VisitTimes] =

    calc(
      visits.asScala.toList.map(es => VisitEvents(es.toVector)),
      instrument.asScalaOpt,
      obsClass,
      qa.qaState,
      store.getObsClass
    ).asJava
}