package edu.gemini.spModel.config.injector

import edu.gemini.spModel.test.SpModelTestBase
import edu.gemini.pot.sp.{ISPSeqComponent, ISPObsComponent}
import edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME
import edu.gemini.spModel.seqcomp.SeqRepeatObserve
import edu.gemini.spModel.data.config.{DefaultParameter, DefaultSysConfig}

import scala.collection.JavaConverters._
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config2.ItemKey

import org.junit.{Test, Before}
import org.junit.Assert._
import edu.gemini.spModel.gemini.gmos.GmosCommonType.{AmpGain, AmpReadMode}
import edu.gemini.spModel.gemini.gmos.{InstGmosCommon, SeqConfigGmosSouth, InstGmosSouth}
import edu.gemini.spModel.config.map.ConfigValMapInstances

/**
 * Test cases for nested sequence components. Using NIRI but the test case
 * is targeted at making sure nesting works.
 */
class DerivedPropertyTest extends SpModelTestBase {
  private var gmosObsComp: ISPObsComponent = null
  private var gmosDataObj: InstGmosSouth = null

  private var obs1SeqComp: ISPSeqComponent = null
  private var obs1DataObj: SeqRepeatObserve = null

  private var nested1SeqComp: ISPSeqComponent = null
  private var nested1DataObj: SeqConfigGmosSouth = null

  private var nested2SeqComp: ISPSeqComponent = null
  private var nested2DataObj: SeqConfigGmosSouth = null

  private var obs2SeqComp: ISPSeqComponent = null
  private var obs2DataObj: SeqRepeatObserve = null

  @Before override def setUp() {
    super.setUp()

    // Sets up the structure for a test case that demonstrates the problem
    //
    // GMOS
    //    Obs
    // GMOS
    //    Obs

    gmosObsComp = addObsComponent(InstGmosSouth.SP_TYPE)
    gmosDataObj = gmosObsComp.getDataObject.asInstanceOf[InstGmosSouth]

    nested1SeqComp = addSeqComponent(getObs.getSeqComponent, SeqConfigGmosSouth.SP_TYPE)
    nested1DataObj = nested1SeqComp.getDataObject.asInstanceOf[SeqConfigGmosSouth]

    obs1SeqComp = addSeqComponent(nested1SeqComp, SeqRepeatObserve.SP_TYPE)
    obs1DataObj = obs1SeqComp.getDataObject.asInstanceOf[SeqRepeatObserve]

    nested2SeqComp = addSeqComponent(getObs.getSeqComponent, SeqConfigGmosSouth.SP_TYPE)
    nested2DataObj = nested2SeqComp.getDataObject.asInstanceOf[SeqConfigGmosSouth]


    obs2SeqComp = addSeqComponent(nested2SeqComp, SeqRepeatObserve.SP_TYPE)
    obs2DataObj = obs2SeqComp.getDataObject.asInstanceOf[SeqRepeatObserve]
  }

  private def readModeParam(d: AmpReadMode*): DefaultParameter =
    DefaultParameter.getInstance(InstGmosCommon.AMP_READ_MODE_PROP, new java.util.ArrayList(d.toList.asJavaCollection))

  @Test def testBasicNesting() {

    gmosDataObj.setGainChoice(AmpGain.LOW)
    gmosDataObj.setAmpReadMode(AmpReadMode.FAST)
    gmosObsComp.setDataObject(gmosDataObj)


    val nested1SysConfig = new DefaultSysConfig(INSTRUMENT_CONFIG_NAME)
    nested1SysConfig.putParameter(readModeParam(AmpReadMode.FAST, AmpReadMode.SLOW))
    nested1DataObj.setSysConfig(nested1SysConfig)
    nested1SeqComp.setDataObject(nested1DataObj)

    val nested2SysConfig = new DefaultSysConfig(INSTRUMENT_CONFIG_NAME)
    nested2SysConfig.putParameter(readModeParam(AmpReadMode.SLOW))
    nested2DataObj.setSysConfig(nested2SysConfig)
    nested2SeqComp.setDataObject(nested2DataObj)

    // Run the sequence.
    val cs = ConfigBridge.extractSequence(getObs, null, ConfigValMapInstances.TO_DISPLAY_VALUE)

    val key = new ItemKey(new ItemKey(INSTRUMENT_CONFIG_NAME), InstGmosCommon.AMP_GAIN_SETTING_PROP.getName)
    val actual = cs.getItemValueAtEachStep(key)

    val expected = List("6", "2", "2")
    expected.zipWithIndex.foreach(tup => assertEquals(tup._1, actual(tup._2)))
  }

}