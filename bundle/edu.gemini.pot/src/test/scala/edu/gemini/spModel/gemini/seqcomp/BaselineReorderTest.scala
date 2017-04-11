package edu.gemini.spModel.gemini.seqcomp

import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances.IDENTITY_MAP
import edu.gemini.spModel.config2.ItemKey
import edu.gemini.spModel.gemini.calunit.CalUnitParams._
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import org.junit.Assert._
import org.junit.{Before, Test}

final class BaselineReorderTest extends GcalExecTest {

  val observeTypeItem  = new ItemKey("observe:observeType")

  @Before override def setUp(): Unit = {
    super.setUp(progId)

    addObsComponent(Flamingos2.SP_TYPE)

    // 2 baseline night observations (4 datasets)
    (1 to 2).foreach { _ =>
      addSeqComponent(getObs.getSeqComponent, SeqRepeatSmartGcalObs.BaselineNight.SP_TYPE)
    }
  }

  val flat = CalImpl(Set(Lamp.IR_GREY_BODY_HIGH), Shutter.OPEN,   Filter.ND_20, Diffuser.IR, 1,  4.0, 1, arc = false)
  val arc  = CalImpl(Set(Lamp.AR_ARC),            Shutter.CLOSED, Filter.NIR,   Diffuser.IR, 1, 15.0, 1, arc = true )

  @Test def testOrderChange(): Unit = {

    // Originally, arcs came before flats.
    setupGcal(arc, flat)
    exec(1)
    exec(2)

    // Now flats come before arcs
    setupGcal(flat, arc)

    // Generate the sequence.
    val cs = ConfigBridge.extractSequence(getObs, null, IDENTITY_MAP)

    assertEquals(4, cs.getAllSteps.size)

    // Even though flats come before arcs, the first two steps are already
    // executed so they don't flip to flat, arc.
    val actual   = cs.getItemValueAtEachStep(observeTypeItem)
    val expected = List("ARC", "FLAT", "FLAT", "ARC")  // yes, Strings

    actual.zip(expected).foreach { case (a,e) => assertEquals(a, e) }
  }

  @Test def testPartialExecution(): Unit = {

    // Originally, arcs came before flats.  Do just one of the two baseline
    // calibration steps.
    setupGcal(arc, flat)
    exec(1)

    // Now flats come before arcs
    setupGcal(flat, arc)

    // Generate the sequence.
    val cs = ConfigBridge.extractSequence(getObs, null, IDENTITY_MAP)

    assertEquals(4, cs.getAllSteps.size)

    // Even though flats come before arcs, the first step was already
    // executed so it doesn't flip to "FLAT".  The second step is not executed
    // though so unfortunately it will be a second ARC.  I don't think there
    // is a way around this.  I also don't think this is a big deal since we
    // won't have any partially executed baseline calibrations that we intend
    // to come back to and finish anyway.

    val actual   = cs.getItemValueAtEachStep(observeTypeItem)
    val expected = List("ARC", "ARC", "FLAT", "ARC")  // yes, Strings

    actual.zip(expected).foreach { case (a,e) => assertEquals(a, e) }
  }
}
