package edu.gemini.spModel.gemini.gmos

import GmosCommonType.{AmpGain, AmpReadMode}
import edu.gemini.spModel.data.config.DefaultParameter
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config2.ItemKey
import edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME
import edu.gemini.spModel.test.{InstrumentSequenceTestBase => Base}

import java.beans.PropertyDescriptor

import org.junit.Test
import org.junit.Assert._

import scala.collection.JavaConverters._
import edu.gemini.spModel.gemini.gmos.GmosCommonType.DetectorManufacturer
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth
import edu.gemini.spModel.config.map.ConfigValMapInstances

/**
 * Test cases for the gain settings injection.
 */
class GainSettingSeqTest extends Base[InstGmosNorth, SeqConfigGmosNorth] {
  def param[T](desc: PropertyDescriptor, t: T*): DefaultParameter =
    DefaultParameter.getInstance(desc, new java.util.ArrayList(t.asJavaCollection))

  def disperserParam(d: DisperserNorth*): DefaultParameter =
    param(InstGmosNorth.DISPERSER_PROP, d: _*)

  def readModeParam(m: AmpReadMode*): DefaultParameter =
    param(InstGmosCommon.AMP_READ_MODE_PROP, m: _*)

  def gainParam(c: AmpGain*): DefaultParameter =
    param(InstGmosCommon.AMP_GAIN_CHOICE_PROP, c: _*)

  def getObsCompSpType = InstGmosNorth.SP_TYPE
  def getSeqCompSpType = SeqConfigGmosNorth.SP_TYPE

  protected def verifyGainSetting(expected: String*) {
    val cs     = ConfigBridge.extractSequence(getObs, null, ConfigValMapInstances.TO_DISPLAY_VALUE);
    val key    = new ItemKey(new ItemKey(INSTRUMENT_CONFIG_NAME), InstGmosCommon.AMP_GAIN_SETTING_PROP.getName)
    val actual = cs.getItemValueAtEachStep(key)
    assertEquals(expected.length, actual.length)
    expected.zipWithIndex.foreach(tup => assertEquals(tup._1, actual(tup._2)))
  }

  @Test def testStatic() {
    getInstDataObj.setGainChoice(AmpGain.HIGH)
    getInstDataObj.setAmpReadMode(AmpReadMode.SLOW)
    getInstDataObj.setDetectorManufacturer(DetectorManufacturer.E2V)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(disperserParam(DisperserNorth.B1200_G5301, DisperserNorth.B600_G5303))
    setSysConfig(sc)

    verifyGainSetting("1", "1")
  }

  @Test def testInherit() {
    getInstDataObj.setGainChoice(AmpGain.HIGH)
    getInstDataObj.setDetectorManufacturer(DetectorManufacturer.E2V)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(readModeParam(AmpReadMode.FAST, AmpReadMode.SLOW))
    setSysConfig(sc)

    verifyGainSetting("5", "1")
  }

  @Test def testIterate() {
    getInstDataObj.setDetectorManufacturer(DetectorManufacturer.HAMAMATSU)
    storeStaticUpdates

    val sc = Base.createSysConfig
    sc.putParameter(readModeParam(AmpReadMode.FAST, AmpReadMode.FAST, AmpReadMode.SLOW, AmpReadMode.SLOW))
    sc.putParameter(gainParam(AmpGain.HIGH, AmpGain.LOW, AmpGain.HIGH, AmpGain.LOW))
    setSysConfig(sc)

    verifyGainSetting("5", "6", "1", "2")
  }
}