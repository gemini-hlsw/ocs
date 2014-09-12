package edu.gemini.sp.vcs.log.impl

import edu.gemini.sp.vcs.log.{VcsEvent, VcsOp}
import edu.gemini.spModel.core.SPProgramID
import java.sql.{SQLException, Timestamp}
import scala.slick.driver.H2Driver.simple._
import java.io.File
import scala.slick.lifted.DDL
import java.util.logging.Logger
import edu.gemini.util.security.principal.GeminiPrincipal
import scala.slick.jdbc.{GetResult, StaticQuery => Q}

trait PersistentVcsSchema extends PersistentVcsMappers {

  // Implementor must provide a logger
  def Log: Logger

  // The idea here is that when we change the schema, we update this number and add a case to the upgradeFrom
  // function below. This may end up being difficult in practice but at least we have a mechanism to do it.
  val SchemaVersion = 4

  // These are DB-specific, sadly
  // http://www.h2database.com/javadoc/org/h2/constant/ErrorCode.html#c42102
  val DUPLICATE_KEY = 23505
  val TABLE_OR_VIEW_NOT_FOUND = 42102

  object VERSION extends Table[(Int)]("VERSION") {
    def value = column[Int]("VALUE")
    def * = value
  }

  object PRINCIPAL extends Table[(Id[GeminiPrincipal], String, String)]("PRINCIPAL") {
    def id     = column[Id[GeminiPrincipal]]("PRINCIPAL_ID", O.PrimaryKey, O.AutoInc)
    def clazz  = column[String]("CLASS") // Note that this is abbreviated; see PersistentVcsMappers
    def name   = column[String]("NAME")
    def *      = id ~ clazz ~ name
    def create = clazz ~ name returning id
    def idx    = index("PRINCIPAL_IDX", (clazz, name), unique = true)
  }

  object EVENT extends Table[(Id[VcsEvent], VcsOp, Timestamp, SPProgramID, String)]("EVENT") {
    def id        = column[Id[VcsEvent]]("EVENT_ID", O.PrimaryKey, O.AutoInc)
    def operator  = column[VcsOp]("OP")
    def timestamp = column[Timestamp]("TIMESTAMP")
    def pid       = column[SPProgramID]("PROGRAM_ID")
    def principalHash = column[String]("PRINCIPAL_HASH")
    def *         = id ~ operator ~ timestamp ~ pid ~ principalHash
    def create    = operator ~ timestamp ~ pid ~ principalHash returning id
  }

  object EVENT_PRINCIPAL extends Table[(Id[VcsEvent], Id[GeminiPrincipal])]("EVENT_PRINCIPAL") {
    def eventId     = column[Id[VcsEvent]]("EVENT_ID")
    def principalId = column[Id[GeminiPrincipal]]("PRINCIPAL_ID")
    def *           = eventId ~ principalId
    def fk1         = foreignKey("EVENT_PRINCIPAL_FK1", eventId, EVENT)(_.id)
    def fk2         = foreignKey("EVENT_PRINCIPAL_FK3", principalId, PRINCIPAL)(_.id)
    def idx         = index("EVENT_PRINCIPAL_IDX", (eventId, principalId), unique = true)
  }

  def ddl: DDL =
    Seq(VERSION, PRINCIPAL, EVENT, EVENT_PRINCIPAL).map(_.ddl).reduce(_ ++ _)

  def checkSchema(path:String)(implicit s: Session): Unit =
    try {
      VERSION.map(identity).first match {
        case SchemaVersion =>
          Log.info(s"Opened database with schema version $SchemaVersion on ${path}")
        case n =>
          Log.info(s"Opened database with obsolete schema version $n on ${path}; upgrading...")
          upgradeFrom(n)
          checkSchema(path)
      }
    } catch {
      case sqle: SQLException if sqle.getErrorCode == TABLE_OR_VIEW_NOT_FOUND =>
        Log.info("This is a new database. Creating schema...")
        ddl.create
        VERSION.insert(SchemaVersion)
    }

  def upgradeFrom(version: Int)(implicit s: Session): Unit =
    version match {
      // Newer versions here

      case 1 =>
        Q.updateNA("update EVENT set op = 'Fetch' where op = 'OpFetch'").execute()
        Q.updateNA("update EVENT set op = 'Store' where op = 'OpStore'").execute()
        Q.updateNA("update VERSION set VALUE = 2").execute()

      case 2 =>
        Log.warning("Major upgrade; dropping all event data.")
        Query(EVENT_PRINCIPAL).delete
        Query(EVENT).delete
        Q.updateNA("alter table EVENT add (PRINCIPAL_HASH varchar NOT NULL DEFAULT '')").execute()
        Q.updateNA("update VERSION set VALUE = 3").execute()

      case 3 =>
        Q.updateNA("alter table EVENT add (PRINCIPAL_HASH varchar NOT NULL DEFAULT '')").execute()
        Q.updateNA("update VERSION set VALUE = 4").execute()

      case n => sys.error(s"Don't know how to upgrade from version $version.")
    }

}
