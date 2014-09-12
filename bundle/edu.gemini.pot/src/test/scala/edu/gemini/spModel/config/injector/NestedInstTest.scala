package edu.gemini.spModel.config.injector

import edu.gemini.spModel.test.SpModelTestBase
import edu.gemini.pot.sp.{ISPSeqComponent, ISPObsComponent}
import edu.gemini.spModel.obscomp.InstConstants.OBSERVING_WAVELENGTH_PROP
import edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME
import edu.gemini.spModel.seqcomp.SeqRepeatObserve
import edu.gemini.spModel.gemini.niri.{InstNIRI, SeqConfigNIRI}
import edu.gemini.spModel.gemini.niri.Niri.Disperser
import edu.gemini.spModel.gemini.niri.Niri.Filter
import edu.gemini.spModel.data.config.{DefaultParameter, DefaultSysConfig}

import scala.collection.JavaConverters._
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config2.ItemKey

import org.junit.{Test, Before}
import org.junit.Assert._
import edu.gemini.spModel.config.map.ConfigValMapInstances

/**
 * Test cases for nested sequence components. Using NIRI but the test case
 * is targeted at making sure nesting works.
 */
class NestedInstTest extends SpModelTestBase {
  private var niriObsComp: ISPObsComponent = null
  private var niriDataObj: InstNIRI = null

  private var topLevelSeqComp: ISPSeqComponent = null
  private var topLevelDataObj: SeqConfigNIRI = null

  private var obs1SeqComp: ISPSeqComponent = null
  private var obs1DataObj: SeqRepeatObserve = null

  private var nestedSeqComp: ISPSeqComponent = null
  private var nestedDataObj: SeqConfigNIRI = null

  private var obs2SeqComp: ISPSeqComponent = null
  private var obs2DataObj: SeqRepeatObserve = null

  @Before override def setUp() {
    super.setUp

    // Sets up the structure for a test case that demonstrates the problem in
    // the 2010-B (and before) OT.  Namely:
    //
    // NIRI
    //     Obs
    //     NIRI
    //         Obs

    niriObsComp = addObsComponent(InstNIRI.SP_TYPE)
    niriDataObj = niriObsComp.getDataObject.asInstanceOf[InstNIRI]

    topLevelSeqComp = addSeqComponent(getObs.getSeqComponent, SeqConfigNIRI.SP_TYPE)
    topLevelDataObj = topLevelSeqComp.getDataObject.asInstanceOf[SeqConfigNIRI]

    obs1SeqComp = addSeqComponent(topLevelSeqComp, SeqRepeatObserve.SP_TYPE)
    obs1DataObj = obs1SeqComp.getDataObject.asInstanceOf[SeqRepeatObserve]

    nestedSeqComp = addSeqComponent(topLevelSeqComp, SeqConfigNIRI.SP_TYPE)
    nestedDataObj = nestedSeqComp.getDataObject.asInstanceOf[SeqConfigNIRI]

    obs2SeqComp = addSeqComponent(nestedSeqComp, SeqRepeatObserve.SP_TYPE)
    obs2DataObj = obs2SeqComp.getDataObject.asInstanceOf[SeqRepeatObserve]
  }

  private def disperserParam(d: Disperser*): DefaultParameter =
    DefaultParameter.getInstance(InstNIRI.DISPERSER_PROP, new java.util.ArrayList(d.asJavaCollection))

  private def filterParam(f: Filter*): DefaultParameter =
    DefaultParameter.getInstance(InstNIRI.FILTER_PROP, new java.util.ArrayList(f.asJavaCollection))

  implicit def filterToWavelengthString(f: Filter): String = f.getWavelengthAsString
  implicit def disperserToWavelengthString(d: Disperser): String = d.getCentralWavelengthAsString

  @Test def testBasicNesting() {
    // Set the Filter to K in the first step.
    niriDataObj.setFilter(Filter.BBF_K)
    niriObsComp.setDataObject(niriDataObj)

    // In top level sequence, put Dispersers {None, H}
    val topSysConfig = new DefaultSysConfig(INSTRUMENT_CONFIG_NAME)
    topSysConfig.putParameter(disperserParam(Disperser.NONE, Disperser.H))
    topLevelDataObj.setSysConfig(topSysConfig)
    topLevelSeqComp.setDataObject(topLevelDataObj)

    // In the nested sequence, put Filters {Y, J}
    val nestedSysConfig = new DefaultSysConfig(INSTRUMENT_CONFIG_NAME)
    nestedSysConfig.putParameter(filterParam(Filter.BBF_Y, Filter.BBF_J))
    nestedDataObj.setSysConfig(nestedSysConfig)
    nestedSeqComp.setDataObject(nestedDataObj)

    // Run the sequence.
    val cs = ConfigBridge.extractSequence(getObs, null, ConfigValMapInstances.TO_DISPLAY_VALUE);

    val key = new ItemKey(new ItemKey(INSTRUMENT_CONFIG_NAME), OBSERVING_WAVELENGTH_PROP)
    val actual   = cs.getItemValueAtEachStep(key)
//    val expected = List("2.2", "1.02", "1.25", "1.65", "1.65", "1.65")
    val expected = List[String](
        Filter.BBF_K   // disperser none, filter from static component
      , Filter.BBF_Y   // disperser none, filter Y
      , Filter.BBF_J   // disperser none, filter J
      , Disperser.H    // Explicit disperser H overrides filters
      , Disperser.H    // ditto
      , Disperser.H    // ditto
    )

    expected.zipWithIndex.foreach(tup => assertEquals(tup._1, actual(tup._2)))
  }

  @Test def testFilterOverride() {
    // In top level sequence, put Dispersers {None, H} and Filters {L', M'}
    val topSysConfig = new DefaultSysConfig(INSTRUMENT_CONFIG_NAME)
    topSysConfig.putParameter(disperserParam(Disperser.NONE, Disperser.H))
    topSysConfig.putParameter(filterParam(Filter.BBF_LPRIME, Filter.BBF_MPRIME))
    topLevelDataObj.setSysConfig(topSysConfig)
    topLevelSeqComp.setDataObject(topLevelDataObj)

    // In the nested sequence, put Filters {Y, J}
    val nestedSysConfig = new DefaultSysConfig(INSTRUMENT_CONFIG_NAME)
    nestedSysConfig.putParameter(filterParam(Filter.BBF_Y, Filter.BBF_J))
    nestedDataObj.setSysConfig(nestedSysConfig)
    nestedSeqComp.setDataObject(nestedDataObj)

    // Run the sequence.
    val cs = ConfigBridge.extractSequence(getObs, null, ConfigValMapInstances.TO_DISPLAY_VALUE);

    val key = new ItemKey(new ItemKey(INSTRUMENT_CONFIG_NAME), OBSERVING_WAVELENGTH_PROP)
    val actual   = cs.getItemValueAtEachStep(key)
//    val expected = List("3.78", "1.02", "1.25", "1.65", "1.65", "1.65")

    val expected = List[String](
        Filter.BBF_LPRIME  // Disperser none, filter L' from outer iterator
      , Filter.BBF_Y       // Disperser none, filter Y from inner iterator
      , Filter.BBF_J       // Disperser none, filter J from inner iterator
      , Disperser.H        // Explicit disperser overrides filters
      , Disperser.H
      , Disperser.H
    )

    expected.zipWithIndex.foreach(tup => assertEquals(tup._1, actual(tup._2)))
  }

  // When moving to a second top level iterator, everything should go back to
  // the default values in the static component
  @Test def testTopLevelReset() {

    // Static (Filter K)
    //     NIRI {Filter K}
    //         Obs 1
    //         NIRI {Filter J}
    //             Obs 2
    //     Obs 3             -- reset to K here

    val obs3SeqComp = addSeqComponent(getObs.getSeqComponent, SeqRepeatObserve.SP_TYPE)
    val obs3DataObj = obs3SeqComp.getDataObject.asInstanceOf[SeqRepeatObserve]


    // Set the Filter to K in the first step.
    niriDataObj.setFilter(Filter.BBF_K)
    niriObsComp.setDataObject(niriDataObj)

    // In top level sequence, put Filters {K}
    val topSysConfig = new DefaultSysConfig(INSTRUMENT_CONFIG_NAME)
    topSysConfig.putParameter(filterParam(Filter.BBF_K))
    topLevelDataObj.setSysConfig(topSysConfig)
    topLevelSeqComp.setDataObject(topLevelDataObj)

    // In the nested sequence, put Filters {J}
    val nestedSysConfig = new DefaultSysConfig(INSTRUMENT_CONFIG_NAME)
    nestedSysConfig.putParameter(filterParam(Filter.BBF_J))
    nestedDataObj.setSysConfig(nestedSysConfig)
    nestedSeqComp.setDataObject(nestedDataObj)

    // Run the sequence.
    val cs = ConfigBridge.extractSequence(getObs, null, ConfigValMapInstances.TO_DISPLAY_VALUE);

    val key = new ItemKey(new ItemKey(INSTRUMENT_CONFIG_NAME), OBSERVING_WAVELENGTH_PROP)
    val actual   = cs.getItemValueAtEachStep(key)

    val expected = List[String](
        Filter.BBF_K
      , Filter.BBF_J
      , Filter.BBF_K
    )

    expected.zipWithIndex.foreach(tup => assertEquals(tup._1, actual(tup._2)))
  }

}