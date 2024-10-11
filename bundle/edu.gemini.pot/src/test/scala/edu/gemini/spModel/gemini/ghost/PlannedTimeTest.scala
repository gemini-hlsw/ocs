// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.gemini.ghost

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.test.InstrumentSequenceTestBase.createSysConfig
import org.junit.Test

class PlannedTimeTest extends SequenceTestBase {

  private def setExposure(
    redCount:    Int,
    redTimeSec:  Int,
    blueCount:   Int,
    blueTimeSec: Int
  ): Unit = {
    val sc = createSysConfig()
    sc.putParameter(getParameter(Ghost.RED_EXPOSURE_COUNT_PROP, redCount))
    sc.putParameter(getParameter(Ghost.RED_EXPOSURE_TIME_PROP, redTimeSec))
    sc.putParameter(getParameter(Ghost.BLUE_EXPOSURE_COUNT_PROP, blueCount))
    sc.putParameter(getParameter(Ghost.BLUE_EXPOSURE_TIME_PROP, blueTimeSec))
    setSysConfig(sc)
  }

  @Test def testOneStep(): Unit = {
    setExposure(1, 11, 1, 12)

    // Red dominates here because of its slow readout
    verifyMs(
      8 * 60 * 1000 + // setup
      11000          + // exposure
      58994          + // 1 1x1 medium (red) readout
       5000            // DHS write
    )
  }

  @Test def testFastRed(): Unit = {
    // Switch to fast red (but leave blue at slow).  Now blue dominates.
    val ghost = getInstDataObj
    ghost.setRedReadNoiseGain(GhostReadNoiseGain.FAST_LOW)
    storeStaticUpdates()

    setExposure(1, 11, 1, 12)

    verifyMs(
      8 * 60 * 1000 + // setup
      12000          + // exposure
      45957          + // 1 1x1 slow (blue) readout
       5000            // DHS write
    )
  }

  @Test def test2by2Red(): Unit = {
    // Switch to 2x2 red (but leave blue at 1x1).  Now blue dominates.
    val ghost = getInstDataObj
    ghost.setRedBinning(GhostBinning.TWO_BY_TWO)
    storeStaticUpdates()

    setExposure(1, 11, 1, 12)

    verifyMs(
      8 * 60 * 1000 + // setup
      12000          + // exposure
      45957          + // 1 1x1 slow (blue) readout
       5000            // DHS write
    )
  }

  @Test def testAddBias(): Unit = {
    // add bias step.  they have 0 exposure time but pay whatever readout
    // cost is charged for the camera, speed and binning
    addSeqComponent(getObs.getSeqComponent, SPComponentType.OBSERVER_BIAS)

    // We'll make blue dominate the science step with a long exposure time.
    // In the bias step, red will dominate because of the long readout time.
    setExposure(1, 11, 1, 100)

    verifyMs(
      8 * 60 * 1000 + // setup

      // science step
      100000         + // exposure
       45957         + // 1 1x1 slow (blue) readout
        5000         + // DHS write

      // bias step
       58994         + // 1 1x1 medium (red) readout
        5000           // DHS write
    )

  }
}
