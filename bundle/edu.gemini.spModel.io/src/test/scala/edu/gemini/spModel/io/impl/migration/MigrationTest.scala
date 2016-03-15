package edu.gemini.spModel.io.impl.migration

import java.io.{FileReader, InputStreamReader}

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.spdb.{DBLocalDatabase, IDBDatabaseService}
import edu.gemini.spModel.io.impl.PioSpXmlParser

/**
 * Base trait for all migration tests
 */
trait MigrationTest {

  protected def withTestOdb[A](block: IDBDatabaseService => A): A = {
    val odb = DBLocalDatabase.createTransient()
    try {
      block(odb)
    } finally {
      odb.getDBAdmin.shutdown()
    }
  }

  protected def withTestProgram[A](programName: String, block: (IDBDatabaseService, ISPProgram) => A): A = withTestOdb { odb =>
    val parser = new PioSpXmlParser(odb.getFactory)
    parser.parseDocument(new InputStreamReader(getClass.getResourceAsStream(programName))) match {
      case p: ISPProgram => block(odb, p)
      case _             => sys.error("Expecting a science program")
    }
  }

  protected def withTestProgram2[A](programName: String)(block: ISPProgram => A): A =
    withTestProgram(programName, (_, p) => block(p))

}

/** Read a bunch of .xml files, reporting any errors. */
object MigrationTester {
  import java.io.File

  def main(args: Array[String]): Unit =
    args match {
      case Array(dir) => go(new File(dir))
      case _          => println("usage: MigrationTester <dir>")
    }

  private def go(dir: File): Unit = {
    val db = DBLocalDatabase.createTransient
    try {
      val parser = new PioSpXmlParser(db.getFactory)
      val files  = Option(dir.listFiles).getOrElse(Array.empty[File]).filter(_.getName.endsWith(".xml")).sortBy(_.getName)
      val total  = files.length
      files.zipWithIndex.foreach { case (f, n) =>
        print(s"[$n/$total] ${f.getName} ... ")
        try {
          parser.parseDocument(new FileReader(f))
          println(" [ok]")
        } catch {
          case e: Exception => println(s" ${Console.RED}[err] ${e.getMessage}${Console.RESET}")
        }
      }
    } finally {
      db.getDBAdmin.shutdown()
    }
  }

}
