package edu.gemini.util.security.auth.keychain

import edu.gemini.spModel.core.{SPProgramID,Affiliate}
import edu.gemini.util.security.principal._

import doobie.imports._

import java.sql.{SQLException, Timestamp}
import java.io.File
import java.security.Principal
import java.util.logging.Logger

import scalaz._, Scalaz._, effect.IO

object KeySchema2 extends KeyMappers2 {
  lazy val Log: Logger = java.util.logging.Logger.getLogger(KeySchema2.getClass.getName)

  // H2-specific SqlState
  val TABLE_OR_VIEW_NOT_FOUND = SqlState("42S02")

  // Current schema version
  val SchemaVersion = 1

  ////// HELPERS

  def info(s: String): ConnectionIO[Unit] =
    FC.delay(Log.info(s))

  def fail[A](s: String): ConnectionIO[A] =
    FC.delay(sys.error(s))

  ////// DATABASE OPERATIONS

  val createSchema: ConnectionIO[Unit] = 
    sql"""
      create table VERSION (
        VALUE INTEGER NOT NULL
      );
      create table KEYS (
        CLASS   VARCHAR NOT NULL,
        NAME    VARCHAR NOT NULL,
        HASH    VARCHAR NOT NULL,
        VERSION INTEGER NOT NULL
      );
      create unique index KEY_IDX on KEYS (CLASS,NAME)
    """.update.run.void

  def insertSchemaVersion(version: Int): ConnectionIO[Unit] =
    sql"insert into VERSION (VALUE) values ($version)".update.run.void

  val getSchemaVersion: ConnectionIO[Int] =
    sql"select VALUE from VERSION".query[Int].unique

  def checkSchema(path: String): ConnectionIO[Unit] =
    open(path) exceptSomeSqlState {
      case TABLE_OR_VIEW_NOT_FOUND => createNewDatabase
    }

  def createNewDatabase: ConnectionIO[Unit] =
    for {
      _ <- info("This is a new database. Creating schema...")
      _ <- createSchema
      _ <- insertSchemaVersion(SchemaVersion)
    } yield ()

  def open(path:String): ConnectionIO[Unit] =
    for {
      v <- getSchemaVersion
      _ <- info(s"Opened database with schema version $SchemaVersion on ${path}")
      _ <- (v != SchemaVersion).whenM(upgrade(v) >> checkSchema(path))
    } yield ()

  def upgrade(from: Int): ConnectionIO[Unit] =
    for {
      _ <- info(s"Upgrading from version ${from}...")
      _ <- from match {
        // case 1 => ...
        case n => fail("Can't upgrade from ${from}")
      }
    } yield ()

  def checkPass(p: GeminiPrincipal, pass: String): ConnectionIO[Option[KeyVersion]] =
    sql"""
      select VERSION
      from   KEYS
      where  CLASS = ${p.clazz}
      and    NAME  = ${p.getName}
      and    HASH  = ${hash(pass)}
    """.query[KeyVersion].option

  def setPass(p: GeminiPrincipal, pass: String): ConnectionIO[KeyVersion] = 
    getVersion(p) >>= (_.fold(insertPass(p, pass))(_ => updatePass(p, pass)))

  private def insertPass(p: GeminiPrincipal, pass: String): ConnectionIO[KeyVersion] =
    sql"""
      insert into KEYS (CLASS, NAME, HASH, VERSION)
      values (${p.clazz}, ${p.getName}, ${hash(pass)}, 1)
    """.update.run *> checkPass(p, pass).map(_.get) // hmm

  def updatePass(p: GeminiPrincipal, pass: String): ConnectionIO[KeyVersion] =
    sql"""
      update KEYS 
      set    HASH    = ${hash(pass)}, 
             VERSION = VERSION + 1
      where  CLASS = ${p.clazz} 
      and    NAME  = ${p.getName}
    """.update.run *> checkPass(p, pass).map(_.get) // hmm

  def getVersion(p: GeminiPrincipal): ConnectionIO[Option[KeyVersion]] =
    sql"""
      select VERSION
      from   KEYS
      where  CLASS = ${p.clazz}
      and    NAME  = ${p.getName}
    """.query[KeyVersion].option

  def revokeKey(p: GeminiPrincipal): ConnectionIO[Unit] =
    sql"""
      delete KEYS
      where  CLASS = ${p.clazz}
      and    NAME  = ${p.getName}
    """.update.run.void

  def backup(f: File): ConnectionIO[Unit] =
    for {
      p <- FC.delay(f.getAbsolutePath)
      _ <- info("Backing up key database to " + p)
      _ <- sql"BACKUP TO $p".update.run
    } yield ()

  def hash(s: String) = s // TODO

}

trait KeyMappers2 {

  // Indirection for principal type names
  object PrincipalType {
    val Affiliate = "Affiliate"
    val Program   = "Program"
    val User      = "User"
    val Staff     = "Staff"
    val Visitor   = "Visitor"
  }

  // Clazz extractor
  implicit class EnhanceGP(p: GeminiPrincipal) {
    def clazz: String = p match {
      case AffiliatePrincipal(n) => PrincipalType.Affiliate
      case ProgramPrincipal(pid) => PrincipalType.Program
      case UserPrincipal(n)      => PrincipalType.User
      case StaffPrincipal(n)     => PrincipalType.Staff
      case VisitorPrincipal(n)   => PrincipalType.Visitor
    }
  }

}
