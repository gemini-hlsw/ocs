package edu.gemini.spModel.gemini.ghost

import edu.gemini.spModel.core.Target.TargetType
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances.IDENTITY_MAP
import edu.gemini.spModel.config2.{Config, ConfigSequence, ItemKey}
import edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME
import edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY
import edu.gemini.spModel.target.SPCoordinates
import edu.gemini.spModel.target.SPTarget
import org.junit.Assert._
import org.junit.Test

class SeqexecConfigTest extends SequenceTestBase {
  import GhostAsterism._

  private val options = new java.util.HashMap[String, Object]

  private def step0: Config =
    ConfigBridge.extractSequence(getObs, options, IDENTITY_MAP).getStep(0)

  private def key(n: String): ItemKey =
    new ItemKey(INSTRUMENT_KEY, n)

  def sidrealTarget: GhostTarget =
    GhostTarget(new SPTarget(), GuideFiberState.Enabled)

  def nonSidrealTarget: GhostTarget = {
    val t = new SPTarget()
    t.setNonSidereal()
    GhostTarget(t, GuideFiberState.Enabled)
  }

  @Test def testSingleTargetSidereal(): Unit = {
    setAsterism(SingleTarget(sidrealTarget, None))
    assertNull(step0.getItemValue(key(Ghost.BASE_TYPE)))
    assertEquals(TargetType.Sidereal, step0.getItemValue(key(Ghost.SRIFU1_TYPE)))
    assertNull(step0.getItemValue(key(Ghost.SRIFU2_TYPE)))
  }

  @Test def testSingleTargetNonsidereal(): Unit = {
    setAsterism(SingleTarget(nonSidrealTarget, None))
    assertNull(step0.getItemValue(key(Ghost.BASE_TYPE)))
    assertEquals(TargetType.NonSidereal, step0.getItemValue(key(Ghost.SRIFU1_TYPE)))
    assertNull(step0.getItemValue(key(Ghost.SRIFU2_TYPE)))
  }

  @Test def testSingleTargetOverridenBase(): Unit = {
    setAsterism(SingleTarget(nonSidrealTarget, Some(SPCoordinates.zero)))
    assertEquals(TargetType.Sidereal,    step0.getItemValue(key(Ghost.BASE_TYPE)))
    assertEquals(TargetType.NonSidereal, step0.getItemValue(key(Ghost.SRIFU1_TYPE)))
    assertNull(step0.getItemValue(key(Ghost.SRIFU2_TYPE)))
  }

  @Test def testSkyPlusTargetSidereal(): Unit = {
    setAsterism(SkyPlusTarget(SPCoordinates.zero, sidrealTarget, None))
    assertNull(step0.getItemValue(key(Ghost.BASE_TYPE)))
    assertEquals(TargetType.Sidereal, step0.getItemValue(key(Ghost.SRIFU1_TYPE)))
    assertEquals(TargetType.Sidereal, step0.getItemValue(key(Ghost.SRIFU2_TYPE)))
  }

  @Test def testSkyPlusTargetNonSidereal(): Unit = {
    setAsterism(SkyPlusTarget(SPCoordinates.zero, nonSidrealTarget, None))
    assertNull(step0.getItemValue(key(Ghost.BASE_TYPE)))
    assertEquals(TargetType.Sidereal,    step0.getItemValue(key(Ghost.SRIFU1_TYPE)))
    assertEquals(TargetType.NonSidereal, step0.getItemValue(key(Ghost.SRIFU2_TYPE)))
  }

  @Test def testSkyPlusTargetNonSiderealOverridenBase(): Unit = {
    setAsterism(SkyPlusTarget(SPCoordinates.zero, nonSidrealTarget, Some(SPCoordinates.zero)))
    assertEquals(TargetType.Sidereal,    step0.getItemValue(key(Ghost.BASE_TYPE)))
    assertEquals(TargetType.Sidereal,    step0.getItemValue(key(Ghost.SRIFU1_TYPE)))
    assertEquals(TargetType.NonSidereal, step0.getItemValue(key(Ghost.SRIFU2_TYPE)))
  }

  @Test def testTargetPlusSkySidereal(): Unit = {
    setAsterism(TargetPlusSky(sidrealTarget, SPCoordinates.zero, None))
    assertNull(step0.getItemValue(key(Ghost.BASE_TYPE)))
    assertEquals(TargetType.Sidereal, step0.getItemValue(key(Ghost.SRIFU1_TYPE)))
    assertEquals(TargetType.Sidereal, step0.getItemValue(key(Ghost.SRIFU2_TYPE)))
  }

  @Test def testTargetPlusSkyNonSidereal(): Unit = {
    setAsterism(TargetPlusSky(nonSidrealTarget, SPCoordinates.zero, None))
    assertNull(step0.getItemValue(key(Ghost.BASE_TYPE)))
    assertEquals(TargetType.NonSidereal, step0.getItemValue(key(Ghost.SRIFU1_TYPE)))
    assertEquals(TargetType.Sidereal,    step0.getItemValue(key(Ghost.SRIFU2_TYPE)))
  }

  @Test def testTargetPlusSkyNonSiderealOverridenBase(): Unit = {
    setAsterism(TargetPlusSky(nonSidrealTarget, SPCoordinates.zero, Some(SPCoordinates.zero)))
    assertEquals(TargetType.Sidereal,    step0.getItemValue(key(Ghost.BASE_TYPE)))
    assertEquals(TargetType.NonSidereal, step0.getItemValue(key(Ghost.SRIFU1_TYPE)))
    assertEquals(TargetType.Sidereal,    step0.getItemValue(key(Ghost.SRIFU2_TYPE)))
  }
}
