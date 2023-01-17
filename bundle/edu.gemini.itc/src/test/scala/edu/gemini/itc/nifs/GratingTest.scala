package edu.gemini.itc.nifs

import org.junit.Test
import org.junit.Assert._

/**
 * Validate that gratings are read properly from the configuration files.
 */
class GratingTest {

  @Test
  def validateNifsGrating(): Unit = {
    val g = new NifsGratingOptics("/nifs/nifs_", "J", 1200.0, 1, 1)
    assertEquals(1200.0,    g.getEffectiveWavelength,         0.00001)
    assertEquals(1199.9475, g.getStart,                       0.00001)
    assertEquals(1200.0525, g.getEnd,                         0.00001)
    assertEquals(6600.0,    g.getGratingBlaze,                0.00001)
    assertEquals(1.093,     g.resolutionHalfArcsecSlit,       0.00001)
    assertEquals(0.105,     g.dispersion(-1),            0.00001)
    assertEquals(6040.0,    g.getGratingResolvingPower,       0.00001)
    assertEquals(0.105,     g.getPixelWidth,                  0.00001)
  }


}
