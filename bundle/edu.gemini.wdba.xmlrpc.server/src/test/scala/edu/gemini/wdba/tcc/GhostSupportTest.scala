//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.gemini.ghost.Ghost
import edu.gemini.spModel.gemini.ghost.GhostAsterism
import edu.gemini.spModel.target.SPCoordinates
import edu.gemini.spModel.target.env.TargetEnvironment
import org.junit.Test;

/**
 * Test cases for GMOS instrument support.
 */
class GhostSupportTest extends InstrumentSupportTestBase[Ghost](SPComponentType.INSTRUMENT_GHOST) {
  private val emptyTarget = GhostAsterism.GhostTarget.empty
  private val emptySky    = new SPCoordinates()

  @Test
  def testSingleTarget() {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.SingleTarget(emptyTarget, None)));
    verifyInstrumentConfig(getSouthResults, "GHOST_SINGLE_TARGET");
  }

  @Test
  def testDualTarget() {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.DualTarget(emptyTarget, emptyTarget, None)));
    verifyInstrumentConfig(getSouthResults, "GHOST_DUAL_TARGET");
  }

  @Test
  def testTargetPlusSky() {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.TargetPlusSky(emptyTarget, emptySky, None)));
    verifyInstrumentConfig(getSouthResults, "GHOST_TARGET_PLUS_SKY");
  }

  @Test
  def testSkyPlusTarget() {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.SkyPlusTarget(emptySky, emptyTarget, None)));
    verifyInstrumentConfig(getSouthResults, "GHOST_SKY_PLUS_TARGET");
  }

  @Test
  def testHighResolution() {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.HighResolutionTargetPlusSky(emptyTarget, emptySky, GhostAsterism.PrvMode.PrvOff, None)));
    verifyInstrumentConfig(getSouthResults, "GHOST_HIGH_RESOLUTION");
  }

  @Test
  def testHighResolutionPrv() {
    setTargetEnvironment(TargetEnvironment.create(GhostAsterism.HighResolutionTargetPlusSky(emptyTarget, emptySky, GhostAsterism.PrvMode.PrvOn, None)));
    verifyInstrumentConfig(getSouthResults, "GHOST_HIGH_RESOLUTION");
  }
}
