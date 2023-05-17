//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.gemini.ghost.Ghost
import edu.gemini.spModel.gemini.ghost.GhostAsterism
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.SPCoordinates
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import org.junit.Test;

/**
 * Test cases for GMOS instrument support.
 */
class GhostSupportTest extends InstrumentSupportTestBase[Ghost](SPComponentType.INSTRUMENT_GHOST) {
  private val emptyTarget = GhostAsterism.GhostTarget.empty
  private val emptySky    = new SPCoordinates()

  @Test
  def testSingleTarget(): Unit = {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.SingleTarget(emptyTarget, None)))
    verifyInstrumentConfig(getSouthResults, "GHOST_SINGLE_TARGET")
  }

  @Test
  def testDualTarget(): Unit = {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.DualTarget(emptyTarget, emptyTarget, None)))
    verifyInstrumentConfig(getSouthResults, "GHOST_DUAL_TARGET")
  }

  @Test
  def testTargetPlusSky(): Unit = {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.TargetPlusSky(emptyTarget, emptySky, None)))
    verifyInstrumentConfig(getSouthResults, "GHOST_TARGET_PLUS_SKY")
  }

  @Test
  def testSkyPlusTarget(): Unit = {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.SkyPlusTarget(emptySky, emptyTarget, None)))
    verifyInstrumentConfig(getSouthResults, "GHOST_SKY_PLUS_TARGET")
  }

  @Test
  def testHighResolution(): Unit = {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.HighResolutionTargetPlusSky(emptyTarget, emptySky, GhostAsterism.PrvMode.PrvOff, None)))
    verifyInstrumentConfig(getSouthResults, "GHOST_HIGH_RESOLUTION")
  }

  @Test
  def testHighResolutionPrv(): Unit = {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.HighResolutionTargetPlusSky(emptyTarget, emptySky, GhostAsterism.PrvMode.PrvOn, None)))
    verifyInstrumentConfig(getSouthResults, "GHOST_HIGH_RESOLUTION")
  }

  @Test
  def testSingleTargetP2(): Unit = {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.SingleTarget(emptyTarget, None)))
    addGuideStar(PwfsGuideProbe.pwfs2)
    verifyInstrumentConfig(getSouthResults, "GHOST_SINGLE_TARGET_P2")
  }

  @Test
  def testDualTargetP2(): Unit = {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.DualTarget(emptyTarget, emptyTarget, None)))
    addGuideStar(PwfsGuideProbe.pwfs2)
    verifyInstrumentConfig(getSouthResults, "GHOST_DUAL_TARGET_P2")
  }

  @Test
  def testTargetPlusSkyP2(): Unit = {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.TargetPlusSky(emptyTarget, emptySky, None)))
    addGuideStar(PwfsGuideProbe.pwfs2)
    verifyInstrumentConfig(getSouthResults, "GHOST_TARGET_PLUS_SKY_P2")
  }

  @Test
  def testSkyPlusTargetP2(): Unit = {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.SkyPlusTarget(emptySky, emptyTarget, None)))
    addGuideStar(PwfsGuideProbe.pwfs2)
    verifyInstrumentConfig(getSouthResults, "GHOST_SKY_PLUS_TARGET_P2")
  }

  @Test
  def testHighResolutionP2(): Unit = {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.HighResolutionTargetPlusSky(emptyTarget, emptySky, GhostAsterism.PrvMode.PrvOff, None)))
    addGuideStar(PwfsGuideProbe.pwfs2)
    verifyInstrumentConfig(getSouthResults, "GHOST_HIGH_RESOLUTION_P2")
  }

  @Test
  def testHighResolutionPrvP2(): Unit = {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.HighResolutionTargetPlusSky(emptyTarget, emptySky, GhostAsterism.PrvMode.PrvOn, None)))
    addGuideStar(PwfsGuideProbe.pwfs2)
    verifyInstrumentConfig(getSouthResults, "GHOST_HIGH_RESOLUTION_P2")
  }
}
