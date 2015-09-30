package edu.gemini.spModel.io.impl.migration

import java.io.InputStreamReader

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.spdb.{DBLocalDatabase, IDBDatabaseService}
import edu.gemini.spModel.io.impl.PioSpXmlParser
import org.junit.Assert._

/**
 * Base trait for all migration tests
 */
trait MigrationTest {

  protected def withTestOdb(block: IDBDatabaseService => Unit): Unit = {
    val odb = DBLocalDatabase.createTransient()
    try {
      block(odb)
    } finally {
      odb.getDBAdmin.shutdown()
    }
  }

  protected def withTestProgram(programName: String, block: (IDBDatabaseService, ISPProgram) => Unit): Unit = withTestOdb { odb =>
    val parser = new PioSpXmlParser(odb.getFactory)
    parser.parseDocument(new InputStreamReader(getClass.getResourceAsStream(programName))) match {
      case p: ISPProgram => block(odb, p)
      case _             => fail("Expecting a science program")
    }
  }

}
