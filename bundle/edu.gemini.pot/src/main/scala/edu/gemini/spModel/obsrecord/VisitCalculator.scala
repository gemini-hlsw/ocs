package edu.gemini.spModel.obsrecord

import java.time.Instant

import edu.gemini.skycalc.TwilightBoundType.NAUTICAL
import edu.gemini.skycalc.{Interval, ObservingNight, Union}
import edu.gemini.spModel.core.{Semester, Site}
import edu.gemini.spModel.dataset.{ DatasetLabel, DatasetQaState }
import edu.gemini.spModel.dataset.DatasetQaState._
import edu.gemini.spModel.event.{EndDatasetEvent, ObsExecEvent, OverlapEvent, StartDatasetEvent}
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
    events: VisitEvents,
    qa:     DatasetLabel => DatasetQaState,
    oc:     DatasetLabel => ObsClass
  ): VisitTimes

}

/**
 * Time accounting calculation based on observation events.
 */
private[obsrecord] object VisitCalculator {

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
      events: VisitEvents,
      qa:     DatasetLabel => DatasetQaState,
      oc:     DatasetLabel => ObsClass
    ): VisitTimes =
      PrimordialVisitCalculator.instance.calc(events.sorted.toList.asJava, qa.asJava, oc.asJava)

  }

  /**
   * The 2019A+ calculator.  The main change from the previous time accounting
   * result is that visits without a passing dataset are not charged at all.  It
   * also simplifies and fixes a number of bugs with the original algorithm:
   *
   * <ul>
   *   <li>Charges for the portion of a dataset after start and before overlap</li>
   *   <li>Handles start/end dataset pairs with intervening events correctly</li>
   *   <li>Doesn't drop time if the first event is start dataset</li>
   * </ul>
   */
  case object Update2019A extends VisitCalculator {
    val semester = new Semester(2018, Semester.Half.B)

    override def validAt(s: Site): Instant =
      Instant.ofEpochMilli(semester.getStartDate(s).getTime)

    override def calc(
      events: VisitEvents,
      qa:     DatasetLabel => DatasetQaState,
      oc:     DatasetLabel => ObsClass
    ): VisitTimes = {

      val total = events.total
      val dsets = events.datasetIntervals

      def normalCharges: VisitTimes = {
        val chargeable = events.chargeable

        val charge: ChargeClass => Union[Interval] =
          dsets.groupBy { case (label, interval) =>
            if (qa(label).isChargeable) oc(label).getDefaultChargeClass else NONCHARGED
          }.mapValues(v => new Union(v.map(_._2).asJava) ∩ chargeable)
           .withDefaultValue(new Union())

        visitTimes(
          total,
          program    = charge(PROGRAM),
          partner    = charge(PARTNER),
          noncharged = charge(NONCHARGED) + (total - chargeable)
        )
      }

      if (dsets.exists { case (lab, _) => qa(lab).isChargeable }) normalCharges
      else VisitTimes.noncharged(total.sum)
    }

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

  // reverse order by valid time (newest first)
  def all: List[VisitCalculator] =
    List(Update2019A, Primordial)

}