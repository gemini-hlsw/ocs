// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.gemini.gmos

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.data.config.ISysConfig
import edu.gemini.spModel.obs.plannedtime.PlannedTime.Category
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator
import edu.gemini.spModel.test.InstrumentSequenceTestBase
import org.junit.Test
import org.junit.Assert.assertEquals

import scala.collection.JavaConverters._

final class PlannedTimeTest extends InstrumentSequenceTestBase[InstGmosNorth, SeqConfigGmosNorth] {

  override protected def getObsCompSpType: SPComponentType = InstGmosNorth.SP_TYPE
  override protected def getSeqCompSpType: SPComponentType = SeqConfigGmosNorth.SP_TYPE

  private def setupNodAndShuffle(eOffsetting: Boolean): Unit = {
    getInstDataObj.setUseNS(true)
    getInstDataObj.setUseElectronicOffsetting(eOffsetting)
    getInstDataObj.setNsNumCycles(10)
    getInstDataObj.getPosList.addPosition(0.0,  0.0)
    getInstDataObj.getPosList.addPosition(0.0, 10.0)
    storeStaticUpdates()
  }

  private def addStep(exposureTimeSec: Double): Unit = {
    val sc = InstrumentSequenceTestBase.createSysConfig
    sc.putParameter(InstrumentSequenceTestBase.getExpTimeParam(90.0))
    setSysConfig(sc)
  }

  private def firstStepExposureTime: Long =
    // Ridiculous
    PlannedTimeCalculator
      .instance
      .calc(getObs)
      .steps
      .get(0)
      .times
      .groupTimes
      .get(Category.EXPOSURE)
      .get(0)
      .time


  @Test def testNodAndShuffleWithEOffsetting(): Unit = {
    setupNodAndShuffle(eOffsetting = true)
    addStep(90.0)

    assertEquals(Math.round(1000.0 * 10.0 * (90.0 * 2.0 + 23.2)), firstStepExposureTime)
  }

  @Test def testNodAndShuffle(): Unit = {
    setupNodAndShuffle(eOffsetting = false)
    addStep(90.0)

    assertEquals(Math.round(1000.0 * 10.0 * (90.0 * 2.0 + 36.0)), firstStepExposureTime)
  }
}
