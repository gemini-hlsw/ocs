package edu.gemini.spModel.gemini.gmos

import GmosCommonType._
import GmosNorthType._

import org.junit.Test

import edu.gemini.spModel.config.injector.ObsWavelengthTestBase
import edu.gemini.spModel.data.config.DefaultParameter

import edu.gemini.spModel.test.{InstrumentSequenceTestBase => Base}

/**
 * ObsWavelength sequence tests for GMOS.
 */
class ObsWavelengthSeqTest extends ObsWavelengthTestBase[InstGmosNorth, SeqConfigGmosNorth] {
  private def disperserParam(d: DisperserNorth*): DefaultParameter = param(InstGmosNorth.DISPERSER_PROP, d: _*)
  private def filterParam(f: FilterNorth*): DefaultParameter       = param(InstGmosNorth.FILTER_PROP, f: _*)
  private def lambdaParam(d: Double*): DefaultParameter            = param(InstGmosCommon.DISPERSER_LAMBDA_PROP, d: _*)
  private def fpuParam(u: FPUnitNorth*): DefaultParameter          = param(InstGmosNorth.FPUNIT_PROP, u: _*)

  private def verifyFilters(filters: Filter*) {
    verifyWavelength(filters.map(_.getWavelength): _*)
  }

  def getObsCompSpType = InstGmosNorth.SP_TYPE
  def getSeqCompSpType = SeqConfigGmosNorth.SP_TYPE

  @Test def testStatic() {
    getInstDataObj.setDisperser(DisperserNorth.MIRROR)
    getInstDataObj.setFilter(FilterNorth.Ha_G0310)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(fpuParam(FPUnitNorth.IFU_1, FPUnitNorth.IFU_2))
    setSysConfig(sc)

    verifyFilters(FilterNorth.Ha_G0310, FilterNorth.Ha_G0310)
  }

  @Test def testInheritMIrror() {
    getInstDataObj.setDisperser(DisperserNorth.MIRROR)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(filterParam(FilterNorth.Ha_G0310, FilterNorth.CaT_G0309))
    setSysConfig(sc)

    verifyFilters(FilterNorth.Ha_G0310, FilterNorth.CaT_G0309)
  }

  @Test def testInheritDisperser() {
    getInstDataObj.setDisperser(DisperserNorth.R150_G5306)
    getInstDataObj.setDisperserLambda(42.0)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(filterParam(FilterNorth.Ha_G0310, FilterNorth.CaT_G0309))
    setSysConfig(sc)

    verifyWavelength("0.042", "0.042")
  }

  @Test def testInheritFilter() {
    getInstDataObj.setFilter(FilterNorth.CaT_G0309)
    getInstDataObj.setDisperserLambda(42.0)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(disperserParam(DisperserNorth.B1200_G5301, DisperserNorth.MIRROR))
    sc.putParameter(lambdaParam(55.0, 66.0))
    setSysConfig(sc)

    verifyWavelength("0.055", FilterNorth.CaT_G0309.getWavelength)
  }

  @Test def testIterateFilterAndDisperser() {
    val sc = Base.createSysConfig
    sc.putParameter(disperserParam(DisperserNorth.B1200_G5301, DisperserNorth.MIRROR))
    sc.putParameter(filterParam(FilterNorth.g_G0301, FilterNorth.Ha_G0310))
    sc.putParameter(lambdaParam(100.0, 200.0))
    setSysConfig(sc)

    verifyWavelength("0.100", FilterNorth.Ha_G0310.getWavelength)
  }
}