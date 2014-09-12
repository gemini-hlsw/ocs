package edu.gemini.spModel.gemini.nifs

import NIFSParams._

import edu.gemini.spModel.config.injector.ObsWavelengthTestBase
import edu.gemini.spModel.data.config.DefaultParameter

import edu.gemini.spModel.test.{InstrumentSequenceTestBase => Base}

import org.junit.Test

/**
 * ObsWavelength sequence tests for NIFS.
 */
class ObsWavelengthSeqTest extends ObsWavelengthTestBase[InstNIFS, SeqConfigNIFS] {

  private def disperserParam(d: Disperser*): DefaultParameter = param(InstNIFS.DISPERSER_PROP, d: _*)
  private def filterParam(f: Filter*): DefaultParameter       = param(InstNIFS.FILTER_PROP, f: _*)
  private def centralParam(d: Double*): DefaultParameter      = doubleParam(InstNIFS.CENTRAL_WAVELENGTH_PROP, d: _*)
  private def maskParam(m: Mask*): DefaultParameter           = param(InstNIFS.MASK_PROP, m: _*)

  private def verifyFilters(filters: Filter*) {
    verifyWavelength(filters.map(_.getWavelength): _*)
  }

  def getObsCompSpType = InstNIFS.SP_TYPE
  def getSeqCompSpType = SeqConfigNIFS.SP_TYPE

  @Test def testStatic() {
    getInstDataObj.setDisperser(Disperser.MIRROR)
    getInstDataObj.setFilter(Filter.HK_FILTER)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(maskParam(Mask.OD_1, Mask.OD_2))
    setSysConfig(sc)

    verifyFilters(Filter.HK_FILTER, Filter.HK_FILTER)
  }

  @Test def testInheritMirror() {
    getInstDataObj.setDisperser(Disperser.MIRROR)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(filterParam(Filter.HK_FILTER, Filter.JH_FILTER))
    setSysConfig(sc)

    verifyFilters(Filter.HK_FILTER, Filter.JH_FILTER)
  }

  @Test def testInheritDisperser() {
    getInstDataObj.setDisperser(Disperser.K)
    getInstDataObj.setCentralWavelength(Disperser.K.getWavelength)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(filterParam(Filter.HK_FILTER, Filter.JH_FILTER))
    setSysConfig(sc)

    val s = Disperser.K.getWavelength.toString
    verifyWavelength(s, s)
  }

  @Test def testInheritFilter() {
    getInstDataObj.setFilter(Filter.HK_FILTER)
    getInstDataObj.setCentralWavelength(Disperser.J.getWavelength)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(disperserParam(Disperser.J, Disperser.MIRROR))
    sc.putParameter(centralParam(Disperser.J.getWavelength, Disperser.H.getWavelength))
    setSysConfig(sc)

    verifyWavelength(Disperser.J.getWavelength.toString, Filter.HK_FILTER.getWavelength)
  }

  @Test def testIterateFilterAndDisperser() {
    val sc = Base.createSysConfig
    sc.putParameter(disperserParam(Disperser.J, Disperser.MIRROR))
    sc.putParameter(filterParam(Filter.HK_FILTER, Filter.JH_FILTER))
    sc.putParameter(centralParam(Disperser.J.getWavelength, Disperser.H.getWavelength))
    setSysConfig(sc)

    verifyWavelength(Disperser.J.getWavelength.toString, Filter.JH_FILTER.getWavelength)
  }
}