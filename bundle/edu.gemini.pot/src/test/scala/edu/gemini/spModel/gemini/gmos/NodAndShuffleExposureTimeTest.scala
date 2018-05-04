package edu.gemini.spModel.gemini.gmos

import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances.IDENTITY_MAP
import edu.gemini.spModel.config2.{ Config, DefaultConfig, ItemKey }
import edu.gemini.spModel.data.config.DefaultParameter
import edu.gemini.spModel.obscomp.InstConstants
import edu.gemini.spModel.obscomp.InstConstants.{ EXPOSURE_TIME_PROP, SCIENCE_OBSERVE_TYPE }
import edu.gemini.spModel.test.{InstrumentSequenceTestBase => Base}

import org.junit.Test
import org.junit.Assert._

import java.time.Duration
import java.util.ArrayList

import scala.collection.JavaConverters._


/**
 * Test cases for nod and shuffle total exposure time.
 */
final class NodAndShuffleExposureTimeTest extends Base[InstGmosNorth, SeqConfigGmosNorth] {

  def getObsCompSpType = InstGmosNorth.SP_TYPE
  def getSeqCompSpType = SeqConfigGmosNorth.SP_TYPE

  def setupNodAndShuffle(stepCount: Int, cycleCount: Int): Unit = {
    getInstDataObj.setUseNS(GmosCommonType.UseNS.TRUE)
    getInstDataObj.setNsNumCycles(cycleCount)

    val posList = getInstDataObj.getPosList
    posList.removeAllPositions
    (0 until stepCount).foreach(i => posList.addPosition(i.toDouble, i.toDouble))

    storeStaticUpdates()
  }

  def setupSteps(times: Double*): Unit = {
    val sc = Base.createSysConfig
    val p  = DefaultParameter.getInstance(EXPOSURE_TIME_PROP, new ArrayList(times.asJavaCollection))
    sc.putParameter(p)
    setSysConfig(sc)
  }

  private def verifyExposureTime(expectedSecs: Int*): Unit = {
    val expected = expectedSecs.toList.map(s => Duration.ofSeconds(s.toLong))
    val cs       = ConfigBridge.extractSequence(getObs, null, IDENTITY_MAP)
    val actual   = cs.getAllSteps.toList.map(FullExposureTime.asDuration)
    assertEquals(expected.size, actual.size)
    val pairs    = expected.zip(actual)
    assertTrue(pairs.mkString(", "), pairs.forall { case (e, a) => e == a })
  }

  @Test def testNotNodAndShuffle(): Unit = {
    setupSteps(10.0, 20.0)
    verifyExposureTime(10, 20)
  }

  @Test def testNormalNodAndShuffle(): Unit = {
    setupNodAndShuffle(2, 3)
    setupSteps(10.0, 20.0)

    verifyExposureTime(10 * 2 * 3, 20 * 2 * 3)
  }

  @Test def testOddNodAndShuffle(): Unit = {
    setupNodAndShuffle(1, 3)
    setupSteps(10.0, 20.0)

    verifyExposureTime(10 * 3, 20 * 3)
  }

  @Test def testNoSteps(): Unit = {
    setupNodAndShuffle(0, 3)
    setupSteps(10.0, 20.0)

    verifyExposureTime(0, 0)
  }

  @Test def testNoCycles(): Unit = {
    setupNodAndShuffle(2, 0)
    setupSteps(10.0, 20.0)

    verifyExposureTime(0, 0)
  }

  private def nsConfigStep: Config = {
    val c = new DefaultConfig()

    import FullExposureTime.keys._
    c.putItem(StepCount,    new java.lang.Integer(2))
    c.putItem(CycleCount,   new java.lang.Integer(3))
    c.putItem(UseNs,        java.lang.Boolean.TRUE)
    c.putItem(ExposureTime, new java.lang.Double(10.0))
    c.putItem(ObsType,      SCIENCE_OBSERVE_TYPE)

    c
  }

  @Test def testRawConfig(): Unit = {
    assertEquals(Duration.ofSeconds(60), FullExposureTime.asDuration(nsConfigStep))
  }

  private def verifyFailure(key: ItemKey, value: Any): Unit =
    try {
      val c = nsConfigStep
      c.putItem(key, value)
      FullExposureTime.asDuration(c)
      fail(s"setting $key to $value should cause a failure")
    } catch {
      case _: RuntimeException => // ok
    }

  @Test def testUnexpectedType(): Unit = {
    import FullExposureTime.keys._
    verifyFailure(StepCount,    "2"   )
    verifyFailure(CycleCount,   "3"   )
    verifyFailure(UseNs,        "true")
    verifyFailure(ExposureTime, "10.0")
    verifyFailure(ObsType,      1     )
  }

  @Test def testObsType(): Unit = {
    val c = nsConfigStep

    val m = Map(
      InstConstants.ARC_OBSERVE_TYPE     -> 10,
      InstConstants.BIAS_OBSERVE_TYPE    -> 10,
      InstConstants.CAL_OBSERVE_TYPE     -> 10,
      InstConstants.DARK_OBSERVE_TYPE    -> 60,
      InstConstants.FLAT_OBSERVE_TYPE    -> 10,
      InstConstants.SCIENCE_OBSERVE_TYPE -> 60
    )

    m.foreach { case (k,v) =>
      c.putItem(FullExposureTime.keys.ObsType, k)
      assertEquals(Duration.ofSeconds(v), FullExposureTime.asDuration(c))
    }
  }
}
