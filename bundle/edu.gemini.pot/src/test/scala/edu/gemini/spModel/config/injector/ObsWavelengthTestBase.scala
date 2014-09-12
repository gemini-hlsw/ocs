package edu.gemini.spModel.config.injector

import edu.gemini.spModel.obscomp.InstConstants.OBSERVING_WAVELENGTH_PROP
import edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.seqcomp.SeqConfigObsBase
import edu.gemini.spModel.test.InstrumentSequenceTestBase
import edu.gemini.spModel.config2.ItemKey
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.data.config.DefaultParameter
import java.beans.PropertyDescriptor

import org.junit.Assert._

import scala.collection.JavaConverters._
import edu.gemini.spModel.config.map.ConfigValMapInstances

/**
 * Simple ObsWavelength sequencing test base class.
 */
abstract class ObsWavelengthTestBase[I <: SPInstObsComp, S <: SeqConfigObsBase] extends InstrumentSequenceTestBase[I, S] {
  protected def param[T](desc: PropertyDescriptor, t: T*): DefaultParameter =
    DefaultParameter.getInstance(desc, new java.util.ArrayList(t.asJavaCollection))

  protected def doubleParam(desc: PropertyDescriptor, d: Double*): DefaultParameter = {
    val col = d.map(new java.lang.Double(_)).asJavaCollection
    DefaultParameter.getInstance(desc, new java.util.ArrayList(col))
  }

  protected def verifyWavelength(expected: String*) {
    verifyWavelengthWithName(OBSERVING_WAVELENGTH_PROP, expected: _*)
  }

  protected def verifyWavelengthWithName(name: String, expected: String*) {
    val cs     = ConfigBridge.extractSequence(getObs, null, ConfigValMapInstances.TO_DISPLAY_VALUE);
    val key    = new ItemKey(new ItemKey(INSTRUMENT_CONFIG_NAME), name)
    val actual = cs.getItemValueAtEachStep(key)
    assertEquals(expected.length, actual.length)
    expected.zipWithIndex.foreach(tup => assertEquals(tup._1, actual(tup._2)))
  }
}