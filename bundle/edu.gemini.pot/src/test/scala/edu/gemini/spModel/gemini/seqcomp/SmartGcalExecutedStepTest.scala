package edu.gemini.spModel.gemini.seqcomp

import edu.gemini.spModel.test.SpModelTestBase
import edu.gemini.spModel.gemini.gnirs.InstGNIRS
import org.junit.{Test, Before}
import org.junit.Assert._
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.gemini.calunit.smartgcal.{Calibration, CalibrationKey, CalibrationProvider, CalibrationProviderHolder}

import collection.JavaConverters._
import edu.gemini.spModel.gemini.calunit.CalUnitParams._
import scala.beans.BeanProperty
import edu.gemini.spModel.config2.ItemKey
import edu.gemini.spModel.obslog.ObsExecLog
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.pot.sp.SPObservationID
import edu.gemini.spModel.dataset.{Dataset, DatasetLabel}
import edu.gemini.spModel.event.{EndDatasetEvent, StartDatasetEvent, ObsExecEvent}

/**
 * Tests that executed steps don't change even if the mapping changes.
 */
class SmartGcalExecutedStepTest extends SpModelTestBase {
  val progId = SPProgramID.toProgramID("GN-6666A-Q-1")
  val obsId  = new SPObservationID(progId, 1)

  val shutterItem  = new ItemKey("calibration:shutter")
  val diffuserItem = new ItemKey("calibration:diffuser")

  def label(num: Int): DatasetLabel = new DatasetLabel(obsId, num)

  def dataset(num: Int): Dataset = new Dataset(label(num), "file%d".format(num), System.currentTimeMillis())
  def startEvt(num: Int): ObsExecEvent = new StartDatasetEvent(System.currentTimeMillis(), dataset(num))
  def endEvt(num: Int): ObsExecEvent = new EndDatasetEvent(System.currentTimeMillis(), label(num))

  @Before override def setUp() {
    super.setUp(progId)

    addObsComponent(InstGNIRS.SP_TYPE)
    addSeqComponent(getObs.getSeqComponent, SeqRepeatSmartGcalObs.Arc.SP_TYPE)
  }

  val cal = CalImpl(Set(Lamp.arcLamps().get(0)), Shutter.OPEN, Filter.ND_10, Diffuser.IR, 1, 1.0, 1, arc = true)


  @Test def testNoExecutedSteps() {
    val cals = List(cal)
    CalibrationProviderHolder.setProvider(new TestCalibrationProvider(cals))
    val cs = ConfigBridge.extractSequence(getObs, null, ConfigValMapInstances.IDENTITY_MAP)

    assertEquals(1, cs.getAllSteps.size)

    val step = cs.getStep(0)
    assertEquals(cal.shutter,  step.getItemValue(shutterItem))
    assertEquals(cal.diffuser, step.getItemValue(diffuserItem))
  }

  private def exec(step: Int) {
    exec(step, startEvt)
    exec(step, endEvt)
  }

  private def exec(step: Int, evt: Int => ObsExecEvent) {
    ObsExecLog.updateObsLog(getOdb, getObs.getObservationID,
      new edu.gemini.shared.util.immutable.Some[DatasetLabel](label(step)),
      new edu.gemini.shared.util.immutable.Some[ObsExecEvent](evt(1)))
  }

  @Test def testExecutedStepDoesNotChange() {
    // Execute the first step with shutter OPEN
    val cals0 = List(cal)
    CalibrationProviderHolder.setProvider(new TestCalibrationProvider(cals0))
    exec(1)

    // Change the calibration mapping to shutter CLOSED
    val cals1 = List(cal.copy(shutter = Shutter.CLOSED))
    CalibrationProviderHolder.setProvider(new TestCalibrationProvider(cals1))

    // Generate the sequence.
    val cs = ConfigBridge.extractSequence(getObs, null, ConfigValMapInstances.IDENTITY_MAP)

    assertEquals(1, cs.getAllSteps.size)

    val step = cs.getStep(0)

    // Still Open because it was that way when we recorded the step executed
    assertEquals(Shutter.OPEN, step.getItemValue(shutterItem))
  }

  @Test def testUnexecutedStepAfterMappingChange() {
    // Add another smart cal to the sequence so that we have two
    addSeqComponent(getObs.getSeqComponent, SeqRepeatSmartGcalObs.Arc.SP_TYPE)

    // Execute the first step with shutter OPEN
    val cals0 = List(cal)
    CalibrationProviderHolder.setProvider(new TestCalibrationProvider(cals0))
    exec(1)

    // Change the calibration mapping to shutter CLOSED
    val cals1 = List(cal.copy(shutter = Shutter.CLOSED))
    CalibrationProviderHolder.setProvider(new TestCalibrationProvider(cals1))


    // Generate the sequence.
    val cs = ConfigBridge.extractSequence(getObs, null, ConfigValMapInstances.IDENTITY_MAP)

    assertEquals(2, cs.getAllSteps.size)

    val step0 = cs.getStep(0)
    val step1 = cs.getStep(1)

    // Still Open because it was that way when we recorded the step executed
    assertEquals(Shutter.OPEN, step0.getItemValue(shutterItem))

    // Closed in the unexecuted step.
    assertEquals(Shutter.CLOSED, step1.getItemValue(shutterItem))
  }

}

case class CalImpl(
                     lamps: Set[Lamp],
       @BeanProperty shutter: Shutter,
       @BeanProperty filter: Filter,
       @BeanProperty diffuser: Diffuser,
                     observe: Int,
                     exposureTime: Double,
                     coadds: Int,
                     arc: Boolean) extends Calibration {
  def isFlat = !isArc
  def isArc  = arc
  def isBasecalNight = true
  def isBasecalDay = true

  def getLamps = lamps.asJava
  def getObserve = observe
  def getExposureTime = exposureTime
  def getCoadds = coadds
}

class TestCalibrationProvider(cals: List[Calibration]) extends CalibrationProvider {
  def getVersionInfo =
    null
  def getVersion(calType: Calibration.Type, instrument: java.lang.String) =
    null
  def getCalibrations(key: CalibrationKey) =
    cals.asJava
}