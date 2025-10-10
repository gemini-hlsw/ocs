package edu.gemini.util.security.auth.keychain

import scalaz._
import Scalaz._
import scalaz.effect.IO
import edu.gemini.util.security.principal._
import java.io.File
import java.util.logging.Logger

trait KeyDatabase {

  def checkPass(p: GeminiPrincipal, pass: String): IO[Option[KeyVersion]]

  def setPass(p: GeminiPrincipal, pass: String): IO[KeyVersion]

  def getVersion(p: GeminiPrincipal): IO[Option[KeyVersion]]

  def revokeKey(p: GeminiPrincipal): IO[Unit]

  def backup(backup: File): IO[Unit]

}

object KeyDatabase {
  private val logger = Logger.getLogger(getClass.getName)

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

  def apply(dir: File): IO[KeyDatabase] = {
    import doobie.imports._
    for {
      p <- IO(dir.getAbsolutePath) // can throw
      _ <- IO(require(dir.mkdirs() || dir.isDirectory, s"Not a valid directory: $p"))
      d = DriverManagerTransactor[IO]("org.h2.Driver", s"jdbc:h2:$p/keydb;DB_CLOSE_ON_EXIT=FALSE;TRACE_LEVEL_FILE=4", "", "")
      _ <- IO(logger.info(s"Using key database at $p"))
      x <- KeySchema2.checkSchema(p).transact(d)
    } yield new KeyDatabase {

      def checkPass(p: GeminiPrincipal, pass: String): IO[Option[KeyVersion]] =
        KeySchema2.checkPass(p, pass).transact(d)

      def setPass(p: GeminiPrincipal, pass: String): IO[KeyVersion] =
        KeySchema2.setPass(p, pass).transact(d)

      def getVersion(p: GeminiPrincipal): IO[Option[KeyVersion]] =
        KeySchema2.getVersion(p).transact(d)

      def revokeKey(p: GeminiPrincipal): IO[Unit] =
        KeySchema2.revokeKey(p).transact(d)

      def backup(backup: File): IO[Unit] =
        KeySchema2.backup(backup).transact(d)

    }
  }

}

