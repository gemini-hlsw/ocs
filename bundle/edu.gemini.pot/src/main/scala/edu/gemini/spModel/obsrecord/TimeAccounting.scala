package edu.gemini.spModel.obsrecord

import edu.gemini.spModel.dataset.{ DatasetLabel, DatasetQaState }
import edu.gemini.spModel.event.ObsExecEvent
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.syntax.all._

import scala.collection.JavaConverters._

/**
 * Time accounting calculation based on observation events.
 */
object TimeAccounting {

  def calc(
    events: Vector[ObsExecEvent],
    qa:     DatasetLabel => DatasetQaState,
    oc:     DatasetLabel => ObsClass
  ): VisitTimes = {

    val calc = for {
      h <- events.headOption
      s <- h.site
      c <- VisitCalculator.all.find(_.validAt(s).isBefore(h.instant))
    } yield c

    calc.fold(new VisitTimes()) { _.calc(events, qa, oc) }
  }

  def calcAsJava(
    events: java.util.List[ObsExecEvent],
    qa:     ObsQaRecord,
    store:  ConfigStore
  ): VisitTimes =
    calc(events.asScala.toVector, qa.qaState, store.getObsClass)

}