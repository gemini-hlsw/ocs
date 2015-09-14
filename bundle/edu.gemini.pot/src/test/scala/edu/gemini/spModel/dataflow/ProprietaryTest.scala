package edu.gemini.spModel.dataflow

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.data.config.DefaultParameter
import edu.gemini.spModel.dataflow.GsaSequenceEditor.{PROPRIETARY_MD_KEY, PROPRIETARY_MONTHS_KEY}
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

  private def setGsaAspect(months: Int, keepPrivate: Boolean): Unit =
    getProgram.spProgram.foreach { p =>
      p.setGsaAspect(new GsaAspect(true, months, keepPrivate))
    }

  private def extractGsaInfo(p: ISPProgram): (Boolean, List[Int]) = {
    val cs    = ConfigBridge.extractSequence(p.getObservations.get(0), null, ConfigValMapInstances.IDENTITY_MAP, false)
    val steps = cs.getAllSteps.toList

    val keepPrivate = steps.headOption.exists(_.getItemValue(PROPRIETARY_MD_KEY).asInstanceOf[Boolean])
    val months      = steps.map(_.getItemValue(PROPRIETARY_MONTHS_KEY).asInstanceOf[Int])

    (keepPrivate, months)
  }

  private def doTest(keepPrivate: Boolean, months: Int*): Unit = {
    val p = getFactory.copyWithNewKeys(getProgram, SPProgramID.toProgramID("GS-2015B-Q-1"))
    assertEquals((keepPrivate, months.toList), extractGsaInfo(p))
  }

  @Test def testDefaultSetup(): Unit =
    doTest(keepPrivate = false, 18)

  @Test def testExplicitSetup(): Unit = {
    getProgram.update { _.setGsaAspect(new GsaAspect(true, 999, true)) }
    doTest(keepPrivate = true, 999)
  }

  @Test def testNotCharged(): Unit = {
    getObserveSeqComp.dataObject = getObserveSeqDataObject <| (_.setObsClass(ObsClass.DAY_CAL))
    doTest(keepPrivate = false, 0)
  }

  @Test def testChanging(): Unit = {
    val o2 = addSeqComponent(getInstSeqComp, SeqRepeatObserve.SP_TYPE)
    o2.dataObject = new SeqRepeatObserve <| (_.setObsClass(ObsClass.DAY_CAL))
    doTest(keepPrivate = false, 18, 0)
  }
}
