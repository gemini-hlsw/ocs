package edu.gemini.spModel.obsrecord

import edu.gemini.skycalc.TwilightBoundType._
import edu.gemini.skycalc.{ Interval, Union }
import edu.gemini.spModel.dataset.DatasetLabel
import edu.gemini.spModel.dataset.Implicits._
import edu.gemini.spModel.event.{EndDatasetEvent, StartDatasetEvent, ObsExecEvent}
import edu.gemini.spModel.syntax.all._

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/**
 * Wraps a vector of ObsExecEvent to provide some convenience methods.
 */
sealed abstract case class VisitEvents(sorted: Vector[ObsExecEvent]) {

  /**
   * Creates an `Interval` that covers the entire time range of the events,
   * assuming there is at least one event.
   */
  def intervalOption: Option[Interval] =
    ^(sorted.headOption, sorted.lastOption) { (h, l) =>
      new Interval(h.timestamp, l.timestamp)
    }

  /**
   * Creates a `Union` with a single large `Interval` covering the entire time
   * range of the events.
   */
  def total: Union[Interval] =
    intervalOption.map(new Union(_)).getOrElse(new Union())

  /**
   * Computes all the dark time in an ordered list of events.
   */
  def dark: Union[Interval] = {
    val headNight   = sorted.headOption.flatMap(_.night)
    val lastNight   = sorted.lastOption.flatMap(_.night)
    val visitNights = (^(headNight, lastNight) { _ to _ }).toList.flatten
    val darkInts    = visitNights.map(_.getDarkTime(NAUTICAL).toInterval)
    new Union(darkInts.asJava) ∩ total
  }

  /**
   * Computes the time range covered by the first overlap event to the end of
   * the events.
   */
  def overlap: Union[Interval] =
    (new VisitEvents(sorted.dropWhile(!_.isOverlap)) {}).total

  /**
   * Computes the potentially chargeable time in a series of events.  We only
   * charge for dark time before the first overlap event.
   */
  def chargeable: Union[Interval] =
    dark - overlap

  // State machine for finding matching dataset intervals in the event vector.
  // Because the event stream is not reliable (there may be multiple start
  // events or missing start/end events, etc.) this is not straightforward.  The
  // algorithm below matches the results of the primordial algorithm for picking
  // out the matching pairs.
  private object dataset {
    sealed trait State extends Product with Serializable {
      def result: Vector[DatasetInterval]

      def start(l: DatasetLabel, start: Long): State

      def end(l: DatasetLabel, end: Long): State
    }

    final case class ExpectStart(result: Vector[DatasetInterval]) extends State {
      def start(l: DatasetLabel, start: Long): State = ExpectEnd(l, start, result)
      def end(l: DatasetLabel, end: Long): State     = this
    }

    final case class ExpectEnd(label: DatasetLabel, start: Long, result: Vector[DatasetInterval]) extends State {
      def start(l: DatasetLabel, start: Long): State = ExpectEnd(l, start, result)

      def end(l: DatasetLabel, end: Long): State =
        if (l =/= label) this
        else ExpectStart(result :+ ((l, new Interval(start, end))))
    }

    val Initial: State =
      ExpectStart(Vector.empty[DatasetInterval])

    def intervals: Vector[DatasetInterval] =
      (sorted.foldLeft(Initial) { (state, evt) =>
        evt match {
          case s: StartDatasetEvent => state.start(s.getDataset.getLabel, s.timestamp)
          case e: EndDatasetEvent   => state.end(e.getDatasetLabel, e.timestamp)
          case _                    => state
        }
      }).result
  }

  /**
   * Finds the time required for each dataset (as defined by start/end event
   * pairs).  Dataset time charges depend on the associated observation class
   * and how the dataset is ultimately qualified by QA, so we have to separate
   * the time per dataset.
   */
  def datasetIntervals: Vector[DatasetInterval] =
    dataset.intervals
}


object VisitEvents {

  def apply(events: ObsExecEvent*): VisitEvents =
    apply(events.toVector)

  def apply(events: Vector[ObsExecEvent]): VisitEvents =
    new VisitEvents(events.sortBy(_.timestamp)) {}

}