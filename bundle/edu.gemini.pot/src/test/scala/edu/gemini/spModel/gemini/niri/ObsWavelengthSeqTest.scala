package edu.gemini.spModel.gemini.niri

import edu.gemini.spModel.gemini.niri.Niri.{Camera, Disperser, Filter}
import edu.gemini.spModel.data.config.DefaultParameter
import edu.gemini.spModel.config.injector.ObsWavelengthTestBase

import edu.gemini.spModel.test.{InstrumentSequenceTestBase => Base}
import org.junit.Test

/**
 * Wavelength sequence tests for NIRI.
 */
class ObsWavelengthSeqTest extends ObsWavelengthTestBase[InstNIRI, SeqConfigNIRI] {

  private def cameraParam(c: Camera*): DefaultParameter = param(InstNIRI.CAMERA_PROP, c: _*)
  private def disperserParam(d: Disperser*): DefaultParameter = param(InstNIRI.DISPERSER_PROP, d: _*)
  private def filterParam(f: Filter*): DefaultParameter = param(InstNIRI.FILTER_PROP, f: _*)

  protected def verifyFilters(filters: Filter*) {
    verifyWavelength(filters.map(_.getWavelengthAsString): _*)
  }

  def getObsCompSpType = InstNIRI.SP_TYPE
  def getSeqCompSpType = SeqConfigNIRI.SP_TYPE

  @Test def testStatic() {
    getInstDataObj.setDisperser(Disperser.NONE)
    getInstDataObj.setFilter(Filter.BBF_J)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(cameraParam(Camera.F14, Camera.F32))
    setSysConfig(sc)

    verifyFilters(Filter.BBF_J, Filter.BBF_J)
  }

  @Test def testInheritMirror() {
    getInstDataObj.setDisperser(Disperser.NONE)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(filterParam(Filter.BBF_H, Filter.BBF_J))
    setSysConfig(sc)

    verifyFilters(Filter.BBF_H, Filter.BBF_J)
  }

  @Test def testInheritDisperser() {
    getInstDataObj.setDisperser(Disperser.K)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(filterParam(Filter.BBF_H, Filter.BBF_J))
    setSysConfig(sc)

    val s = Disperser.K.getCentralWavelengthAsString
    verifyWavelength(s, s)
  }

  @Test def testInheritFilter() {
    getInstDataObj.setFilter(Filter.BBF_K)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(disperserParam(Disperser.J, Disperser.NONE))
    setSysConfig(sc)

    verifyWavelength(Disperser.J.getCentralWavelengthAsString, Filter.BBF_K.getWavelengthAsString)
  }

  @Test def testIterateFilterAndDisperser() {
    val sc = Base.createSysConfig
    sc.putParameter(disperserParam(Disperser.J, Disperser.NONE))
    sc.putParameter(filterParam(Filter.BBF_Y, Filter.BBF_K))
    setSysConfig(sc)

    verifyWavelength(Disperser.J.getCentralWavelengthAsString, Filter.BBF_K.getWavelengthAsString)
  }
}