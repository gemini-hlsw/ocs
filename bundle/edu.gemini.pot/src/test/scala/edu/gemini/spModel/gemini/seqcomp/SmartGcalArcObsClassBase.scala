package edu.gemini.spModel.gemini.seqcomp

import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances.IDENTITY_MAP
import edu.gemini.spModel.config2.ItemKey
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.calunit.CalUnitParams._
import edu.gemini.spModel.gemini.gnirs.InstGNIRS
import edu.gemini.spModel.obsclass.ObsClass
import org.junit.Assert._
import org.junit.Before
import org.junit.Test

/**
 * Support for testing the obs class setting for arcs.
 */
abstract class SmartGcalArcObsClassBase extends GcalExecTest {

  val obsClassItem = new ItemKey("observe:class")

  @Before override def setUp() {
    super.setUp(progId)

    addObsComponent(InstGNIRS.SP_TYPE)
    addSeqComponent(getObs.getSeqComponent, SeqRepeatSmartGcalObs.Arc.SP_TYPE)
  }

  val cal = CalImpl(Set(Lamp.arcLamps().get(0)), Shutter.OPEN, Filter.ND_10, Diffuser.IR, 1, 1.0, 1, arc = true)

  protected def expect(c: ObsClass): Unit = {
    locking {
      setupGcal(cal)

      // Generate the sequence.
      val cs = ConfigBridge.extractSequence(getObs, null, IDENTITY_MAP)

      assertEquals(1, cs.getAllSteps.size)

      val step = cs.getStep(0)

      assertEquals(c, ObsClass.parseType(step.getItemValue(obsClassItem).asInstanceOf[String]))
    }
  }

}