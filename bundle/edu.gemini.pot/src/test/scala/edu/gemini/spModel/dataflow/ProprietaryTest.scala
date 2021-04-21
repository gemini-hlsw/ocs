package edu.gemini.spModel.dataflow

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.core.{ProgramType, SPProgramID}
import edu.gemini.spModel.data.config.DefaultParameter
import edu.gemini.spModel.dataflow.GsaAspect.Visibility
import edu.gemini.spModel.dataflow.GsaAspect.Visibility.{PUBLIC, PRIVATE}
import edu.gemini.spModel.dataflow.GsaSequenceEditor.{HEADER_VISIBILITY_KEY, PROPRIETARY_MONTHS_KEY}
import edu.gemini.spModel.gemini.flamingos2.{SeqConfigFlamingos2, Flamingos2}
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.seqcomp.SeqRepeatObserve
import edu.gemini.spModel.test.InstrumentSequenceTestBase
import edu.gemini.spModel.test.InstrumentSequenceTestBase._
import edu.gemini.spModel.rich.pot.sp._

import org.junit.Test
import org.junit.Assert._

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

class ProprietaryTest extends InstrumentSequenceTestBase[Flamingos2, SeqConfigFlamingos2] {
  override protected def getObsCompSpType = Flamingos2.SP_TYPE
  override protected def getSeqCompSpType = SeqConfigFlamingos2.SP_TYPE

  override def setUp(): Unit = {
    super.setUp()

    // We're not really interested in the instrument sequence but we have to put
    // at least thing there in order to have steps to iterate.
    import Flamingos2.{FILTER_PROP, Filter}

    val sc    = createSysConfig
    val filts = (List(Filter.H): List[Filter]).asJava
    sc.putParameter(DefaultParameter.getInstance(FILTER_PROP, filts))
    setSysConfig(sc)
  }

  private def extractGsaInfo(p: ISPProgram): (Visibility, List[Int]) = {
    val cs    = ConfigBridge.extractSequence(p.getObservations.get(0), null, ConfigValMapInstances.IDENTITY_MAP, false)
    val steps = cs.getAllSteps.toList

    val visibility = steps.head.getItemValue(HEADER_VISIBILITY_KEY).asInstanceOf[Visibility]
    val months     = steps.map(_.getItemValue(PROPRIETARY_MONTHS_KEY).asInstanceOf[Int])

    (visibility, months)
  }

  private def doTest(pid: SPProgramID, vis: GsaAspect.Visibility, months: Int*): Unit = {
    val p = getFactory.copyWithNewKeys(getProgram, pid)
    assertEquals((vis, months.toList), extractGsaInfo(p))
  }

  private def doTest(vis: GsaAspect.Visibility, months: Int*): Unit =
    doTest(SPProgramID.toProgramID("GS-2015B-Q-1"), vis, months: _*)

  @Test def testDefaultSetup(): Unit =
    doTest(PUBLIC, 12)

  @Test def testExplicitSetup(): Unit = {
    getProgram.update { _.setGsaAspect(new GsaAspect(true, 999, PRIVATE)) }
    doTest(PRIVATE, 999)
  }

  @Test def testNotCharged(): Unit = {
    getObserveSeqComp.dataObject = getObserveSeqDataObject <| (_.setObsClass(ObsClass.DAY_CAL))
    doTest(PUBLIC, 0)
  }

  @Test def testChanging(): Unit = {
    val o2 = addSeqComponent(getInstSeqComp, SeqRepeatObserve.SP_TYPE)
    o2.dataObject = new SeqRepeatObserve <| (_.setObsClass(ObsClass.DAY_CAL))
    doTest(PUBLIC, 12, 0)
  }

  // REL-3926
  @Test def testProgramCal(): Unit = {
    val o2 = addSeqComponent(getInstSeqComp, SeqRepeatObserve.SP_TYPE)
    o2.dataObject = new SeqRepeatObserve <| (_.setObsClass(ObsClass.PROG_CAL))

    val o3 = addSeqComponent(getInstSeqComp, SeqRepeatObserve.SP_TYPE)
    o3.dataObject = new SeqRepeatObserve <| (_.setObsClass(ObsClass.SCIENCE))

    doTest(PUBLIC, 12, 0, 12)
  }

  @Test def testDefaultPeriods(): Unit =
    ProgramType.All.foreach { pt =>
      val pid = pt.isScience ? s"GS-2015B-${pt.abbreviation}-1" | s"GS-${pt.abbreviation}20150102"
      val gsa = GsaAspect.getDefaultAspect(pt)
      doTest(SPProgramID.toProgramID(pid), PUBLIC, gsa.getProprietaryMonths)
    }

  @Test def testPrivateOverrides(): Unit = {
    getProgram.update { _.setGsaAspect(new GsaAspect(true, 42, PRIVATE)) }
    getObserveSeqComp.dataObject = getObserveSeqDataObject <| (_.setObsClass(ObsClass.DAY_CAL))

    // Even though the daycal isn't charged, the private header flag makes it
    // keep the proprietary period instead of the default of 0 months.
    doTest(PRIVATE, 42)
  }
}
