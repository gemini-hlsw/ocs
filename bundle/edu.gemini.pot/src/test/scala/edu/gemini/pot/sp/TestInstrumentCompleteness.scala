package edu.gemini.pot.sp

import edu.gemini.pot.sp.SPComponentBroadType.INSTRUMENT;

import org.junit.Assert._
import org.junit.Test

/**
 * Tests that the Instrument enumeration agrees with SPComponentType
 */
final class TestInstrumentCompleteness {

  @Test
  def testInstrumentEnumCompleteness(): Unit = {
    val all = SPComponentType.values.toSet.filter(t => t.broadType == INSTRUMENT) -
                SPComponentType.QPT_CANOPUS -
                SPComponentType.QPT_PWFS

    all.foreach { t =>
      assertTrue(
        s"Instrument enumeration is missing $t",
        Instrument.fromComponentType(t).isDefined
      )
    }
  }
}
