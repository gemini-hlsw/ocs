package edu.gemini.util.security.auth.keychain

import java.io.File
import scalaz._
import Scalaz._
import scalaz.effect.IO

import java.security.KeyPair
import edu.gemini.util.security.principal._
import edu.gemini.util.security.auth.DSA

import java.util.logging.Logger

trait KeyMailer {
  def notifyPassword(u: UserPrincipal, pass: String): IO[Unit]
}

class KeyServer private (keyPair: KeyPair, mailer: KeyMailer, db: KeyDatabase) { ks =>
  private val logger = Logger.getLogger(getClass.getName)

  /** Action to retrieve the key for the specified principal using the given password. */
  def tryKey(principal: GeminiPrincipal, pass: String): Action[Key] =
    for {
      v0 <- db.checkPass(principal, pass).liftIO[Action]
      v1 <- v0.fold[Action[Int]](Action.fail(KeyFailure.InvalidPassword))(Action.apply(_))
      k  <- Key.sign(keyPair.getPrivate, principal, v1)
    } yield k

  /** Action to validates the given key. May fail with `InvalidSignature` or `InvalidVersion`. */
  def validateKey(key: Key): Action[Unit] =
    for {
      _ <- key.verify(keyPair.getPublic).leftMap(KeyFailure.InvalidSignature).fold(Action.fail(_), Action.apply(_))
      _ <- validateVersion(key.get._1, key.get._2)
    } yield ()

  /** Action to validates the given key version. May fail with `InvalidVersion`. */
  def validateVersion(p: GeminiPrincipal, expected: KeyVersion): Action[Unit] =
    for {
      v <- getVersion(p)
      _ <- v.fold(false)(_ == expected).unlessM(Action.fail(KeyFailure.InvalidVersion))
    } yield ()

  /** Action to return this principal's key version (if any). */
  def getVersion(p: GeminiPrincipal): Action[Option[KeyVersion]] =
    db.getVersion(p).liftIO[Action]

  /** Action to change the password for the given GeminiPrincipal. */
  def setPassword(p: GeminiPrincipal, pass: String): Action[Key] =
    for {
      v <- db.setPass(p, pass).liftIO[Action]
      k <- Key.sign(keyPair.getPrivate, p, v)
    } yield k

  /** Action to set a user password and notify via email. */
  def setPasswordAndNotify(u: UserPrincipal, pass: String): Action[Key] =
    for {
      k <- setPassword(u, pass)
      _ <- mailer.notifyPassword(u, pass).liftIO[Action]
    } yield k

  /** Action to set a user's password to a random value and notify via email. */
  def resetPasswordAndNotify(u: UserPrincipal): Action[Unit] =
    for {
      p <- randomPassword
      k <- setPasswordAndNotify(u, p)
    } yield ()


  /** Action to revoke a ket. */
  def revokeKey(p: GeminiPrincipal): Action[Unit] =
    db.revokeKey(p).liftIO[Action]

  /** Action to generate a random password. */
  def randomPassword: Action[String] =
    IO(List.fill(6)(util.Random.nextInt(10)).mkString).liftIO[Action]

  /** Action to back up the keyserver. */
  def backup(backup: File): Action[Unit] =
    db.backup(backup: File).liftIO[Action]

  /**
   * A KeyService interface for publication. We're doing this rather than extending KeyService to
   * prevent malicious use via downcasting.
   */
  object keyService extends KeyService {

    /** Action to retrieve the key for the specified principal using the given password. */
    def tryKey(principal: GeminiPrincipal, pass: String): \/[KeyFailure, Key] =
      (IO(logger.info("attempt to validate key")) *> ks.tryKey(principal, pass).run).unsafePerformIO

    /** Action to validates the given key. May fail with `InvalidSignature` or `InvalidVersion`. */
    def validateKey(key: Key): \/[KeyFailure, Unit] =
      (IO(logger.info("attempt to validate key")) *> ks.validateKey(key).run).unsafePerformIO

    /** Action to set a user's password to a random value and notify via email. */
    def resetPasswordAndNotify(u: UserPrincipal): \/[KeyFailure, Unit] =
      ks.resetPasswordAndNotify(u).run.unsafePerformIO

  }

}


object KeyServer extends DSA {

  def apply(keyPair: KeyPair, mailer: KeyMailer, db: KeyDatabase): IO[KeyServer] =
    IO(new KeyServer(keyPair, mailer, db))

  def apply(dir: File, mailer: KeyMailer): IO[KeyServer] =
    for {
      po <- PersistentObject[KeyPair](new File(dir, "keypair.bin"), keyPairGenerator.generateKeyPair)
      kp <- po.get
      kd <- KeyDatabase.apply(dir)
      ks <- apply(kp, mailer, kd)
    } yield ks

}

