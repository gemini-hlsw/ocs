package edu.gemini.spModel.io.impl.migration.to2016A

import edu.gemini.pot.sp.{SPComponentType, ISPObservation, ISPProgram}
import edu.gemini.spModel.gemini.gpi.Gpi
import edu.gemini.spModel.io.impl.migration.MigrationTest
import org.junit.{Assert, Test}
import scala.collection.JavaConverters._

class GpiUnblockedModesTest extends MigrationTest {

  @Test def testGpiConversion(): Unit =
    withTestProgram("gpi.xml", { (_,p) => validateProgram(p) })

  private def validateProgram(p: ISPProgram): Unit = {
    p.getAllObservations.asScala.foreach(validateObservation)
  }

  private def validateObservation(obs: ISPObservation): Unit = {

    val isUnblocked = Set(Gpi.ObservingMode.UNBLOCKED_Y,
      Gpi.ObservingMode.UNBLOCKED_H,
      Gpi.ObservingMode.UNBLOCKED_J,
      Gpi.ObservingMode.UNBLOCKED_K1,
      Gpi.ObservingMode.UNBLOCKED_K2)

    import edu.gemini.shared.util.immutable.ScalaConverters._

    val gpi = obs.getObsComponents.asScala.find(_.getType == SPComponentType.INSTRUMENT_GPI).map(_.getDataObject).collect {
      case g: Gpi => g
    }

    //check that the all unblocked GPI modes have useCal set to false (so they were indeed converted during migration)
    Assert.assertFalse(gpi.exists(g => g.getObservingMode.asScalaOpt.forall(isUnblocked) && g.isUseCal))

    //also ensure that unrelated modes aren't affected.
    Assert.assertTrue(gpi.filter(_.getObservingMode.getValue == Gpi.ObservingMode.CORON_Y_BAND).forall(_.isUseCal))
  }

}
