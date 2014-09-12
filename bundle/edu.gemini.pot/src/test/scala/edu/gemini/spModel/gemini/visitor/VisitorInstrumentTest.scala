package edu.gemini.spModel.gemini.visitor

import org.junit.Test
import org.junit.Assert._
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.config2.DefaultConfig
import edu.gemini.shared.util.immutable.Some
import edu.gemini.pot.sp.SPComponentBroadType

class VisitorInstrumentTest {
  @Test def testVisitorIsAnInstrument() {
    assertEquals(SPComponentBroadType.INSTRUMENT, new VisitorInstrument().getType.broadType)
  }

  @Test def testVisitorCanBeInBothSites() {
    assertEquals(Site.SET_BOTH, new VisitorInstrument().getSite)
  }

  @Test def testVisitorPhaseIResource() {
    assertEquals("gemVisitor", new VisitorInstrument().getPhaseIResourceName)
  }

  @Test def testCalcTimes() {
    assertEquals(0, new VisitorInstrument().calc(new DefaultConfig, new Some(new DefaultConfig)).totalTime)
  }

  @Test def testNameProperty() {
    val inst = new VisitorInstrument

    assertTrue(inst.getProperties.containsKey("name"))
  }

  @Test def testVisitorName() {
    val inst = new VisitorInstrument
    inst.setName("MyInst")

    assertEquals("MyInst", inst.getName)
  }
}
