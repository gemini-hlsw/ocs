package edu.gemini.spModel.seqcomp

import edu.gemini.pot.sp.{ISPSeqComponent, ISPNode, SPComponentType}
import edu.gemini.spModel.gemini.gmos.{GmosSouthType, SeqConfigGmosSouth, InstGmosSouth}
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth.{r_G0326, g_G0325}
import edu.gemini.spModel.test.InstrumentSequenceTestBase
import edu.gemini.spModel.test.InstrumentSequenceTestBase._

import org.junit.Test
import org.junit.Assert._

import scala.collection.JavaConverters._

class InstrumentSequenceSyncTest extends InstrumentSequenceTestBase[InstGmosSouth, SeqConfigGmosSouth] {
  override protected val getObsCompSpType: SPComponentType = SPComponentType.INSTRUMENT_GMOSSOUTH
  override protected val getSeqCompSpType: SPComponentType = SPComponentType.ITERATOR_GMOSSOUTH

  private val FilterProp = InstGmosSouth.FILTER_PROP.getName

  private def expectStaticFilter(f: GmosSouthType.FilterSouth): Unit =
    assertEquals(f, getInstObsComp.getDataObject.asInstanceOf[InstGmosSouth].getFilter)


  private def expectSequenceFilters(sc: ISPSeqComponent, fs: GmosSouthType.FilterSouth*): Unit =
    assertEquals(fs.asJava, sc.getDataObject.asInstanceOf[SeqConfigGmosSouth].getSysConfig.getParameterValue(FilterProp))

  private def expectSequenceFilters(fs: GmosSouthType.FilterSouth*): Unit =
    expectSequenceFilters(getInstSeqComp, fs: _*)

  @Test
  def testIteratorUpdatePropagates(): Unit = {
    assertNotEquals(r_G0326, getInstDataObj.getFilter)

    // Add a step that sets the "r" filter.
    val sc = createSysConfig()
    sc.putParameter(getParam(FilterProp, r_G0326))
    setSysConfig(sc)

    // Should be reflected in the static component.
    expectStaticFilter(r_G0326)
  }

  @Test
  def testStaticUpdatePropagates(): Unit = {

    // Add a step that sets the "r" filter.
    val sc = createSysConfig()
    sc.putParameter(getParam(FilterProp, r_G0326))
    setSysConfig(sc)

    // Set the filter in the static component.
    getInstDataObj.setFilter(g_G0325)
    storeStaticUpdates()

    // Should be reflected in the iterator.
    expectSequenceFilters(g_G0325)
  }

  @Test
  def testRearrangeIterators(): Unit = {
    // REL-1939: Top level configuration overwrites instrument iterators
    // Changed behavior to reverse the sync direction when a new or different
    // iterator becomes the first iterator.  Was inst => iterator,
    // now iterator => inst.

    val cmp1 = getInstSeqComp

    // Add a step that sets the "r" filter.
    val sc1 = createSysConfig()
    sc1.putParameter(getParam(FilterProp, r_G0326))
    setSysConfig(sc1)

    // Create a second sequence iterator.
    val cmp2 = addSeqComponent(getObs.getSeqComponent, getSeqCompSpType)
    val dbj2 = cmp2.getDataObject.asInstanceOf[SeqConfigGmosSouth]
    val sc2  = createSysConfig()
    sc2.putParameter(getParam(FilterProp, g_G0325))
    dbj2.setSysConfig(sc2)
    cmp2.setDataObject(dbj2)

    // Static component should still show r
    expectStaticFilter(r_G0326)

    // Rearrange the iterators.
    val root = getObs.getSeqComponent
    root.setChildren(List(cmp2: ISPNode, cmp1).asJava)

    // Static component switches to g.
    expectStaticFilter(g_G0325)

    // First iterator (which was originally the second) stays at g
    expectSequenceFilters(cmp2, g_G0325)

    // Second iterator (which was originally the first) stays at r
    expectSequenceFilters(cmp1, r_G0326)
  }
}
