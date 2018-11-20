package edu.gemini.dbTools.mailer


import edu.gemini.dbTools.mailer.MailerType._
import edu.gemini.spModel.core.Site

import java.util.logging.{ Logger, Level }

import javax.mail._
import javax.mail.Message.RecipientType.TO
import javax.mail.internet._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaz._
import Scalaz._

import scalaz.effect.IO
import scalaz.effect.IO._

/**
 * Mailer is used to send emails from cron jobs.  There are three flavors,
 * development, test, and production obtained from `Mailer.forDevelopment`,
 * `Mailer.forTesting`, and `Mailer.apply` respectively.  The development
 * version simply logs the message that would have been emailed.  The test
 * version alters the content with a "test only" disclaimer and strips the
 * message of non-gemini emails.  It is meant to be used during the pre-release
 * test month.  The production version sends the mail as requested.
 */
sealed abstract class Mailer(val site: Site, val smtpHost: String) {

  /**
   * Creates a program that will send the given message with the given subject
   * to the given recipients, logging along the way.  Once the client has a
   * Mailer instance via one of the constructors, this method is the entire API.
   */
  def sendText(
    to:      List[InternetAddress],
    subject: String,
    message: String
  ): IO[Unit] = IO {

    val sender = new InternetAddress       <|
      (_.setAddress("noreply@gemini.edu")) <|
      (_.setPersonal(s"Gemini ODB (${site.abbreviation})"))

    SafeMimeMessage(sender, to, subject, message)

  } >>= send

  // Left abstract so that the various types of mailer (production, test, dev)
  // can take the appropriate action.
  protected def send(m: SafeMimeMessage): IO[Unit]

  protected final def sendAsync(m: SafeMimeMessage): IO[Unit] = IO {
    import Mailer.Log

    // OCSADV-85: use a future here; it can block for a long time if the mail server is down
    if (m.to.isEmpty) {
      Log.info(s"""Cannot send mail "${m.subject}" because there are no recipients.""")
    } else {
      val to = m.to.mkString(", ")
      Future(Transport.send(m.toMimeMessage(smtpHost))).onComplete {
        case Success(_) => Log.info(s"""Successfully sent mail "${m.subject}" to $to.""")
        case Failure(e) => Log.log(Level.WARNING, s"""Failed to send mail "${m.subject}" to $to.""", e)
      }
    }
  }
}

object Mailer {

  val Log = Logger.getLogger(getClass.getName)

  // Logs the message.
  private def write(prefix: String, m: SafeMimeMessage): IO[Unit] = IO {
    Log.info(
       s"""$prefix
         |
         |From...: ${m.from}
         |To.....: ${m.to.mkString(", ")}
         |Subject: ${m.subject}
         |
         |${m.content}
       """.stripMargin
    )
  }

  def apply(site: Site, smtpHost: String): Mailer =
    new Mailer(site, smtpHost) {
      override def send(m: SafeMimeMessage): IO[Unit] =
        write("", m) *> sendAsync(m)
    }

  def forTesting(site: Site, smtpHost: String): Mailer =
    new Mailer(site, smtpHost) {
      private def isGeminiAddress(a: Address): Boolean =
        a match {
          case ia: InternetAddress => Option(ia.getAddress).exists(_.trim.endsWith("@gemini.edu"))
          case _                   => false
        }

      override def send(m: SafeMimeMessage): IO[Unit] = {
        write("Test Mailer: ", m) *> {

          import SafeMimeMessage.{ Content, Subject, To }

          sendAsync(
            (for {
              _ <- To      := m.to.filter(isGeminiAddress)
              _ <- Subject := s"Test: ${m.subject}"
              _ <- Content :=
                s"""
                  |This is a test message that, were it running in production, would have been sent to these recipients:
                  |
                  |  ${m.to.mkString(", ")}
                  |
                  |Message Body:
                  |
                  |${m.content.toString}
                  |
                """.stripMargin

            } yield ()).exec(m)
          )
        }
      }
    }

  def forDevelopment(site: Site): Mailer =
    new Mailer(site, "bogus.mail.host") {
      override def send(m: SafeMimeMessage): IO[Unit] =
        write("Development Mailer: ", m)
    }

  def ofType(t: MailerType, site: Site, smtpHost: String): Mailer =
    t match {
      case Production  => Mailer(site, smtpHost)
      case Test        => Mailer.forTesting(site, smtpHost)
      case Development => Mailer.forDevelopment(site)
    }

}
