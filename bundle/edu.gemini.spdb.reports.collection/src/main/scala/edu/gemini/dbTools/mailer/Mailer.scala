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


sealed abstract class Mailer(val site: Site, val smtpHost: String) {

  def sendText(
    to:      List[InternetAddress],
    subject: String,
    message: String
  ): IO[Unit] = IO {

    val sender = new InternetAddress       <|
      (_.setAddress("noreply@gemini.edu")) <|
      (_.setPersonal(s"Gemini ODB (${site.abbreviation})"))

    val props = new java.util.Properties         <|
      (_.put("mail.transport.protocol", "smtp")) <|
      (_.put("mail.smtp.host", smtpHost))

    new MimeMessage(Session.getInstance(props, null)) <|
      (_.setFrom(sender))                             <|
      (m => to.foreach(m.addRecipient(TO, _)))        <|
      (_.setSubject(subject))                         <|
      (_.setContent(message, "text/plain"))

  } >>= send

  def send(m: MimeMessage): IO[Unit]

}

object Mailer {

  val Log = Logger.getLogger(getClass.getName)

  private def sendAsync(m: MimeMessage): Unit = {
    // OCSADV-85: use a future here; it can block for a long time if the mail server is down
    val to = m.getRecipients(TO).mkString(", ")
    Future(Transport.send(m)).onComplete {
      case Success(_) => Log.info(s"Successfully sent mail to $to.")
      case Failure(e) => Log.log(Level.WARNING, s"Failed to send mail to $to", e)
    }
  }

  private def write(prefix: String, m: MimeMessage): IO[Unit] = {
    def log(m: String): IO[Unit] =
      putStrLn(s"$prefix: $m")

    for {
      _ <- putStrLn("")
      _ <- log(s"From...: ${m.getFrom.mkString(", ")}")
      _ <- log(s"To.....: ${m.getRecipients(TO).mkString(", ")}")
      _ <- log(s"Subject: ${m.getSubject}")
      _ <- log("")
      _ <- m.getContent.toString.lines.toList.traverseU(log)
    } yield ()
  }

  def apply(site: Site, smtpHost: String): Mailer =
    new Mailer(site, smtpHost) {
      def send(m: MimeMessage): IO[Unit] =
        write("Production Mailer", m) *> IO(sendAsync(m))
    }

  def forTesting(site: Site, smtpHost: String): Mailer =
    new Mailer(site, smtpHost) {
      private def isGeminiAddress(a: Address): Boolean =
        a match {
          case ia: InternetAddress => Option(ia.getAddress).exists(_.trim.endsWith("@gemini.edu"))
          case _                   => false
        }

      def send(m: MimeMessage): IO[Unit] = {
        write("Test Mailer", m) *> {

          val allRecipients = m.getRecipients(TO)

          val content       =
            s"""
              |This is a test message that, were it running in production, would have been sent to these recipients:
              |
              |  ${allRecipients.mkString(", ")}
              |
              |Message Body:
              |
              |${m.getContent.toString}
              |
            """.stripMargin

          m.setRecipients(TO, allRecipients.filter(isGeminiAddress))
          m.setSubject("Test: " + m.getSubject)
          m.setContent(content, "text/plain")

          IO(sendAsync(m))
        }
      }
    }

  def forDevelopment(site: Site): Mailer =
    new Mailer(site, "bogus.mail.host") {
      def send(m: MimeMessage): IO[Unit] =
        write("Development Mailer", m)
    }

  def ofType(t: MailerType, site: Site, smtpHost: String): Mailer =
    t match {
      case Production  => Mailer(site, smtpHost)
      case Test        => Mailer.forTesting(site, smtpHost)
      case Development => Mailer.forDevelopment(site)
    }

}
