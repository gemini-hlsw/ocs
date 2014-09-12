package edu.gemini.p2checker.rules

import edu.gemini.p2checker.api.ObservationElements
import edu.gemini.p2checker.rules.altair.AltairRule
import edu.gemini.p2checker.util.PositionOffsetChecker
import edu.gemini.spModel.gemini.altair.{InstAltair, AltairParams}
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffset

import org.junit.Assert._
import org.junit.{Test, Before}
import scala.collection.JavaConverters._
import edu.gemini.spModel.gemini.gmos.{GmosNorthType, InstGmosNorth}
import edu.gemini.spModel.gemini.niri.InstNIRI

/**
 * Created with IntelliJ IDEA.
 * User: sraaphor
 * Date: 4/15/14
 * Time: 10:03 AM
 * To change this template use File | Settings | File Templates.
 */
class PositionOffsetCheckerTest extends AbstractRuleTest {

  @Before def before() {
    prog = super.createBasicProgram("GN-2014B-LP-2")
  }

  private def initAltair(offsets: Pair[Double, Double]*): Unit = {
    // Set up GMOS-N as the primary instrument.
    //val gmos        = addGmosNorth()
    //val gmosDataObj = gmos.getDataObject.asInstanceOf[InstGmosNorth]
    //gmosDataObj.setFilter(GmosNorthType.FilterNorth.Ha_G0310) // avoid a different unrelated warning ...
    //gmos.setDataObject(gmosDataObj)

    val niri        = addNiri()
    val niriDataObj = niri.getDataObject.asInstanceOf[InstNIRI]
    // Any other config necessary here?
    niri.setDataObject(niriDataObj)

    // Set up Altair with laser guide stars.
    val altair = addAltair(AltairParams.Mode.LGS)
    val altairDataObj = altair.getDataObject.asInstanceOf[InstAltair]
    // Any other config necessary here?
    altair.setDataObject(altairDataObj)

    // Add a target.
    addTargetObsCompAO

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

  private def expectSuccess(): Unit = {
    val rule     = AltairRule.INSTANCE
    val elems    = new ObservationElements(obs)
    val problems = rule.check(elems).getProblems.asScala.toList.map(_.getDescription)
    println("PROBLEMS: " + problems.mkString(", "))
    assertTrue(problems.filter(_ == PositionOffsetChecker.PROBLEM_MESSAGE).isEmpty)
  }

  private def expectFailure(): Unit = {
    val rule     = AltairRule.INSTANCE
    val elems    = new ObservationElements(obs)
    val problems = rule.check(elems).getProblems.asScala.toList.map(_.getDescription)

    println("PROBLEMS: " + problems.mkString(", "))
    // Note that we want only ONE offset problem to be reported, regardless of the number of bad offsets.
    problems.filter(_ == PositionOffsetChecker.PROBLEM_MESSAGE) match {
      case PositionOffsetChecker.PROBLEM_MESSAGE :: Nil => // okay
      case _                                            => fail(problems.mkString(", "))
    }
  }

  @Test def testNoOffsetsSuccess(): Unit = {
    initAltair()
    expectSuccess()
  }

  @Test def testOneOffsetSuccess(): Unit = {
    initAltair((1.0, 2.0))
    expectSuccess()
  }

  @Test def testMultipleOffsetSuccess(): Unit = {
    initAltair((1.0, 2.0), (2.0, 1.0), (1.0, 1.0), (-1.0, -1.0))
    expectSuccess()
  }

  @Test def testOneOffsetFailure(): Unit = {
    initAltair((4.0, 5.0))
    expectFailure()
  }

  @Test def testOneNegativeOffsetFailure(): Unit = {
    initAltair((-4.0, -5.0))
    expectFailure()
  }

  // Check for offsets after pos 0 to make sure iteration is happening correctly.
  @Test def testMultipleOffsetFailure(): Unit = {
    initAltair((2.0, 3.0), (3.0, 2.0), (4.0, 5.0))
    expectFailure()
  }
}
