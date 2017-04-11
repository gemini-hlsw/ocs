package edu.gemini.spModel.gemini.seqcomp

import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances.IDENTITY_MAP
import edu.gemini.spModel.config2.ItemKey
import edu.gemini.spModel.gemini.calunit.CalUnitParams._
import edu.gemini.spModel.gemini.gnirs.InstGNIRS
import org.junit.Assert._
import org.junit.{Before, Test}

/**
 * Tests that executed steps don't change even if the mapping changes.
 */
class SmartGcalExecutedStepTest extends GcalExecTest {
  val shutterItem  = new ItemKey("calibration:shutter")
  val diffuserItem = new ItemKey("calibration:diffuser")

  @Before override def setUp() {
    super.setUp(progId)

    addObsComponent(InstGNIRS.SP_TYPE)
    addSeqComponent(getObs.getSeqComponent, SeqRepeatSmartGcalObs.Arc.SP_TYPE)
  }

  val cal = CalImpl(Set(Lamp.arcLamps().get(0)), Shutter.OPEN, Filter.ND_10, Diffuser.IR, 1, 1.0, 1, arc = true)

  @Test def testNoExecutedSteps() {
    setupGcal(cal)
    val cs = ConfigBridge.extractSequence(getObs, null, IDENTITY_MAP)

    assertEquals(1, cs.getAllSteps.size)

    val step = cs.getStep(0)
    assertEquals(cal.shutter,  step.getItemValue(shutterItem))
    assertEquals(cal.diffuser, step.getItemValue(diffuserItem))
  }

  @Test def testExecutedStepDoesNotChange() {
    // Execute the first step with shutter OPEN
    setupGcal(cal)
    exec(1)

    // Change the calibration mapping to shutter CLOSED
    setupGcal(cal.copy(shutter = Shutter.CLOSED))

    // Generate the sequence.
    val cs = ConfigBridge.extractSequence(getObs, null, IDENTITY_MAP)

    assertEquals(1, cs.getAllSteps.size)

    val step = cs.getStep(0)

    // Still Open because it was that way when we recorded the step executed
    assertEquals(Shutter.OPEN, step.getItemValue(shutterItem))
  }

  @Test def testUnexecutedStepAfterMappingChange() {
    // Add another smart cal to the sequence so that we have two
    addSeqComponent(getObs.getSeqComponent, SeqRepeatSmartGcalObs.Arc.SP_TYPE)

    // Execute the first step with shutter OPEN
    setupGcal(cal)
    exec(1)

    // Change the calibration mapping to shutter CLOSED
    setupGcal(cal.copy(shutter = Shutter.CLOSED))

    // Generate the sequence.
    val cs = ConfigBridge.extractSequence(getObs, null, IDENTITY_MAP)

    assertEquals(2, cs.getAllSteps.size)

    val step0 = cs.getStep(0)
    val step1 = cs.getStep(1)

    // Still Open because it was that way when we recorded the step executed
    assertEquals(Shutter.OPEN, step0.getItemValue(shutterItem))

    // Closed in the unexecuted step.
    assertEquals(Shutter.CLOSED, step1.getItemValue(shutterItem))
  }

}