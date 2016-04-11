package edu.gemini.pot.sp

import edu.gemini.pot.spdb.{DBLocalDatabase, IDBDatabaseService}
import edu.gemini.spModel.target.env.Arbitraries
import org.scalacheck.{Prop, Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Utility for writing test cases that use ProgramGen. */
trait ProgramTestSupport extends Specification with ScalaCheck with Arbitraries {

  def genTestProg: Gen[ISPFactory => ISPProgram]

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
