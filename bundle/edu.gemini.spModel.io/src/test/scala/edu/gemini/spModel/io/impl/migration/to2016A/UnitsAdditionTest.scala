package edu.gemini.spModel.io.impl.migration.to2016A

import edu.gemini.pot.sp.{ISPObservation, ISPProgram, SPComponentType}
import edu.gemini.spModel.core.EmissionLine
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.target.obsComp.TargetObsComp
import org.junit.{Assert, Test}
import squants.motion.KilometersPerSecond
import squants.radio.{WattsPerSquareMeter, WattsPerSquareMeterPerMicron}

import scala.collection.JavaConverters._

// Verifies that as part of the migration to 2016A squants quantities have their units added
class UnitsAdditionTest extends MigrationTest {

  // Simple conversion from 2015B model.
  @Test
  def testTemplateConversion(): Unit =
    withTestProgram("GN-2016A-Q-1.xml", { (_,p) => validateProgram(p) })

  private def validateProgram(p: ISPProgram): Unit = {
    p.getAllObservations.asScala.foreach(validateObservation)
  }

  private def validateObservation(obs: ISPObservation): Unit = {
    val targetComp = obs.getObsComponents.asScala.find(_.getType == SPComponentType.TELESCOPE_TARGETENV).get
    val toc        = targetComp.getDataObject.asInstanceOf[TargetObsComp]

    toc.getAsterism.allSpTargets.head.getSpectralDistribution match {
      // Check that units have been properly added during migration;
      // without migration we wouldn't get the right values here
      case Some(EmissionLine(_, width, flux, continuum)) =>
        Assert.assertEquals(510, width.value, 1e-3)
        Assert.assertEquals(KilometersPerSecond, width.unit)
        Assert.assertEquals(5.4e-19, flux.value, 1e-20)
        Assert.assertEquals(WattsPerSquareMeter, flux.unit)
        Assert.assertEquals(1.4e-16, continuum.value, 1e-17)
        Assert.assertEquals(WattsPerSquareMeterPerMicron, continuum.unit)
      case _ => // not interested in any other case
    }
  }

}
