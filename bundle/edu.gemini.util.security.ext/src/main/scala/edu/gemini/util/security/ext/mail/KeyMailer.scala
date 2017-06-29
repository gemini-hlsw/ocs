package edu.gemini.util.security.ext.mail

import edu.gemini.spModel.core.Site
import edu.gemini.util.security.auth.keychain._
import java.io.File
import java.security.Principal
import scalaz._
import Scalaz._
import scalaz.effect.IO
import scalaz.effect.IO._
import javax.mail._
import javax.mail.internet._
import java.security.PublicKey
import java.util.logging.{ Logger, Level }
import scala.util.{ Success, Failure }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

abstract class KeyMailerImpl(site: Site, smtpHost: String) extends KeyMailer {

  /** Email a password to a user. */
  def notifyPassword(u: UserPrincipal, pass: String): IO[Unit] = IO {

    val sender = new InternetAddress <|
      (_.setAddress("noreply@gemini.edu")) <|
      (_.setPersonal(s"Gemini ODB (${site.abbreviation})"))

    val recipient = new InternetAddress <|
      (_.setAddress(u.getName)) <|
      (_.setPersonal("Gemini User"))

    val props = new java.util.Properties <|
      (_.put("mail.transport.protocol", "smtp")) <|
      (_.put("mail.smtp.host", smtpHost))

    val text =
      s"""|Your password has been updated for the Observing Database at ${site.displayName}.
          |Please use the following credentials to retrieve your key using the Observing Tool.
          |
          |   Database: ${site.displayName}
          |   User Key: ${u.getName}
          |   Password: ${pass}
          |
          |Please speak with your Gemini or NGO contact if you have questions about key retrieval.
          |""".stripMargin

    val session = Session.getInstance(props, null)
    new MimeMessage(session) <|
      (_.setFrom(sender)) <|
      (_.addRecipient(Message.RecipientType.TO, recipient)) <|
      (_.setSubject(s"Your ${site.displayName} password.")) <|
      (_.setContent(text, "text/plain"))

  } >>= send

  def send(m: MimeMessage): IO[Unit]

}

object KeyMailer {

  val Log = Logger.getLogger(getClass.getName)

  def apply(site: Site, smtpHost: String): KeyMailer =
    new KeyMailerImpl(site, smtpHost) {
      def send(msg: MimeMessage): IO[Unit] =
        IO {
          // OCSADV-85: use a future here; it can block for a long time if the mail server is down
          val recipients = msg.getRecipients(Message.RecipientType.TO).mkString(", ")
          Future(Transport.send(msg)).onComplete {
            case Success(_) => Log.info(s"Successfully sent mail to $recipients.")
            case Failure(e) => Log.log(Level.WARNING, s"Failed to send mail to $recipients", e)
          }
        }
    }

  /** Constructs a KeyMailer that prints mails to the console rather than actually sending them. */
  def forTesting(site: Site): KeyMailer =
    new KeyMailerImpl(site, "bogus.mail.host") {

      def send(msg: MimeMessage): IO[Unit] =
        for {
          _ <- putStrLn("")
          _ <- log(s"From: ${msg.getFrom.mkString(", ")}")
          _ <- log(s"To: ${msg.getRecipients(Message.RecipientType.TO).mkString(", ")}")
          _ <- log(s"Subject: ${msg.getSubject}")
          _ <- log("")
          _ <- msg.getContent.toString.lines.toList.traverseU(log)
        } yield ()

      def log(msg: String): IO[Unit] =
        putStrLn(s"TestMailer: $msg")

    }

}

