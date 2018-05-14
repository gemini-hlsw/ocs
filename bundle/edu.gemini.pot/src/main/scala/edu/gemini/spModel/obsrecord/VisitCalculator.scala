package edu.gemini.spModel.obsrecord

import java.time.Instant

import edu.gemini.skycalc.TwilightBoundType.NAUTICAL
import edu.gemini.skycalc.{Interval, ObservingNight, Union}
import edu.gemini.spModel.core.{Semester, Site}
import edu.gemini.spModel.dataset.{ DatasetLabel, DatasetQaState }
import edu.gemini.spModel.dataset.DatasetQaState._
import edu.gemini.spModel.dataset.Implicits._
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
    events: Vector[ObsExecEvent],
    qa:     DatasetLabel => DatasetQaState,
    oc:     DatasetLabel => ObsClass
  ): VisitTimes

}

/**
 * Time accounting calculation based on observation events.
 */
private[obsrecord] object VisitCalculator {

  /**
   * This is a reimplementation of the time accounting algorithm that existed
   * when the world was new.
   */
  case object Primordial extends VisitCalculator {
    def validAt(s: Site): Instant =
      Instant.MIN

    def calc(
      events: Vector[ObsExecEvent],
      qa:     DatasetLabel => DatasetQaState,
      oc:     DatasetLabel => ObsClass
    ): VisitTimes = {

      import impl._

      // The original time accounting code has a bug wherein the portion of a
      // dataset between a start event and an overlap event that comes before
      // its matching dataset end is not charged.  This code replicates that bug
      // so that the time accounting doesn't change for old observations. It
      // does so via datasetIntervals(normal) which will skip the dataset in
      // which the overlap occurs (along with all subsequent datasets).

      val total             = unionOf(events)
      val (normal, overlap) = events.span(!_.isOverlap)
      val chargeable        = dark(events) - unionOf(overlap)
      val charge            = datasetCharge(datasetIntervals(normal), chargeable, qa, oc)

      println(s"total      = $total")
      println(s"normal     = $normal")
      println(s"dark       = ${dark(events)}")
      println(s"overlap    = ${unionOf(overlap)}")
      println(s"chargeable = $chargeable")

      visitTimes(
        total,
        program    = charge(PROGRAM),
        partner    = charge(PARTNER),
        noncharged = charge(NONCHARGED) + (total - chargeable)
      )
    }

  }

  /**
   * The 2018B+ calculator.  The main change from the previous time accounting
   * result is that visits without a passing dataset are not charged at all.  It
   * also simplifies and fixes a number of bugs with the original algorithm:
   *
   * <ul>
   *   <li>Charges for the portion of a dataset after start and before overlap</li>
   *   <li>Handles start/end dataset pairs with intervening events correctly</li>
   *   <li>Doesn't drop time if the first event is start dataset</li>
   * </ul>
   */
  case object Update2018B extends VisitCalculator {
    val semester = new Semester(2018, Semester.Half.B)

    def validAt(s: Site): Instant =
      Instant.ofEpochMilli(semester.getStartDate(s).getTime)

    def calc(
      events: Vector[ObsExecEvent],
      qa:     DatasetLabel => DatasetQaState,
      oc:     DatasetLabel => ObsClass
    ): VisitTimes = {

      import impl._

      val total = unionOf(events)
      val dsets = datasetIntervals(events)

      def isChargeable(di: DatasetInterval): Boolean =
        qa(di._1) match {
          case CHECK | PASS | UNDEFINED => true
          case FAIL | USABLE            => false
          case _                        =>
            sys.error(s"Unexpected QA state for dataset ${di._1}, ${qa(di._1)}")
        }

      def normalCharges: VisitTimes = {
        val overlap    = unionOf(events.dropWhile(!_.isOverlap))
        val chargeable = dark(events) - overlap
        val charge     = datasetCharge(dsets, chargeable, qa, oc)

        visitTimes(
          total,
          program    = charge(PROGRAM),
          partner    = charge(PARTNER),
          noncharged = charge(NONCHARGED) + (total - chargeable)
        )
      }

      if (dsets.exists(isChargeable)) normalCharges
      else VisitTimes.noncharged(total.sum)
    }

  }

  // reverse order by valid time (newest first)
  def all: List[VisitCalculator] =
    List(Update2018B, Primordial)


  // Implementation support functions
  private object impl {

    // Union is fundamentally mutable but we want to treat it as though it were
    // immutable with operations that return new Unions.
    implicit class UnionOps(u: Union[Interval]) {

      def modify(f: Union[Interval] => Unit): Union[Interval] = {
        val res = u.clone()
        f(res)
        res
      }

      def ∩(that: Union[Interval]): Union[Interval] =
        modify(_.intersect(that))

      def +(that: Union[Interval]): Union[Interval] =
        modify(_.add(that))

      def -(that: Union[Interval]): Union[Interval] =
        modify(_.remove(that))

    }

    def intervalOf(es: Vector[ObsExecEvent]): Option[Interval] =
      ^(es.headOption, es.lastOption) { (h, l) => new Interval(h.timestamp, l.timestamp) }

    def unionOf(es: Vector[ObsExecEvent]): Union[Interval] =
      intervalOf(es).map(new Union(_)).getOrElse(new Union())

    /**
     * Computes all the dark time in an ordered list of events.
     */
    def dark(es: Vector[ObsExecEvent]): Union[Interval] = {
      val headNight   = es.headOption.flatMap(_.night)
      val lastNight   = es.lastOption.flatMap(_.night)
      val visitNights = (^(headNight, lastNight) { _ to _ }).toList.flatten
      val darkInts = visitNights.map(_.getDarkTime(NAUTICAL).toInterval)
      new Union(darkInts.asJava) ∩ unionOf(es)
    }

    type DatasetInterval = (DatasetLabel, Interval)

    /**
     * Finds the time required for each dataset (as defined by start/end event
     * pairs).  Dataset time charges depend on the associated observation class
     * and how the dataset is ultimately qualified by QA, so we have to separate
     * the time per dataset.  Because the event stream is not reliable (there may
     * be multiple start events or missing start/end events, etc.) this is not
     * straightforward.  The algorithm below matches the results of the primordial
     * algorithm for picking out the matching pairs.
     */
    def datasetIntervals(es: Vector[ObsExecEvent]): Vector[DatasetInterval] = {
      sealed trait State extends Product with Serializable {
        def result: Vector[DatasetInterval]

        // start is implemented the same way in both States, but placing the
        // implementation here triggers a runtime error:
        //    Duplicate field name&signature in class file edu/gemini/spModel/obsrecord/TimeAccounting$ExpectStart$3
        def start(l: DatasetLabel, start: Long): State
        def end(l: DatasetLabel, end: Long): State
      }

      final case class ExpectStart(result: Vector[DatasetInterval]) extends State {
        def start(l: DatasetLabel, start: Long): State =
          ExpectEnd(l, start, result)

        def end(l: DatasetLabel, end: Long): State =
          this
      }

      final case class ExpectEnd(label: DatasetLabel, start: Long, result: Vector[DatasetInterval]) extends State {
        def start(l: DatasetLabel, start: Long): State =
          ExpectEnd(l, start, result)

        def end(l: DatasetLabel, end: Long): State =
          if (l =/= label) this else ExpectStart(result :+ ((l, new Interval(start, end))))
      }

      (es.foldLeft(ExpectStart(Vector.empty[DatasetInterval]): State) { (state, evt) =>
        evt match {
          case s: StartDatasetEvent => state.start(s.getDataset.getLabel, s.timestamp)
          case e: EndDatasetEvent   => state.end(e.getDatasetLabel, e.timestamp)
          case _                    => state
        }
      }).result
    }

    /**
     * Creates a function that can be used to obtain the Union[Interval]
     * associated with datasets for particular charge classes.  This is the time
     * between start/end dataset events.  Only passing or non-final (check or
     * undefined) are charged.  The charge class to use is the one associated
     * with the observation class of the step.
     */
    def datasetCharge(
      datasets:   Vector[DatasetInterval],
      chargeable: Union[Interval],
      qa:         DatasetLabel => DatasetQaState,
      oc:         DatasetLabel => ObsClass
    ): ChargeClass => Union[Interval] =
      datasets.groupBy { case (label, interval) =>
        val obsClass = oc(label)
        val qaState  = qa(label)
        if (qaState.isFinal && qaState != PASS) NONCHARGED
        else obsClass.getDefaultChargeClass
      }.mapValues(v => new Union(v.map(_._2).asJava) ∩ chargeable)
       .withDefaultValue(new Union())

    /**
     * Converts a collection of categorized Union[Interval] into VisitTimes.
     */
    def visitTimes(
      total:      Union[Interval],
      program:    Union[Interval],
      partner:    Union[Interval],
      noncharged: Union[Interval]
    ): VisitTimes = {
      val vt = new VisitTimes()
      vt.addClassifiedTime(PROGRAM,    program.sum)
      vt.addClassifiedTime(PARTNER,    partner.sum)
      vt.addClassifiedTime(NONCHARGED, noncharged.sum)
      vt.addUnclassifiedTime((total - program - partner - noncharged).sum)
      vt
    }
  }

}