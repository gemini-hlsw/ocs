package edu.gemini.spModel.io.impl.migration.to2016A

import edu.gemini.pot.sp.{ISPObservation, ISPProgram}
import edu.gemini.spModel.dataset.{DatasetQaState, SummitState, DatasetExecRecord}
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.obslog.ObsLog
import org.junit.{Assert, Test}

import scala.collection.JavaConverters._

class DatasetMigrationTest extends MigrationTest {
  @Test def testDatasetMigration(): Unit =
    withTestProgram("datasetMigration.xml", { (_,p) => validateProgram(p) })

  private def validateProgram(p: ISPProgram): Unit =
    p.getAllObservations.asScala.foreach(validateObservation)

  private def validateObservation(obs: ISPObservation): Unit = {
    val obsLogOpt = Option(ObsLog.getIfExists(obs))
    Assert.assertTrue(obsLogOpt.isDefined)

    obsLogOpt.foreach { obsLog =>
      val actual = obsLog.getAllDatasetRecords.asScala.map(_.exec).collect {
        case DatasetExecRecord(ds, SummitState.Missing(qa), None) => ds.getIndex -> qa
      }.sortBy(_._1).toList

      val expected = List(
        (1, DatasetQaState.PASS),
        (2, DatasetQaState.USABLE),
        (3, DatasetQaState.PASS),
        (4, DatasetQaState.UNDEFINED)
      )

      Assert.assertEquals(expected, actual)
    }
  }

}
