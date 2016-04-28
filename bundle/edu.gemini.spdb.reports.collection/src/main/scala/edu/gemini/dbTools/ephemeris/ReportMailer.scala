package edu.gemini.dbTools.ephemeris

import edu.gemini.spModel.core.Site

import java.util.Properties
import java.util.logging.{Level, Logger}
import javax.mail.{Transport, Session}
import javax.mail.Message.RecipientType.TO
import javax.mail.internet.{InternetAddress, MimeMessage}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import scalaz._, Scalaz._
import scalaz.effect.IO
import scalaz.effect.IO._

/** Mailer for ephemeris error reports.
 */
sealed abstract class ReportMailer(site: Site, val smtpHost: String, val recipients: List[InternetAddress]) {

  def notifyError(report: String): IO[Unit] =
    createMessage(report).flatMap(send)

  private def createMessage(report: String): IO[MimeMessage] = IO {
    val sender = new InternetAddress()
    sender.setAddress("noreply@gemini.edu")
    sender.setPersonal(s"Gemini ODB (${site.abbreviation})")

    val props = new Properties()
    props.put("mail.transport.protocol", "smtp")
    props.put("mail.smtp.host",          smtpHost)

    val text =
      s"""
         |There was an error running the ephemeris update service at ${site.displayName}:
         |
         |$report
         |
       """.stripMargin

    val mm = new MimeMessage(Session.getInstance(props, null))
    mm.setFrom(sender)
    recipients.foreach(mm.addRecipient(TO, _))
    mm.setSubject(s"${site.abbreviation} Ephemeris Update Error")
    mm.setContent(text, "text/plain")
    mm
  }

  protected def send(m: MimeMessage): IO[Unit]
}

object ReportMailer {
  val Log = Logger.getLogger(getClass.getName)

  def apply(site: Site, smtpHost: String, recipients: List[InternetAddress]): ReportMailer =
    new ReportMailer(site, smtpHost, recipients) {
      override def send(msg: MimeMessage): IO[Unit] =
        IO {
          val rs = recipients.mkString(", ")
          Future(Transport.send(msg)).onComplete {
            case Success(_) => Log.info(s"Successfully sent mail to $rs.")
            case Failure(e) => Log.log(Level.WARNING, s"Failed to send mail to $rs.", e)
          }
        }
    }

  def forTesting(site: Site): ReportMailer =
    new ReportMailer(site, "bogus.mail.host", Nil) {
      override def send(msg: MimeMessage): IO[Unit] =
        for {
          _ <- log("")
          _ <- log(s"From: ${msg.getFrom.mkString(", ")}")
          _ <- log(s"To: ${recipients.mkString(", ")}")
          _ <- log(s"Subject: ${msg.getSubject}")
          _ <- log("")
          _ <- msg.getContent.toString.lines.toList.traverseU(log)
        } yield ()

      def log(msg: String): IO[Unit] =
        putStrLn(s"TestMailer: $msg")
    }
}
