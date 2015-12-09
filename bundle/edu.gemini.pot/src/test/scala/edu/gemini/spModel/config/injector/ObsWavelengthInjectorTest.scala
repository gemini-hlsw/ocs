package edu.gemini.spModel.config.injector

import obswavelength.ObsWavelengthCalc2
import org.junit.Test
import org.junit.Assert._
import java.beans.PropertyDescriptor

import scala.beans.BeanProperty
import edu.gemini.spModel.obscomp.InstConstants.OBSERVING_WAVELENGTH_PROP
import edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME
import edu.gemini.spModel.data.config._

class ObsWavelengthInjectorTest {
  case class Filter(wl: Double)
  case class Disperser(wl: Double, useFilter: Boolean)

  class SimpleInstrument(@BeanProperty var filter: Filter, @BeanProperty var disperser: Disperser)

  object SimpleInstrument {
    val injector = ConfigInjector.create(new ObsWavelengthCalc2[Disperser, Filter]() {
      def descriptor1 = new PropertyDescriptor("disperser", classOf[SimpleInstrument])
      def descriptor2 = new PropertyDescriptor("filter",    classOf[SimpleInstrument])
      override def calcWavelength(d: Disperser, f: Filter): String = calc(d, f)
    })

    def calc(d: Disperser, f: Filter): String =
      (if (d.useFilter) f.wl else d.wl).toString
  }

  // Doesn't inject anything, but also doesn't blow up.
  @Test def testEmptyCur() {
    val cur  = new DefaultConfig
    val prev = new DefaultConfig

    SimpleInstrument.injector.inject(cur, prev)
    assertNull(cur.getSysConfig(INSTRUMENT_CONFIG_NAME))
  }

  // Should inject a wavelength.
  @Test def testEmptyPrev() {
    val cur  = new DefaultConfig
    val prev = new DefaultConfig

    insert(Filter(1.0), Disperser(2.0, useFilter = true), cur)

    SimpleInstrument.injector.inject(cur, prev)
    verify(1.0, cur)  // use the filter value
  }

  // Tests a missing value.  Since it doesn't have a value for all the
  // parameters, nothing is done.
  @Test def testMissingValue() {
    val cur  = new DefaultConfig
    val prev = new DefaultConfig

    insert(Some(Filter(1.0)), None, cur)

    SimpleInstrument.injector.inject(cur, prev)
    verifyNoWavelength(cur)
  }

  // Nothing changed, so it doesn't try to inject a value for wv.  Assumes the
  // value that should have been put in a previous step is still current.
  @Test def testNoUpdates() {
    val cur  = new DefaultConfig
    val prev = new DefaultConfig

    insert(Filter(1.0), Disperser(2.0, true), cur)
    insert(Filter(1.0), Disperser(2.0, true), prev)
    prev.putParameter(INSTRUMENT_CONFIG_NAME, DefaultParameter.getInstance(OBSERVING_WAVELENGTH_PROP, "1.0"))

    SimpleInstrument.injector.inject(cur, prev)
    verifyNoWavelength(cur)
  }

  // Don't insert a wavelength value even though the parameters are different
  // if the calculated value is the same.
  @Test def testDifferingParamsSameWavelength() {
    val cur  = new DefaultConfig
    val prev = new DefaultConfig

    insert(Filter(1.0), Disperser(2.0, true), cur)
    insert(Filter(1.0), Disperser(3.0, true), prev)

    // Somehow a previous step inserted the same wavelength value we calculate
    // for the current step so nothing should be added by the injector.
    prev.putParameter(INSTRUMENT_CONFIG_NAME, DefaultParameter.getInstance(OBSERVING_WAVELENGTH_PROP, "1.0"))

    SimpleInstrument.injector.inject(cur, prev)
    verifyNoWavelength(cur)
  }

  // A normal case with differing parameters that results in a new wavelength.
  @Test def testWavelengthUpdate() {
    val cur  = new DefaultConfig
    val prev = new DefaultConfig

    insert(Filter(1.0), Disperser(2.0, true), cur)
    insert(Filter(3.0), Disperser(4.0, true), prev)

    // Somehow a previous step inserted the same wavelength value we calculate
    // for the current step so nothing should be added by the injector.
    prev.putParameter(INSTRUMENT_CONFIG_NAME, DefaultParameter.getInstance(OBSERVING_WAVELENGTH_PROP, "3.0"))

    SimpleInstrument.injector.inject(cur, prev)
    verify(1.0, cur)  // use the filter value
  }

  // Test use of value from previous steps.
  @Test def testUsePrevStepValues() {
    val cur  = new DefaultConfig
    val prev = new DefaultConfig

    // We'll get the disperser from the previous step
    insert(Some(Filter(1.0)), None, cur)
    insert(Filter(3.0), Disperser(4.0, true), prev)

    // Somehow a previous step inserted the same wavelength value we calculate
    // for the current step so nothing should be added by the injector.
    prev.putParameter(INSTRUMENT_CONFIG_NAME, DefaultParameter.getInstance(OBSERVING_WAVELENGTH_PROP, "3.0"))

    SimpleInstrument.injector.inject(cur, prev)
    verify(1.0, cur)  // use the filter value
  }

  private def insert(fopt: Option[Filter], dopt: Option[Disperser], c: IConfig) {
    val sc = new DefaultSysConfig(INSTRUMENT_CONFIG_NAME)

    fopt.foreach {
      f => sc.putParameter(DefaultParameter.getInstance("filter", f))
    }

    dopt.foreach {
      d => sc.putParameter(DefaultParameter.getInstance("disperser", d))
    }

    c.putSysConfig(sc)
  }

  private def insert(f: Filter, d: Disperser, c: IConfig) {
    insert(Some(f), Some(d), c)
  }

  private def verify(wl: Double, config: IConfig) {
    assertEquals(wl.toString, config.getParameterValue(INSTRUMENT_CONFIG_NAME, OBSERVING_WAVELENGTH_PROP))
  }

  private def verifyNoWavelength(config: IConfig) {
    assertNull(config.getParameterValue(INSTRUMENT_CONFIG_NAME, OBSERVING_WAVELENGTH_PROP))
  }

}