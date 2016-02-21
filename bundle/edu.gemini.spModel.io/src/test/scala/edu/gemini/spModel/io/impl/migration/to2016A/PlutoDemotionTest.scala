package edu.gemini.spModel.io.impl.migration.to2016A

import edu.gemini.pot.sp.{ISPObservation, ISPProgram, SPComponentType}
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.target.{SPTargetPio, SPTarget}
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.target.system.ConicTarget
import org.junit.{Assert, Test}

import scala.collection.JavaConverters._

class PlutoDemotionTest extends MigrationTest {

  @Test def testTemplateConversion(): Unit =
    withTestProgram("pluto.xml", { (_,p) => validateProgram(p) })

  private def validateProgram(p: ISPProgram): Unit = {
    p.getAllObservations.asScala.foreach(validateObservation)
  }

  private def validateObservation(obs: ISPObservation): Unit = {
    obs.getObsComponents.asScala
      .find(_.getType == SPComponentType.TELESCOPE_TARGETENV).get
      .getDataObject.asInstanceOf[TargetObsComp]
      .getTargetEnvironment
      .getBase match {

      case sp: SPTarget =>

        sp.getConicTarget match {

          case Some(ct) => // we know system is correct

            // These are preserved from the fixture
            Assert.assertEquals("Name", "Pluto", ct.getName)
            Assert.assertEquals("ValidAt", "10/28/15 7:39:58 PM UTC", SPTargetPio.formatter.format(ct.getDateForPosition))
            Assert.assertEquals("RA", "15:41:38.380", ct.getRa.toString)
            Assert.assertEquals("Dec", "-15:52:28.70", ct.getDec.toString)

            // These are all new
            Assert.assertEquals("Epoch", 2457217.5, ct.getEpoch.getValue, 0.00001)
            Assert.assertEquals("ANode", 110.2100007229519, ct.getANode.getValue, 0.00001)
            Assert.assertEquals("AQ", 39.74409337717218, ct.getAQ.getValue, 0.00001)
            Assert.assertEquals("E", 0.2543036816945946, ct.getE, 0.00001)
            Assert.assertEquals("Inclination", 17.36609399010031, ct.getInclination.getValue, 0.00001)
            Assert.assertEquals("LM", 0.0, ct.getLM.getValue, 0.00001)
            Assert.assertEquals("N", 0.0, ct.getN.getValue, 0.00001)
            Assert.assertEquals("Perihelion", 114.2248220688449, ct.getPerihelion.getValue, 0.00001)
            Assert.assertEquals("Epoch of Perihelion", 2447885.60548777, ct.getEpochOfPeri.getValue, 0.00001)

          case None => Assert.fail("Expected ConicTarget, found " + sp.getTarget)

        }
    }
  }

}
