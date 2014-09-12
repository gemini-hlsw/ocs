package edu.gemini.spModel.gemini.gsaoi

import Gsaoi.UtilityWheel
import Gsaoi.Filter

import edu.gemini.spModel.config.injector.ObsWavelengthTestBase
import edu.gemini.spModel.data.config.DefaultParameter
import edu.gemini.spModel.test.{InstrumentSequenceTestBase => Base}

import org.junit.Test

/**
 * Wavelength sequence tests for GSAOI.
 */
class ObsWavelengthSeqTest extends ObsWavelengthTestBase[Gsaoi, GsaoiSeqConfig] {
  private def utilityWheelParam(u: UtilityWheel*): DefaultParameter = param(Gsaoi.UTILITY_WHEEL_PROP, u: _*)
  private def filterParam(f: Filter*): DefaultParameter = param(Gsaoi.FILTER_PROP, f: _*)

  private def verifyFilters(filters: Filter*) {
    verifyWavelength(filters.map(_.formattedWavelength): _*)
  }

  def getObsCompSpType = Gsaoi.SP_TYPE
  def getSeqCompSpType = GsaoiSeqConfig.SP_TYPE

  @Test def testStatic() {
    getInstDataObj.setFilter(Filter.CO)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(utilityWheelParam(UtilityWheel.CLEAR, UtilityWheel.DEFAULT))
    setSysConfig(sc)

    verifyFilters(Filter.CO, Filter.CO)
  }

  @Test def testIterateFilter() {
    val sc = Base.createSysConfig
    sc.putParameter(filterParam(Filter.CO, Filter.H20_ICE))
    setSysConfig(sc)

    verifyFilters(Filter.CO, Filter.H20_ICE)
  }
}