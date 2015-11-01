package edu.gemini.dataman.app

import edu.gemini.dataman.core.Arbitraries
import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.pot.spdb.{IDBDatabaseService, DBLocalDatabase}
import edu.gemini.spModel.core.SPProgramID

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz.{-\/, \/-}

object PidFunctorSpec extends Specification with ScalaCheck with Arbitraries {
  protected def withTestOdb(block: IDBDatabaseService => Boolean): Boolean = {
    val odb = DBLocalDatabase.createTransient()
    try {
      block(odb)
    } finally {
      odb.getDBAdmin.shutdown()
    }
  }

  "PidFunctor" should {
    "find all pids" !
      forAll { (pids: Set[SPProgramID]) =>
        withTestOdb { odb =>
          val fact = odb.getFactory
          pids.foreach { pid =>
            odb.put(fact.createProgram(new SPNodeKey(), pid))
          }

          PidFunctor.exec(odb, null) match {
            case \/-(actualPids) =>
              actualPids.toSet == pids

            case -\/(f)          =>
              println(f.explain)
              false
          }
        }
      }
  }

}
