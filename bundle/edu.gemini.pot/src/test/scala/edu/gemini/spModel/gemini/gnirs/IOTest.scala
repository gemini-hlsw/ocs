package edu.gemini.spModel.gemini.gnirs

import org.junit._
import Assert._
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.{CrossDispersed, PixelScale}
import edu.gemini.spModel.pio.ParamSet
import edu.gemini.spModel.pio.xml.{PioXmlFactory, PioXmlUtil}
import java.io.{StringWriter, StringReader}

class IOTest {

  val sxd = <paramset name="GNIRS" kind="dataObj">
              <param name="exposureTime" value="17.0"/>
              <param name="posAngle" value="0"/>
              <param name="coadds" value="1"/>
              <param name="pixelScale" value="PS_015"/>
              <param name="disperser" value="D_32"/>
              <param name="slitWidth" value="SW_4"/>
              <param name="crossDispersed" value="YES"/>
              <param name="wollastonPrism" value="NO"/>
              <param name="readMode" value="BRIGHT"/>
              <param name="wellDepth" value="SHALLOW"/>
              <param name="centralWavelength" value="2.2"/>
            </paramset>

  @Test def testSxd() { verifyXd(sxd, CrossDispersed.SXD)}

  val lxd = <paramset name="GNIRS" kind="dataObj">
              <param name="exposureTime" value="17.0"/>
              <param name="posAngle" value="0"/>
              <param name="coadds" value="1"/>
              <param name="pixelScale" value="PS_005"/>
              <param name="disperser" value="D_32"/>
              <param name="slitWidth" value="SW_4"/>
              <param name="crossDispersed" value="YES"/>
              <param name="wollastonPrism" value="NO"/>
              <param name="readMode" value="BRIGHT"/>
              <param name="wellDepth" value="SHALLOW"/>
              <param name="centralWavelength" value="2.2"/>
            </paramset>

  @Test def testLxd() { verifyXd(lxd, CrossDispersed.LXD)}

  private def verifyXd(node: scala.xml.Node, xd: CrossDispersed) {
    val pio  = PioXmlUtil.read(new StringReader(node.toString))
    val pset = pio.asInstanceOf[ParamSet]
    val gnirs = new InstGNIRS()
    gnirs.setParamSet(pset)
    assertEquals(xd, gnirs.getCrossDispersed)
  }

  @Test def testWrite() {
    CrossDispersed.values.foreach(testWrite _)
  }

  def testWrite(xd: CrossDispersed) {
    val gnirs = new InstGNIRS();
    gnirs.setPixelScale(PixelScale.PS_005)
    gnirs.setCrossDispersed(xd)

    val wtr  = new StringWriter()
    val pset = gnirs.getParamSet(new PioXmlFactory())
    PioXmlUtil.write(pset, wtr)

    val node = scala.xml.XML.loadString(wtr.toString)
    val xdStr = ((node \\ "param").find(n => (n \ "@name").text == "crossDispersed").get \ "@value").text
    assertEquals(xd.name, xdStr)
  }
}