package edu.gemini.spModel.io.impl.migration.to2016A

import edu.gemini.pot.sp.{ISPObservation, ISPProgram, SPComponentType}
import edu.gemini.spModel.core.NonSiderealTarget
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.target.{SPTargetPio, SPTarget}
import edu.gemini.spModel.target.obsComp.TargetObsComp
import org.junit.{Assert, Test}

import scala.collection.JavaConverters._

class PlutoDemotionTest extends MigrationTest {

  @Test def testTemplateConversion(): Unit =
    withTestProgram("pluto.xml", { (_,p) => validateProgram(p) })

  private def validateProgram(p: ISPProgram): Unit = {
    p.getAllObservations.asScala.foreach(validateObservation)
  }

  private def validateObservation(obs: ISPObservation): Unit =
    obs.getObsComponents.asScala
      .find(_.getType == SPComponentType.TELESCOPE_TARGETENV).get
      .getDataObject.asInstanceOf[TargetObsComp]
      .getTargetEnvironment
      .getAsterism.allTargets.head match {
          case NonSiderealTarget("Pluto", e, None, Nil, None, None) =>
            e.toList match {
              case List((t, c)) =>

                  Assert.assertEquals("ValidAt", "10/28/15 7:39:58 PM UTC", SPTargetPio.formatter.format(t))
                  Assert.assertEquals("RA",      "15:41:38.379", c.ra.toAngle.formatHMS)
                  Assert.assertEquals("Dec",     "-15:52:28.70", c.dec.formatDMS)

              case e => Assert.fail("Expected 1-element ephemeris, found " + e)
            }
          case t => Assert.fail("Expected Pluto, found " + t)
        }

}
