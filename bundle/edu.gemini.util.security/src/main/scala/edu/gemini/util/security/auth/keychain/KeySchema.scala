package edu.gemini.util.security.auth.keychain

import java.sql.{SQLException, Timestamp}
import scala.slick.driver.H2Driver.simple._
import java.io.File
import scala.slick.lifted.DDL
import java.util.logging.Logger
import edu.gemini.util.security.principal._
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scalaz._
import Scalaz._
import scalaz.effect._
import scalaz.effect.kleisliEffect._
import edu.gemini.spModel.core.{SPProgramID,Affiliate}
import java.security.Principal
import scala.slick.jdbc.{GetResult, StaticQuery => Q}

object KeySchema extends KeyMappers {

  lazy val Log: Logger = java.util.logging.Logger.getLogger(KeySchema.getClass.getName)

  // These are DB-specific, sadly
  // http://www.h2database.com/javadoc/org/h2/constant/ErrorCode.html#c42102
  val DUPLICATE_KEY = 23505
  val TABLE_OR_VIEW_NOT_FOUND = 42102

  ////// SCHEMA

  val SchemaVersion = 1

  object VERSION extends Table[(Int)]("VERSION") {
    def value = column[Int]("VALUE")
    def *     = value
  }

  object KEYS extends Table[(String, String, String, Int)]("KEYS") {
    def clazz    = column[String]("CLASS") // Note that this is abbreviated; see KeyMappers
    def name     = column[String]("NAME")
    def hash     = column[String]("HASH")
    def version  = column[Int]("VERSION", O.Default(1))
    def *        = clazz ~ name ~ hash ~ version
    def create   = clazz ~ name ~ hash
    def idx      = index("KEY_IDX", (clazz, name), unique = true)
  }

  def ddl: DDL =
    Seq(VERSION, KEYS).map(_.ddl).reduce(_ ++ _)

  ////// MONAD FOR SLICK OPERATIONS

  type Slick[+A] = Kleisli[IO, Session, A]

  object Slick {
    def apply[A](f: Session => A): Slick[A] = Kleisli(s => IO(f(s)))
    def unit[A](a: => A): Slick[A] = apply(_ => a)
    def session: Slick[Session] = Kleisli.ask[IO, Session]
  }
  
  implicit class SlickOps[A](a: Slick[A]) {
    def except[B >: A](b: Throwable => Slick[B]): Slick[B] =
      Slick.session >>= { s => 
        a(s).except { 
          case t: Throwable => b(t)(s) 
        } .liftIO[Slick] 
      }
  }

  ////// OPERATIONS

  def checkSchema(path: String): Slick[Unit] =
    open(path) except {
      case e: SQLException if e.getErrorCode === TABLE_OR_VIEW_NOT_FOUND => create
      case e => Slick(throw e)
    }

  def create: Slick[Unit] =
    for {
      _ <- Slick.unit(Log.info("This is a new database. Creating schema..."))
      _ <- Slick(ddl.create(_))
      _ <- Slick(VERSION.insert(SchemaVersion)(_))
    } yield ()

  def open(path:String): Slick[Unit] = 
    for {
      v <- Slick(implicit s => VERSION.map(identity).first)
      _ <- Slick.unit(Log.info(s"Opened database with schema version $SchemaVersion on ${path}"))
      _ <- (v != SchemaVersion).whenM(upgrade(v) >> checkSchema(path))
    } yield ()

  def upgrade(from: Int): Slick[Unit] =
    for {
      _ <- Slick.unit(Log.info(s"Upgrading from version ${from}..."))
      _ <- from match {
     // case 1 => ...
        case n => Slick(sys.error("Can't upgrade from ${from}"))
      }
    } yield ()

  def checkPass(p: GeminiPrincipal, pass: String): Slick[Option[KeyVersion]] =
    Slick(Query(KEYS).filter { k => 
        k.clazz === p.clazz   &&
        k.name  === p.getName &&
        k.hash  === hash(pass)
      } .map(_.version).firstOption(_))

  def setPass(p: GeminiPrincipal, pass: String): Slick[KeyVersion] = 
    getVersion(p) >>= (_.fold(insert(p, pass))(_ => update(p, pass)))

  def insert(p: GeminiPrincipal, pass: String): Slick[KeyVersion] =
    Slick(KEYS.create.insert((p.clazz, p.getName, hash(pass)))(_))

  def update(p: GeminiPrincipal, pass: String): Slick[KeyVersion] =
    for {
      _ <- Slick { s =>
        Q.update[(String,String,String)]("""
          UPDATE KEYS SET HASH = ?, 
          VERSION = VERSION + 1
          WHERE CLASS = ? AND NAME = ?
          """).execute((hash(pass), p.clazz, p.getName))(s)
      }
      v <- getVersion(p)
    } yield v.get // YOLO

  def getVersion(p: GeminiPrincipal): Slick[Option[KeyVersion]] =
    Slick(Query(KEYS).filter { k => 
        k.clazz === p.clazz   &&
        k.name  === p.getName
      } .map(_.version).firstOption(_))

  def revokeKey(p: GeminiPrincipal): Slick[Unit] =
    Slick(Query(KEYS).filter { k =>
      k.clazz === p.clazz   &&
        k.name  === p.getName
    }.delete(_)).map(_ => ())

  def hash(s:String) = s // TODO

  def backup(f: File): Slick[Unit] =
    for {
      p <- Slick.unit(f.getAbsolutePath)
      _ <- Slick.unit(Log.info("Backing up key database to " + p))
      _ <- Slick { implicit s => Q.update[String]("BACKUP TO ?").execute(p) }
    } yield ()

}



trait KeyMappers {

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
