package edu.gemini.spModel.gemini.seqcomp

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.shared.util.immutable.{Some => GSome}
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.dataset.{Dataset, DatasetLabel}
import edu.gemini.spModel.event.{EndDatasetEvent, ObsExecEvent, StartDatasetEvent}
import edu.gemini.spModel.gemini.calunit.smartgcal.{Calibration, CalibrationProviderHolder}
import edu.gemini.spModel.obslog.ObsExecLog
import edu.gemini.spModel.test.SpModelTestBase


abstract class GcalExecTest extends SpModelTestBase {
  val progId = SPProgramID.toProgramID("GS-3000A-Q-1")
  val obsId  = new SPObservationID(progId, 1)

  def label(num: Int): DatasetLabel    = new DatasetLabel(obsId, num)
  def dataset(num: Int): Dataset       = new Dataset(label(num), "file%d".format(num), System.currentTimeMillis())

  def startEvt(num: Int): ObsExecEvent = new StartDatasetEvent(System.currentTimeMillis(), dataset(num))
  def endEvt(num: Int): ObsExecEvent   = new EndDatasetEvent(System.currentTimeMillis(), label(num))

  def exec(step: Int): Unit = {
    exec(step, startEvt)
    exec(step, endEvt)
  }

  private def exec(step: Int, evt: Int => ObsExecEvent): Unit = {
    ObsExecLog.updateObsLog(getOdb, getObs.getObservationID, new GSome(label(step)), new GSome(evt(step)))
  }

  def setupGcal(cs: Calibration*): Unit =
    CalibrationProviderHolder.setProvider(new TestCalibrationProvider(cs.toList))
}
