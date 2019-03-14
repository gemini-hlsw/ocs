package edu.gemini.spModel.obsrecord

import edu.gemini.pot.sp.Instrument
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.dataset.{ DatasetLabel, DatasetQaState }
import edu.gemini.spModel.event.ObsExecEvent
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.syntax.all._

import scala.collection.JavaConverters._

/**
 * Time accounting calculation based on observation events.
 */
object TimeAccounting {

  /**
   * Calculate VisitTimes for the events in this visit.
   */
  def calc(
    instrument: Option[Instrument],
    obsClass:   ObsClass,
    events:     Vector[ObsExecEvent],
    datasetQa:  DatasetLabel => DatasetQaState,
    datasetOc:  DatasetLabel => ObsClass
  ): VisitTimes = {

    val ve = VisitEvents(events)

    // Find the visit calculator that applies to the events.  We use the start
    // time of the first event for the visit to select the corresponding
    // calculator that is used for the rest of the sequence regardless of when
    // it ends.
    val calculator = for {
      h <- ve.sorted.headOption
      s <- h.site
      c <- VisitCalculator.all.find(_.validAt(s).isBefore(h.instant))
    } yield c

    calculator.fold(new VisitTimes()) { _.calc(instrument, obsClass, ve, datasetQa, datasetOc) }
  }

  /**
   * Calculate VisitTimes for the events in this visit.  This method is provided
   * to facilitate execution from the Java based PrivateVisit class.
   */
  def calcAsJava(
    instrument: GOption[Instrument],
    obsClass:   ObsClass,
    events:     java.util.List[ObsExecEvent],
    qa:         ObsQaRecord,
    store:      ConfigStore
  ): VisitTimes =
    calc(instrument.asScalaOpt, obsClass, events.asScala.toVector, qa.qaState, store.getObsClass)

}