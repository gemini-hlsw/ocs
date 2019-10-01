package edu.gemini.spModel.gemini.gsaoi

import org.junit.Test
import org.junit.Assert._
import java.beans.PropertyDescriptor

import edu.gemini.spModel.core.SiderealTarget
import edu.gemini.spModel.data.config.{DefaultParameter, IParameter}
import edu.gemini.spModel.gemini.gems.CanopusWfs
import edu.gemini.spModel.gemini.gsaoi.Gsaoi._
import edu.gemini.spModel.gemini.gsaoi.Gsaoi.Filter._
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffset
import edu.gemini.spModel.guide.DefaultGuideOptions
import edu.gemini.spModel.guide.DefaultGuideOptions.Value.{off, on}
import edu.gemini.spModel.obs.plannedtime.{OffsetOverheadCalculator, PlannedTimeCalculator}
import edu.gemini.skycalc.{Angle, Offset}
import edu.gemini.spModel.target.env.GuideProbeTargets
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.target.offset.OffsetPos
import edu.gemini.spModel.test.InstrumentSequenceTestBase
import edu.gemini.spModel.test.InstrumentSequenceTestBase._

import scala.collection.JavaConverters._

class PlannedTimeTest extends InstrumentSequenceTestBase[Gsaoi, GsaoiSeqConfig] {
  override protected def getObsCompSpType = Gsaoi.SP_TYPE
  override protected def getSeqCompSpType = GsaoiSeqConfig.SP_TYPE

  private def getParameter[T](pd: PropertyDescriptor, values: T*): IParameter =
    DefaultParameter.getInstance(pd.getName, values.toList.asJava)

  override def setUp() {
    super.setUp()

    // Add a canopus guide star so that guiding will be turned on
    val env  = getTargetEnvironment
    val grp  = env.getPrimaryGuideGroup
    val target = new SPTarget(SiderealTarget.empty)
    val env2 = env.setPrimaryGuideGroup(grp.put(GuideProbeTargets.create(CanopusWfs.cwfs3, target)))

    val dobj = getTarget.getDataObject.asInstanceOf[TargetObsComp]
    dobj.setTargetEnvironment(env2)
    getTarget.setDataObject(dobj)
  }

  @Test def testFilterWheelMove() {
    val sc0 = createSysConfig
    sc0.putParameter(getParameter(FILTER_PROP, BR_GAMMA, BR_GAMMA))
    setSysConfig(sc0)

    val noMove = PlannedTimeCalculator.instance.calc(getObs).totalTime

    val sc1 = createSysConfig
    sc1.putParameter(getParameter(FILTER_PROP, BR_GAMMA, CH4_LONG))
    setSysConfig(sc1)

    val move = PlannedTimeCalculator.instance.calc(getObs).totalTime

    assertEquals(15000, move - noMove)
  }

  // difference between running the sequence with offset positions (guiding
  // according to the guiding param) vs. with no offset positions
  private def offsetDifference(guiding: Seq[DefaultGuideOptions.Value]): Long = {

    // compute no offset time by creating a sequence with the same number of
    // steps as offset positions
    val sc     = createSysConfig()
    sc.putParameter(getCoaddsParam(Seq.fill(guiding.length)(new java.lang.Integer(1)): _*))
    sc.putParameter(getExpTimeParam(Seq.fill(guiding.length)(new java.lang.Double(1.0)): _*))
    setSysConfig(sc)

    val noOffsetTime = PlannedTimeCalculator.instance.calc(getObs).totalTime

    // Create an offset sequence with a position per guiding option, using
    // the corresponding guiding option.  All the positions are (1,1) away
    // from the previous (with the first one being at (1,1))
    sc.putParameter(getCoaddsParam(1))
    sc.putParameter(getExpTimeParam(1.0))
    setSysConfig(sc)

    val comp     = getInstSeqComp
    val children = comp.getSeqComponents

    comp.setSeqComponents(java.util.Collections.emptyList())

    val offsetComp    = addSeqComponent(comp, SeqRepeatOffset.SP_TYPE)
    val offsetDataObj = offsetComp.getDataObject.asInstanceOf[SeqRepeatOffset]
    val posList       = offsetDataObj.getPosList

    guiding.zipWithIndex foreach { case (g, i) =>
      val pos = posList.addPosition(i+1, i+1)
      pos.setDefaultGuideOption(g)
    }

    offsetComp.setDataObject(offsetDataObj)
    offsetComp.setSeqComponents(children)

    val offsetTime = PlannedTimeCalculator.instance.calc(getObs).totalTime

    offsetTime - noOffsetTime
  }

  // Time for a normal offset of distance (1, 1)
  private val standardOffsetTime = {
    val a0 = new Angle(0, Angle.Unit.ARCSECS)
    val o0 = new Offset(a0, a0)

    val a1 = new Angle(1, Angle.Unit.ARCSECS)
    val o1 = new Offset(a1, a1)

    OffsetOverheadCalculator.instance.calc(o0, o1)
  }

  private def expected(guiding: Seq[DefaultGuideOptions.Value]): Long = {
    val offsets = (off :: guiding.toList).zipWithIndex map { case (guideOption, index) =>
      val pos = new OffsetPos("", index.toDouble, index.toDouble)
      pos.setDefaultGuideOption(guideOption)
      pos
    }

    def isStandardOffset(prev: OffsetPos, cur: OffsetPos): Boolean =
      (prev.getDefaultGuideOption == off) && (cur.getDefaultGuideOption == off)

    val (time, _) = ((0.0,offsets.head)/:offsets.tail) { case ((time,prev),cur) =>
      if (isStandardOffset(prev, cur)) (time + standardOffsetTime, cur)
      else (time + Gsaoi.GUIDED_OFFSET_OVERHEAD, cur)
    }
    (time * 1000).round
  }

  private def validate(guiding: DefaultGuideOptions.Value*) {
    val actual = offsetDifference(guiding)
    assertEquals(expected(guiding), actual)
  }

  @Test def testNoSkyOffsets() {
    validate(on, on)
  }

  @Test def testAllSkyOffsets() {
    validate(off, off)
  }


  @Test def testOneSkyPosition() {
    validate(on, off, on)
  }


  @Test def testMultipleSkyPositions() {
    validate(on, off, off, on)
  }

  @Test def testInitialSkyPositions() {
    validate(off, off, on)
  }
}
