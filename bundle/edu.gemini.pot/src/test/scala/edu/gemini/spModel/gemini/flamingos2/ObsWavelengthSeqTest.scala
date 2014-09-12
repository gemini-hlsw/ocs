package edu.gemini.spModel.gemini.flamingos2

import edu.gemini.spModel.gemini.flamingos2.Flamingos2.{Disperser, Filter, LyotWheel}
import edu.gemini.spModel.data.config.DefaultParameter
import edu.gemini.spModel.config.injector.ObsWavelengthTestBase

import edu.gemini.spModel.test.{InstrumentSequenceTestBase => Base}
import org.junit.Test

/**
 * Wavelength sequence tests for Flamingos2.
 */
class ObsWavelengthSeqTest extends ObsWavelengthTestBase[Flamingos2, SeqConfigFlamingos2] {

  private def lyotParam(l: LyotWheel*): DefaultParameter      = param(Flamingos2.LYOT_WHEEL_PROP, l: _*)
  private def disperserParam(d: Disperser*): DefaultParameter = param(Flamingos2.DISPERSER_PROP, d: _*)
  private def filterParam(f: Filter*): DefaultParameter       = param(Flamingos2.FILTER_PROP, f: _*)

  private def verifyFilters(filters: Filter*) {
    verifyWavelength(filters.map(_.getWavelength.getValue.toString): _*)
  }

  private def wavelen(d: Disperser): String = d.getWavelength.getValue.toString
  private def wavelen(f: Filter): String    = f.getWavelength.getValue.toString

  def getObsCompSpType = Flamingos2.SP_TYPE
  def getSeqCompSpType = SeqConfigFlamingos2.SP_TYPE

  @Test def testStatic() {
    getInstDataObj.setDisperser(Disperser.NONE)
    getInstDataObj.setFilter(Filter.J_LOW)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(lyotParam(LyotWheel.H2, LyotWheel.LOW))
    setSysConfig(sc)

    verifyFilters(Filter.J_LOW, Filter.J_LOW)
  }

  @Test def testInheritNone() {
    getInstDataObj.setDisperser(Disperser.NONE)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(filterParam(Filter.J_LOW, Filter.K_SHORT))
    setSysConfig(sc)

    verifyFilters(Filter.J_LOW, Filter.K_SHORT)
  }

  @Test def testInheritDisperser() {
    getInstDataObj.setDisperser(Disperser.R1200HK)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(filterParam(Filter.OPEN, Filter.J_LOW))
    setSysConfig(sc)

    verifyWavelength(wavelen(Disperser.R1200HK), wavelen(Filter.J_LOW))
  }

  @Test def testInheritFilter() {
    getInstDataObj.setFilter(Filter.OPEN)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(disperserParam(Disperser.R1200HK, Disperser.NONE))
    setSysConfig(sc)

    verifyWavelength(wavelen(Disperser.R1200HK), wavelen(Filter.OPEN))
  }

  @Test def testIterateFilterAndDisperser() {
    val sc = Base.createSysConfig
    sc.putParameter(disperserParam(Disperser.NONE, Disperser.R1200HK))
    sc.putParameter(filterParam(Filter.J_LOW, Filter.OPEN))
    setSysConfig(sc)

    verifyWavelength(wavelen(Filter.J_LOW), wavelen(Disperser.R1200HK))
  }
}