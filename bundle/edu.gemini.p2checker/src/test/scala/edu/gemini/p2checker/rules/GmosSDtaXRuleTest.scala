package edu.gemini.p2checker.rules


import edu.gemini.p2checker.api.{Problem, ObservationElements}
import edu.gemini.p2checker.rules.gmos.GmosRule
import edu.gemini.spModel.data.config.DefaultParameter
import edu.gemini.spModel.gemini.gmos.GmosCommonType.{DTAX, Binning}
import edu.gemini.spModel.gemini.gmos.{InstGmosNorth, GmosNorthType, SeqConfigGmosSouth, GmosSouthType, InstGmosSouth}
import org.junit.{Before, Test}
import org.junit.Assert._

import scala.collection.JavaConverters._

class GmosSDtaXRuleTest extends AbstractRuleTest {

  @Before def before() {
    prog = super.createBasicProgram("GN-2015B-Q-1")
  }

  private def initGmos(binDtaX: (Int, Int)*): Unit = {
    def toBinning(b: Int): Binning = Binning.values().toList.find(_.getValue == b).get
    def toDtaX(x: Int): DTAX       = DTAX.values().toList.find(_.intValue() == x).get
    val params                     = binDtaX.toList.map { case (b0, x0) => (toBinning(b0), toDtaX(x0)) }

    val gmos    = addGmosSouth()
    val dataObj = gmos.getDataObject.asInstanceOf[InstGmosSouth]
    dataObj.setFilter(GmosSouthType.FilterSouth.g_G0325) // avoid a different unrelated warning ...

    val (b, x) = params.head
    dataObj.setCcdXBinning(b)
    dataObj.setCcdYBinning(b)
    dataObj.setDtaXOffset(x)

    gmos.setDataObject(dataObj)

    val iter       = addGmosSouthIterator()
    val seqDataObj = iter.getDataObject.asInstanceOf[SeqConfigGmosSouth]
    val sysConf    = seqDataObj.getSysConfig

    val (bs, xs)   = params.unzip
    val binX       = DefaultParameter.getInstance("ccdXBinning", bs.asJava)
    val binY       = DefaultParameter.getInstance("ccdYBinning", bs.asJava)
    val dtaX       = DefaultParameter.getInstance("dtaXOffset",  xs.asJava)

    sysConf.putParameter(binX)
    sysConf.putParameter(binY)
    sysConf.putParameter(dtaX)

    seqDataObj.setSysConfig(sysConf)
    iter.setDataObject(seqDataObj)
  }

  private def runCheck: List[Problem] = {
    val rule     = new GmosRule
    val elems    = new ObservationElements(obs)
    rule.check(elems).getProblems.asScala.toList
  }

  private def expectSuccess(hasId: Set[String]): Unit =
    assertTrue(!runCheck.exists{p => hasId(p.getId)})

  private def expectSuccessBinY(): Unit =
    expectSuccess(Set(
      GmosRule.GMOS_S_DTA_X_RULE_Y1_ID,
      GmosRule.GMOS_S_DTA_X_RULE_Y2_4_ID)
    )

  private def expectSuccessBinMultiple(): Unit =
    expectSuccess(Set(GmosRule.DTA_X_Y_MULTIPLE_BINNING_RULE_ID))

  private def expectFailure(problemType: Problem.Type, hasId: Set[String]): Unit =
    assertTrue(runCheck.exists { p =>
      p.getType == problemType && hasId(p.getId)
    })

  private def expectFailureY1(): Unit =
    expectFailure(Problem.Type.ERROR, Set(GmosRule.GMOS_S_DTA_X_RULE_Y1_ID))

  private def expectFailureY2_4(): Unit =
    expectFailure(Problem.Type.ERROR, Set(GmosRule.GMOS_S_DTA_X_RULE_Y2_4_ID))

  private def expectFailureBinMultiple(): Unit =
      expectFailure(Problem.Type.WARNING, Set(GmosRule.DTA_X_Y_MULTIPLE_BINNING_RULE_ID))

  @Test def testStaticComponentSuccess1(): Unit =
    (-4 to 6).foreach { x =>
      initGmos((1, x))
      expectSuccessBinY()
    }

  @Test def testStaticComponentSuccess2(): Unit =
    (-2 to 6).foreach { x =>
      initGmos((2, x))
      expectSuccessBinY()
    }

  @Test def testIteratorSuccess1(): Unit =
    for {
      x0 <- -4 to 0
      x1 <- -4 to 0
    } {
      initGmos((1, x0), (1, x1))
      expectSuccessBinY()
    }

  @Test def testIteratorSuccess2(): Unit =
    for {
      x0 <- -2 to 0
      x1 <- -2 to 0
    } {
      initGmos((2, x0), (2, x1))
      expectSuccessBinY()
    }

  @Test def testStaticComponentFailure1(): Unit =
    (-6 to -5).foreach { x =>
      initGmos((1, x))
      expectFailureY1()
    }

  @Test def testIteratorComponentFailure1(): Unit =
    (-6 to -5).foreach { x =>
      initGmos((1, 2), (1, x))
      expectFailureY1()
    }

  @Test def testStaticComponentFailure2(): Unit =
    (-6 to -3).foreach { x =>
      initGmos((2, x))
      expectFailureY2_4()
    }

  @Test def testIteratorComponentFailure2(): Unit =
    (-6 to -3).foreach { x =>
      initGmos((2, 2), (2, x))
      expectFailureY2_4()
    }

  @Test def testGmosNorthIgnored(): Unit = {
    val gmos    = addGmosNorth()
    val dataObj = gmos.getDataObject.asInstanceOf[InstGmosNorth]
    dataObj.setFilter(GmosNorthType.FilterNorth.g_G0301) // avoid a different unrelated warning ...

    dataObj.setCcdXBinning(Binning.ONE)
    dataObj.setCcdYBinning(Binning.ONE)
    dataObj.setDtaXOffset(DTAX.MSIX)

    gmos.setDataObject(dataObj)

    expectSuccessBinY()
  }

  @Test def testDtaXYMultipleBinningSuccess(): Unit = {
    initGmos((1,4), (2, 4))
    expectSuccessBinMultiple()
  }
  @Test def testDtaXYMultipleBinningFailure(): Unit = {
    initGmos((2, 3), (2,5))
    expectFailureBinMultiple()
  }
}
