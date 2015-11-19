package edu.gemini.dataman.app

import edu.gemini.dataman.core.DmanId.Obs
import edu.gemini.spModel.dataset.DataflowStatus
import edu.gemini.spModel.dataset.DataflowStatus.{Diverged, SummitOnly, UpdateInProgress, SyncPending}

import scalaz._
import Scalaz._

object ObsRefreshRunnableSpec extends TestSupport {
  "ObsRefreshRunnable" should {
    "find all exepected updates" ! forAllPrograms { (odb, progs) =>

      val expected = allDatasets(progs).filter { ds =>
        DataflowStatus.derive(ds) match {
          case SyncPending | UpdateInProgress | SummitOnly | Diverged => true
          case _                                                      => false
        }
      }.map(_.label.getObservationId).distinct.map(Obs).toSet

      var actual = Set.empty[Obs]
      val orr = new ObsRefreshRunnable(odb, User, oids => actual = oids.toSet)
      orr.run()

      expected == actual
    }
  }
}
