package edu.gemini.p2checker.rules

import edu.gemini.p2checker.api.ObservationElements
import edu.gemini.p2checker.rules.gmos.GmosRule
import edu.gemini.spModel.data.config.DefaultParameter
import edu.gemini.spModel.gemini.gmos.{GmosCommonType, GmosNorthType, InstGmosNorth, SeqConfigGmosNorth}

import org.junit.{Before, Test}
import org.junit.Assert._
import scala.collection.JavaConverters._
import edu.gemini.p2checker.util.MdfConfigRule


class GmosMdfConfigRuleTest extends AbstractRuleTest {

  @Before def before() {
    prog = super.createBasicProgram("GN-2014B-LP-2")
  }

  private def initGmos(names: Option[String]*): Unit = {
    val gmos    = addGmosNorth()
    val dataObj = gmos.getDataObject.asInstanceOf[InstGmosNorth]
    dataObj.setFilter(GmosNorthType.FilterNorth.Ha_G0310) // avoid a different unrelated warning ...

    import GmosNorthType.FPUnitNorth.{CUSTOM_MASK, LONGSLIT_1}
    names.toList match {
      case Some(maskName) :: _ =>
        dataObj.setFPUnitNorth(CUSTOM_MASK)
        dataObj.setFPUnitCustomMask(maskName)
      case _                   => // do nothing
    }
    gmos.setDataObject(dataObj)

    val iter       = addGmosIterator()
    val seqDataObj = iter.getDataObject.asInstanceOf[SeqConfigGmosNorth]

    val pairs = names.map { _.fold((LONGSLIT_1, "x")){ name => (CUSTOM_MASK, name)} }
    val (fpus, maskNames) = pairs.unzip

    val fpuParam       = DefaultParameter.getInstance("fpu", fpus.asJava)
    val fpuModeParam   = DefaultParameter.getInstance("fpuMode", fpus.map{
      case GmosNorthType.FPUnitNorth.CUSTOM_MASK => GmosCommonType.FPUnitMode.CUSTOM_MASK
      case _                                     => GmosCommonType.FPUnitMode.BUILTIN
    }.asJava)
    val maskNamesParam = DefaultParameter.getInstance("fpuCustomMask", maskNames.asJava)
    val sysConf        = seqDataObj.getSysConfig
    sysConf.putParameter(fpuParam)
    sysConf.putParameter(fpuModeParam)
    sysConf.putParameter(maskNamesParam)
    seqDataObj.setSysConfig(sysConf)
    iter.setDataObject(seqDataObj)
  }

  private def expectSuccess(): Unit = {
    val rule     = new GmosRule
    val elems    = new ObservationElements(obs)
    val problems = rule.check(elems).getProblems
    assertTrue(problems.isEmpty)
  }

  private def expectFailure(description: String): Unit = {
    val rule     = new GmosRule
    val elems    = new ObservationElements(obs)
    val problems = rule.check(elems).getProblems.asScala.toList.map(_.getDescription)
    problems.filter(_ == description) match {
      case `description` :: Nil => // okay
      case _                    => fail(problems.mkString(", "))
    }
  }

  private def expectFormatFailure(maskName: String): Unit = expectFailure(MdfConfigRule.FormatError(maskName))
  private def expectMatchFailure(maskName: String): Unit  = expectFailure(MdfConfigRule.MatchWarning(maskName))

  @Test def testStaticComponentSuccess(): Unit = {
    initGmos(Some("GN2014BLP002-40"))
    expectSuccess()
  }

  @Test def testStaticComponentFailure(): Unit = {
    val maskName = "GX2014BLP002-41"
    initGmos(Some(maskName))
    expectFormatFailure(maskName)
  }

  @Test def testTwoLittle0PaddingComponentFailure(): Unit = {
    val maskName = "GN2014BLP02-41"
    initGmos(Some(maskName))
    expectFormatFailure(maskName)
  }

  @Test def testTwoMuch0PaddingComponentFailure(): Unit = {
    val maskName = "GN2014BLP0002-41"
    initGmos(Some(maskName))
    expectFormatFailure(maskName)
  }

  @Test def testIteratorSuccess(): Unit = {
    initGmos(Some("GN2014BLP002-42"), Some("GN2014BLP002-43"))
    expectSuccess()
  }

  @Test def testIteratorFailure(): Unit = {
    val failMaskName = "GX2014BLP002-45"
    initGmos(Some("GN2014BLP002-44"), Some(failMaskName))
    expectFormatFailure(failMaskName)
  }

  @Test def testIteratorFailureShowsUpOnce(): Unit = {
    val failMaskName = "GX2014BLP002-46"
    initGmos(Some(failMaskName), Some("GX2014BLP002-47"))
    expectFormatFailure(failMaskName)
  }

  @Test def testBuiltInFpuInSequence(): Unit = {
    initGmos(Some("GN2014BLP002-48"), None)
    expectSuccess()
  }

  @Test def testBuiltInFpuInSequence2(): Unit = {
    initGmos(Some("GN2014BLP002-49"), None, Some("GN2014BLP002-50"))
    expectSuccess()
  }

  @Test def testMatchFailure(): Unit = {
    val maskName = "GN2014BLP003-51"
    initGmos(Some(maskName))
    expectMatchFailure(maskName)
  }

}
