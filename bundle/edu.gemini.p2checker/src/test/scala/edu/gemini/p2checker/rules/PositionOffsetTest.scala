package edu.gemini.p2checker.rules

import edu.gemini.p2checker.api.{IRule, ObservationElements}
import edu.gemini.p2checker.rules.altair.AltairRule
import edu.gemini.p2checker.rules.gmos.GmosRule
import edu.gemini.p2checker.util.{NoPOffsetWithSlitRule, PositionOffsetChecker}
import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.spModel.gemini.altair.{AltairParams, InstAltair}
import edu.gemini.spModel.gemini.gmos.{GmosNorthType, GmosSouthType, InstGmosNorth, InstGmosSouth}
import edu.gemini.spModel.gemini.niri.InstNIRI
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffset
import org.junit.Assert._
import org.junit.{Before, Test}

import scala.collection.JavaConverters._

/**
 * Some support methods and test cases related to checks that deal with offset positions.
 * NOTE: Offsets in tests are in arcminutes!
 */
class PositionOffsetTest extends AbstractRuleTest {

  @Before def before() {
    prog = super.createBasicProgram("GN-2014B-LP-2")
  }

  private def initAltair(offsets: Pair[Double, Double]*): Unit = {
    val niri = addNiri()
    val niriDataObj = niri.getDataObject.asInstanceOf[InstNIRI]
    // Any other config necessary here?
    niri.setDataObject(niriDataObj)

    // Set up Altair with laser guide stars.
    val altair = addAltair(AltairParams.Mode.LGS)
    val altairDataObj = altair.getDataObject.asInstanceOf[InstAltair]
    // Any other config necessary here?
    altair.setDataObject(altairDataObj)

    // Add a target.
    addTargetObsCompAO()

    // Add offsets
    addOffsets(offsets:_*)
  }

  private def initGmosNorth(offsets: Pair[Double, Double]*): ISPObsComponent = {
    val gmos        = addGmosNorth()
    val gmosDataObj = gmos.getDataObject.asInstanceOf[InstGmosNorth]
    gmosDataObj.setFilter(GmosNorthType.FilterNorth.Ha_G0310) // avoid a different unrelated warning ...
    gmos.setDataObject(gmosDataObj)
    addSimpleScienceObserve()
    addOffsets(offsets:_*)
    gmos
  }

  private def setGmosNorthFpu(gmos: ISPObsComponent, fpu: GmosNorthType.FPUnitNorth): Unit = {
    val gmosDataObj = gmos.getDataObject.asInstanceOf[InstGmosNorth]
    gmosDataObj.setFPUnit(fpu)
    gmos.setDataObject(gmosDataObj)
  }

  private def initGmosSouth(offsets: Pair[Double, Double]*): ISPObsComponent = {
    val gmos        = addGmosSouth()
    val gmosDataObj = gmos.getDataObject.asInstanceOf[InstGmosSouth]
    gmosDataObj.setFilter(GmosSouthType.FilterSouth.Ha_G0336) // avoid a different unrelated warning ...
    gmos.setDataObject(gmosDataObj)
    addSimpleScienceObserve()
    addOffsets(offsets:_*)
    gmos
  }

  private def setGmosSouthFpu(gmos: ISPObsComponent, fpu: GmosSouthType.FPUnitSouth): Unit = {
    val gmosDataObj = gmos.getDataObject.asInstanceOf[InstGmosSouth]
    gmosDataObj.setFPUnit(fpu)
    gmos.setDataObject(gmosDataObj)
  }

  private def addOffsets(offsets: Pair[Double, Double]*): Unit = {
    // Create and add an offset iterator.
    val iter = addOffsetIterator()
    val seqDataObj = iter.getDataObject.asInstanceOf[SeqRepeatOffset]

    // Get the position list. It is an OffsetPosList[OffsetPos].
    val positionList = seqDataObj.getPosList

    // Now we add offset positions to the list.
    // addPosition takes arcsec, we need arcmin.
    offsets.foreach{ position => positionList.addPosition(position._1 * 60, position._2 * 60) }

    iter.setDataObject(seqDataObj)
  }

  // success is absence of problem message with given text
  private def expectSuccess(rule: IRule, msg: String): Unit = {
    val problems = applyRule(rule)
//    println("PROBLEMS: " + problems.mkString(", "))
    assertTrue(problems.filter(_ == msg).isEmpty)
  }

  // failure is presence of problem message with given text
  private def expectFailure(rule: IRule, msg: String): Unit = {
    val problems = applyRule(rule)
//    println("PROBLEMS: " + problems.mkString(", "))
    // Note that we want only ONE offset problem to be reported, regardless of the number of bad offsets.
    problems.filter(_ == msg) match {
      case msg :: Nil => // okay
      case _          => fail(problems.mkString(", "))
    }
  }

  private def applyRule(rule: IRule): List[String] = {
    val elems    = new ObservationElements(obs)
    rule.check(elems).getProblems.asScala.toList.map(_.getDescription)
  }

  @Test def testNoOffsetsSuccess(): Unit = {
    initAltair()
    expectSuccess(AltairRule.INSTANCE, PositionOffsetChecker.PROBLEM_MESSAGE)
  }

  @Test def testOneOffsetSuccess(): Unit = {
    initAltair((1.0, 2.0))
    expectSuccess(AltairRule.INSTANCE, PositionOffsetChecker.PROBLEM_MESSAGE)
  }

  @Test def testMultipleOffsetSuccess(): Unit = {
    initAltair((1.0, 2.0), (2.0, 1.0), (1.0, 1.0), (-1.0, -1.0))
    expectSuccess(AltairRule.INSTANCE, PositionOffsetChecker.PROBLEM_MESSAGE)
  }

  @Test def testOneOffsetFailure(): Unit = {
    initAltair((4.0, 5.0))
    expectFailure(AltairRule.INSTANCE, PositionOffsetChecker.PROBLEM_MESSAGE)
  }

  @Test def testOneNegativeOffsetFailure(): Unit = {
    initAltair((-4.0, -5.0))
    expectFailure(AltairRule.INSTANCE, PositionOffsetChecker.PROBLEM_MESSAGE)
  }

  // Check for offsets after pos 0 to make sure iteration is happening correctly.
  @Test def testMultipleOffsetFailure(): Unit = {
    initAltair((2.0, 3.0), (3.0, 2.0), (4.0, 5.0))
    expectFailure(AltairRule.INSTANCE, PositionOffsetChecker.PROBLEM_MESSAGE)
  }

  // ==== GMOS North/South: Warn if P offsets are used with slit spectroscopy
  import edu.gemini.spModel.gemini.gmos.GmosNorthType.{FPUnitNorth => GmosFpuNorth}
  import edu.gemini.spModel.gemini.gmos.GmosSouthType.{FPUnitSouth => GmosFpuSouth}
  val gmosRule = new GmosRule()
  val pOffsetInvalidMsg = NoPOffsetWithSlitRule.Message

  // no p-offset is always ok
  @Test def testGMOSNNoOffsetWithSlitOk(): Unit = {
    val gmos = initGmosNorth((0.0, 1.0))
    GmosFpuNorth.values().foreach { fpu =>
      setGmosNorthFpu(gmos, fpu)
      expectSuccess(gmosRule, pOffsetInvalidMsg)
    }
  }

  // warn if p-offset is set and we're doing slit spectroscopy
  @Test def testGMOSNOffsetWithSlitFailure(): Unit = {
    val gmos = initGmosNorth((1.0, 1.0))
    GmosFpuNorth.values().foreach { fpu =>
      setGmosNorthFpu(gmos, fpu)
      if (fpu.isSpectroscopic || fpu.isNSslit || fpu == GmosFpuNorth.CUSTOM_MASK)
        expectFailure(gmosRule, pOffsetInvalidMsg)
      else
        expectSuccess(gmosRule, pOffsetInvalidMsg)
    }
  }

  // no p-offset is always ok
  @Test def testGMOSSNoOffsetWithSlitOk(): Unit = {
    val gmos = initGmosSouth((0.0, 1.0))
    GmosFpuSouth.values().foreach { fpu =>
      setGmosSouthFpu(gmos, fpu)
      expectSuccess(gmosRule, pOffsetInvalidMsg)
    }
  }

  // warn if p-offset is set and we're doing slit spectroscopy
  @Test def testGMOSSOffsetWithSlitFailure(): Unit = {
    val gmos = initGmosSouth((1.0, 1.0))
    GmosFpuSouth.values().foreach { fpu =>
      setGmosSouthFpu(gmos, fpu)
      if (fpu.isSpectroscopic || fpu.isNSslit || fpu == GmosFpuSouth.CUSTOM_MASK)
        expectFailure(gmosRule, pOffsetInvalidMsg)
      else
        expectSuccess(gmosRule, pOffsetInvalidMsg)
    }
  }
}
