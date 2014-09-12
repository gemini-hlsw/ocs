package edu.gemini.spModel.gemini.trecs

import TReCSParams._

import edu.gemini.spModel.config.injector.ObsWavelengthTestBase
import edu.gemini.spModel.data.config.DefaultParameter
import org.junit.Test

import edu.gemini.spModel.test.{InstrumentSequenceTestBase => Base}

/**
 * ObsWavelength sequence tests for TReCS.
 */
class ObsWavelengthSeqTest extends ObsWavelengthTestBase[InstTReCS, SeqConfigTReCS] {
  private def disperserParam(d: Disperser*): DefaultParameter = param(InstTReCS.DISPERSER_PROP, d: _*)
  private def filterParam(f: Filter*): DefaultParameter       = param(InstTReCS.FILTER_PROP, f: _*)
  private def lamdaParam(l: Double*): DefaultParameter        = param(InstTReCS.DISPERSER_LAMBDA_PROP, l: _*)
  private def maskParam(m: Mask*): DefaultParameter           = param(InstTReCS.MASK_PROP, m: _*)

  private def verifyFilters(filters: Filter*) {
    verifyWavelength(filters.map(_.getWavelength): _*)
  }

  def getObsCompSpType = InstTReCS.SP_TYPE
  def getSeqCompSpType = SeqConfigTReCS.SP_TYPE

  @Test def testStatic() {
    getInstDataObj.setDisperser(Disperser.MIRROR)
    getInstDataObj.setFilter(Filter.N_PRIME)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(maskParam(Mask.MASK_1, Mask.MASK_2))
    setSysConfig(sc)

    verifyFilters(Filter.N_PRIME, Filter.N_PRIME)
  }

  @Test def testInheritMirror() {
    getInstDataObj.setDisperser(Disperser.MIRROR)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(filterParam(Filter.Q, Filter.K))
    setSysConfig(sc)

    verifyFilters(Filter.Q, Filter.K)
  }

  @Test def testInheritDisperser() {
    getInstDataObj.setDisperser(Disperser.LOW_RES_10)
    getInstDataObj.setDisperserLambda(42.0)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(filterParam(Filter.Q, Filter.K))
    setSysConfig(sc)

    verifyWavelength("42.0", "42.0")
  }

  @Test def testInheritFilter() {
    getInstDataObj.setFilter(Filter.Q)
    getInstDataObj.setDisperserLambda(17.0)
    storeStaticUpdates

    val d10 = Disperser.LOW_RES_10
    val dMr = Disperser.MIRROR
    val sc = Base.createSysConfig
    sc.putParameter(disperserParam(d10, dMr))
    sc.putParameter(lamdaParam(6.0, 7.0))
    setSysConfig(sc)

    verifyWavelength("6.0", Filter.Q.getWavelength)
  }

  @Test def testIterateFilterAndDisperser() {
    val sc = Base.createSysConfig

    val d10 = Disperser.LOW_RES_10
    val dMr = Disperser.MIRROR
    sc.putParameter(disperserParam(d10, dMr))
    sc.putParameter(filterParam(Filter.Q, Filter.N_PRIME))
    sc.putParameter(lamdaParam(6.0, 7.0))
    setSysConfig(sc)

    verifyWavelength("6.0", Filter.N_PRIME.getWavelength)
  }

}