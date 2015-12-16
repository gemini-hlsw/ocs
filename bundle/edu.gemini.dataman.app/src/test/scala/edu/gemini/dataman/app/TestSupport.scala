package edu.gemini.dataman.app

import edu.gemini.gsa.query.Arbitraries
import edu.gemini.pot.sp.{ISPFactory, ISPProgram, ProgramGen}
import edu.gemini.pot.spdb.{DBLocalDatabase, IDBDatabaseService}
import edu.gemini.spModel.dataset.{DatasetRecord, DatasetGsaState, SummitState, DatasetQaState, Dataset, DatasetExecRecord, DatasetQaRecord, DatasetLabel}
import edu.gemini.spModel.obslog.ObsLog
import edu.gemini.util.security.principal.StaffPrincipal
import org.scalacheck.{Prop, Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import Arbitrary.arbitrary
import java.security.Principal
import java.time.Instant

import scala.collection.JavaConverters._

trait TestSupport extends Specification with ScalaCheck with Arbitraries {
  val User = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)

  import ProgramGen._

  // Generate a program edit that adds a new dataset to a random observation.
  val genAddDatasetRecord: Gen[ProgEdit] =
    for {
      qa  <- arbitrary[DatasetQaState]
      sum <- arbitrary[SummitState]
      gsa <- arbitrary[DatasetGsaState]
      arc <- Gen.oneOf(Some(gsa), None)
      fn  <- maybePickObservation
    } yield { (_: ISPFactory, p: ISPProgram) =>
      for {
        obs <- fn(p)
        log <- Option(ObsLog.getIfExists(obs))
      } {
        val lab   = new DatasetLabel(obs.getObservationID, log.getAllDatasetRecords.size() + 1)
        val dset  = new Dataset(lab, lab.toString, Instant.now().toEpochMilli)
        val qaRec = new DatasetQaRecord(lab, qa, "")
        val exRec = DatasetExecRecord(dset,sum, arc)

        log.qaLogDataObject.set(qaRec)
        log.execLogDataObject.getRecord.putDatasetExecRecord(exRec, null)

        obs.getObsQaLog.setDataObject(log.qaLogDataObject)
        obs.getObsExecLog.setDataObject(log.execLogDataObject)
      }
    }

  val genAddDatasetRecords: Gen[List[ProgEdit]] =
    Gen.sized { size => Gen.listOfN(size, genAddDatasetRecord) }

  // Generates a function that creates a random test program complete with
  // datasets.
  val genTestProg: Gen[ISPFactory => ISPProgram] =
    for {
      pCons <- genProg
      edits <- genAddDatasetRecords
    } yield { (fact: ISPFactory) =>
      val p = pCons(fact)
      edits.foreach { edit => edit(fact, p) }
      p
    }


  // Generates programs and populates a database with them.
  val genTestEnvironment: Gen[IDBDatabaseService => List[ISPProgram]] =
    for {
      num <- Gen.chooseNum(0, 5)
      fn  <- Gen.listOfN(num, genTestProg)
    } yield { (odb: IDBDatabaseService) =>
      val fact   = odb.getFactory
      val progs  = fn.map { f => f(fact) }
      // Nothing stops multiple programs in the list from having the same ID.
      // The database can't have two or more programs with the same ID.  Just
      // filter out any duplicates.
      val unique = progs.groupBy(_.getProgramID).mapValues(_.head).values.toList
      unique.foreach { odb.put }
      unique
    }

  // Extracts all the dataset records in the program into a single list.
  def allDatasets(progs: List[ISPProgram]): List[DatasetRecord] =
    for {
      p  <- progs
      o  <- p.getAllObservations.asScala
      dr <- Option(ObsLog.getIfExists(o)).toList.flatMap(_.getAllDatasetRecords.asScala)
    } yield dr


  def withTestOdb(block: IDBDatabaseService => Boolean): Boolean = {
    val odb = DBLocalDatabase.createTransient()
    try {
      block(odb)
    } finally {
      odb.getDBAdmin.shutdown()
    }
  }

  def forAllPrograms(test: (IDBDatabaseService, List[ISPProgram]) => Boolean): Prop = {
    implicit val arbTestEnvironment: Arbitrary[IDBDatabaseService => List[ISPProgram]] =
      Arbitrary { genTestEnvironment }

    Prop.forAll { (setup: (IDBDatabaseService => List[ISPProgram])) =>
      withTestOdb { odb => test(odb, setup(odb)) }
    }
  }
}
