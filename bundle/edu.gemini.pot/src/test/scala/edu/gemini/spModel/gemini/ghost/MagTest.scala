// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.gemini.ghost

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.ItemKey
import edu.gemini.spModel.core.Magnitude
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.core.MagnitudeSystem.Vega
import edu.gemini.spModel.gemini.ghost.GhostAsterism._
import edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.test.{InstrumentSequenceTestBase => Base}
import org.junit.Test
import org.junit.Assert._

class MagTest extends Base[Ghost, SeqConfigGhost] {

  def getObsCompSpType = SPComponentType.INSTRUMENT_GHOST
  def getSeqCompSpType = SeqConfigGhost.SP_TYPE

  def ghostTarget(
    magG: Option[Double],
    magV: Option[Double]
  ): GhostTarget = {
    val sp = new SPTarget()
    magG.foreach { v => sp.putMagnitude(Magnitude(v, MagnitudeBand._g, None, Vega)) }
    magV.foreach { v => sp.putMagnitude(Magnitude(v, MagnitudeBand.V,  None, Vega)) }
    GhostTarget(sp, GuideFiberState.Enabled)
  }

  private def verifyMagProp(
    magG: Option[Double],
    magV: Option[Double]
  ): Unit = {

    val cs   = ConfigBridge.extractSequence(getObs, null, ConfigValMapInstances.IDENTITY_MAP)
    val gKey = new ItemKey(new ItemKey(INSTRUMENT_CONFIG_NAME), Ghost.MAG_G_PROP)
    val vKey = new ItemKey(new ItemKey(INSTRUMENT_CONFIG_NAME), Ghost.MAG_V_PROP)

    val gValue = Option(cs.getItemValue(0, gKey))
    val vValue = Option(cs.getItemValue(0, vKey))

    assertEquals(magG, gValue)
    assertEquals(magV, vValue)
  }

  @Test def testSingleTargetMags(): Unit = {
    val a = GhostAsterism.SingleTarget(ghostTarget(Some(5.0), Some(6.0)), None)
    setAsterism(a)
    verifyMagProp(Some(5.0), Some(6.0))
  }

  @Test def testMissingMag(): Unit = {
    val a = GhostAsterism.SingleTarget(ghostTarget(None, Some(6.0)), None)
    setAsterism(a)
    verifyMagProp(None, Some(6.0))
  }

  @Test def testDualTargetMags(): Unit = {
    val a = GhostAsterism.DualTarget(
      ghostTarget(Some(5.0), Some(6.0)),
      ghostTarget(Some(7.0), Some(4.0)),
      None
    )
    setAsterism(a)
    verifyMagProp(Some(7.0), Some(6.0))
  }
}
