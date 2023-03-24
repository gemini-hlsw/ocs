package edu.gemini.spModel.gemini.igrins2

import edu.gemini.spModel.gemini.gnirs.GNIRSParams.{CrossDispersed, PixelScale}
import edu.gemini.spModel.pio.ParamSet
import edu.gemini.spModel.pio.xml.{PioXmlFactory, PioXmlUtil}
import edu.gemini.spModel.telescope.{IssPort, PosAngleConstraint}
import org.junit.Assert._
import org.junit._

import java.io.{StringReader, StringWriter}
import scala.xml.Elem

class IOTest {

  val expTime: Elem = <paramset name="IGRINS-2" kind="dataObj">
    <param name="exposureTime" value="17.0"/>
    <param name="posAngle" value="15"/>
    <param name="posAngleConstraint" value="PARALLACTIC_OVERRIDE"/>
  </paramset>

  @Test def testExpTime(): Unit = {
    val pio = PioXmlUtil.read(new StringReader(expTime.toString))
    val pset = pio.asInstanceOf[ParamSet]
    val igrins2 = new Igrins2()
    igrins2.setParamSet(pset)
    assertEquals(17, igrins2.getExposureTime, 0.0)
  }

  @Test def testPosAngle(): Unit = {
    val pio = PioXmlUtil.read(new StringReader(expTime.toString))
    val pset = pio.asInstanceOf[ParamSet]
    val igrins2 = new Igrins2()
    igrins2.setParamSet(pset)
    assertEquals(15, igrins2.getPosAngleDegrees, 0.0)
  }

  val sideLooking: Elem = <paramset name="IGRINS-2" kind="dataObj">
    <param name="exposureTime" value="17.0"/>
    <param name="posAngle" value="15"/>
    <param name="posAngleConstraint" value="PARALLACTIC_OVERRIDE"/>
    <param name="issPort" value="SIDE_LOOKING" />
  </paramset>

  @Test def testIssPort(): Unit = {
    val pio = PioXmlUtil.read(new StringReader(sideLooking.toString))
    val pset = pio.asInstanceOf[ParamSet]
    val igrins2 = new Igrins2()
    igrins2.setParamSet(pset)
    assertEquals(IssPort.SIDE_LOOKING, igrins2.getIssPort)
  }

  val continuosSVC: Elem = <paramset name="IGRINS-2" kind="dataObj">
    <param name="exposureTime" value="17.0"/>
    <param name="posAngle" value="15"/>
    <param name="posAngleConstraint" value="PARALLACTIC_OVERRIDE"/>
    <param name="slitViewingCamera" value="CONTINUOUS" />
  </paramset>

  @Test def testSVC(): Unit = {
    val pio = PioXmlUtil.read(new StringReader(continuosSVC.toString))
    val pset = pio.asInstanceOf[ParamSet]
    val igrins2 = new Igrins2()
    igrins2.setParamSet(pset)
    assertEquals(SlitViewingCamera.CONTINUOUS, igrins2.getSlitViewingCamera)
  }

  val defaultValues =
    <paramset kind="dataObj" name="IGRINS2">
      <param value="30.0" name="exposureTime"/>
      <param value="0" name="posAngle"/>
      <param value="1" name="coadds"/>
      <param value="PARALLACTIC_ANGLE" name="posAngleConstraint"/>
      <param value="UP_LOOKING" name="issPort"/>
      <param value="ONE_IMAGE_EXPOSURE" name="slitViewingCamera"/>
    </paramset>

  @Test def testWrite(): Unit = {
    val igrins2 = new Igrins2
    val wtr = new StringWriter()
    val pset = igrins2.getParamSet(new PioXmlFactory())
    PioXmlUtil.write(pset, wtr)

    val node = scala.xml.XML.loadString(wtr.toString)
    assertEquals(defaultValues.buildString(true).replace(" ", ""),
      node.buildString(true).replace(" ", ""))
  }
}
