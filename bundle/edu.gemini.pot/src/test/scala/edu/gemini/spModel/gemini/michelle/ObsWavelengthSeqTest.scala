package edu.gemini.spModel.gemini.michelle

import MichelleParams._

import edu.gemini.spModel.config.injector.ObsWavelengthTestBase
import edu.gemini.spModel.data.config.DefaultParameter
import org.junit.Test

import edu.gemini.spModel.test.{InstrumentSequenceTestBase => Base}

/**
 * ObsWavelength sequence tests for Michelle.
 */
class ObsWavelengthSeqTest extends ObsWavelengthTestBase[InstMichelle, SeqConfigMichelle] {
  private def disperserParam(d: Disperser*): DefaultParameter = param(InstMichelle.DISPERSER_PROP, d: _*)
  private def filterParam(f: Filter*): DefaultParameter       = param(InstMichelle.FILTER_PROP, f: _*)
  private def lamdaParam(l: Double*): DefaultParameter        = param(InstMichelle.DISPERSER_LAMBDA_PROP, l: _*)
  private def maskParam(m: Mask*): DefaultParameter           = param(InstMichelle.MASK_PROP, m: _*)

  private def verifyFilters(filters: Filter*) {
    verifyWavelength(filters.map(_.getWavelength): _*)
  }

  def getObsCompSpType = InstMichelle.SP_TYPE
  def getSeqCompSpType = SeqConfigMichelle.SP_TYPE

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
    sc.putParameter(filterParam(Filter.Q, Filter.N10))
    setSysConfig(sc)

    verifyFilters(Filter.Q, Filter.N10)
  }

  @Test def testInheritDisperser() {
    getInstDataObj.setDisperser(Disperser.LOW_RES_10)
    getInstDataObj.setDisperserLambda(Disperser.LOW_RES_10.getLamda)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(filterParam(Filter.Q, Filter.N10))
    setSysConfig(sc)

    val s = Disperser.LOW_RES_10.getLamda.toString
    verifyWavelength(s, s)
  }

  @Test def testInheritFilter() {
    getInstDataObj.setFilter(Filter.Q)
    getInstDataObj.setDisperserLambda(Disperser.LOW_RES_10.getLamda)
    storeStaticUpdates

    val d10 = Disperser.LOW_RES_10
    val dMr = Disperser.MIRROR
    val sc = Base.createSysConfig
    sc.putParameter(disperserParam(d10, dMr))
    sc.putParameter(lamdaParam(d10.getLamda, Disperser.LOW_RES_20.getLamda))
    setSysConfig(sc)

    verifyWavelength(d10.getLamda.toString, Filter.Q.getWavelength)
  }

  @Test def testIterateFilterAndDisperser() {
    val sc = Base.createSysConfig

    val d10 = Disperser.LOW_RES_10
    val dMr = Disperser.MIRROR
    sc.putParameter(disperserParam(d10, dMr))
    sc.putParameter(filterParam(Filter.Q, Filter.N_PRIME))
    sc.putParameter(lamdaParam(d10.getLamda, dMr.getLamda))
    setSysConfig(sc)

    verifyWavelength(d10.getLamda.toString, Filter.N_PRIME.getWavelength)
  }

}