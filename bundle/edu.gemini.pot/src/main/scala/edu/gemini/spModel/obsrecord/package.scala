package edu.gemini.spModel

import edu.gemini.skycalc.Interval
import edu.gemini.spModel.dataset.{DatasetLabel, DatasetQaState}
import edu.gemini.spModel.dataset.DatasetQaState._

package object obsrecord {

  type DatasetInterval = (DatasetLabel, Interval)


  // Add an "isChargeable" method to DatasetQaState
  implicit class DatasetQaStateOps(s: DatasetQaState) {

    def isChargeable: Boolean =
      s match {
        case CHECK | PASS | UNDEFINED => true
        case FAIL | USABLE            => false
        case _                        =>
          sys.error(s"Unexpected DatasetQaState $s")
      }

  }

}
