package edu.gemini.util.security.auth.keychain

import scalaz._
import Scalaz._
import scalaz.effect.IO
import scala.slick.driver.H2Driver.simple._
import edu.gemini.util.security.principal._
import java.io.File

trait KeyDatabase {

  def checkPass(p: GeminiPrincipal, pass: String): IO[Option[KeyVersion]]

  def setPass(p: GeminiPrincipal, pass: String): IO[KeyVersion]

  def getVersion(p: GeminiPrincipal): IO[Option[KeyVersion]]

  def revokeKey(p: GeminiPrincipal): IO[Unit]

  def backup(backup: File): IO[Unit]

}

object KeyDatabase {

  def forTesting: IO[KeyDatabase] =
    IO(new KeyDatabase {

      private var data = Map[(String, String), (String, KeyVersion)]()

      def key(p: GeminiPrincipal): (String, String) =
        (p.getClass.getSimpleName, p.getName)

      def checkPass(p: GeminiPrincipal, pass: String): IO[Option[KeyVersion]] =
        IO(data.get(key(p)).filter(_._1 === pass).map(_._2))

      def setPass(p: GeminiPrincipal, pass: String): IO[KeyVersion] =
        for {
          v0 <- getVersion(p)
          v = v0.map(_ + 1).getOrElse(1)
          _ <- IO(data = data + (key(p) -> ((pass, v))))
        } yield v

      def getVersion(p: GeminiPrincipal): IO[Option[KeyVersion]] =
        IO(data.get(key(p)).map(_._2))

      def revokeKey(p: GeminiPrincipal): IO[Unit] =
        IO(data = data - key(p))

      def backup(backup: File): IO[Unit] =
        IO(())

    })

  /** Lift an IO action to run inside a Slick callback. */
  private def liftS[A](d: Database, f: Session => IO[A]): IO[A] =
    IO(d.withSession((s: Session) => f(s).unsafePerformIO))

  // as an experiment we will try a connection pool here
  private def db(url: String): IO[Database] =
    IO {
      val cpds = new com.mchange.v2.c3p0.ComboPooledDataSource();
      cpds.setDriverClass("org.h2.Driver")
      cpds.setJdbcUrl(url)
//      cpds.setUser("sa")
//      cpds.setPassword("")
      Database.forDataSource(cpds)
    }

  def apply(dir: File): IO[KeyDatabase] = {
    for {
      p <- IO(dir.getAbsolutePath) // can throw
      _ <- IO(require(dir.mkdirs() || dir.isDirectory, s"Not a valid directory: $p"))
      d <- db(s"jdbc:h2:$p/keydb;DB_CLOSE_ON_EXIT=FALSE;TRACE_LEVEL_FILE=4")
      x <- liftS(d, KeySchema.checkSchema(p))
    } yield new KeyDatabase {

      def checkPass(p: GeminiPrincipal, pass: String): IO[Option[KeyVersion]] =
        liftS(d, KeySchema.checkPass(p, pass))

      def setPass(p: GeminiPrincipal, pass: String): IO[KeyVersion] =
        liftS(d, KeySchema.setPass(p, pass))

      def getVersion(p: GeminiPrincipal): IO[Option[KeyVersion]] =
        liftS(d, KeySchema.getVersion(p))

      def revokeKey(p: GeminiPrincipal): IO[Unit] =
        liftS(d, KeySchema.revokeKey(p))

      def backup(backup: File): IO[Unit] =
        liftS(d, KeySchema.backup(backup))

    }
  }

}

