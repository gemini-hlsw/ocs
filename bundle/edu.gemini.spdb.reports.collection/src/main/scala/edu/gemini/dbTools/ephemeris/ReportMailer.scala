package edu.gemini.dbTools.ephemeris

import edu.gemini.dbTools.mailer.Mailer
import edu.gemini.spModel.core.Site

import javax.mail.internet.InternetAddress

import scalaz.effect.IO

/**
 * Mailer for ephemeris error reports.
 */
sealed abstract class ReportMailer(val mailer: Mailer, val recipients: List[InternetAddress]) {

  def notifyError(report: String): IO[Unit] = {

    val subject = s"${mailer.site.abbreviation} Ephemeris Update Error"

    val text =
      s"""
         |There was an error running the ephemeris update service at ${mailer.site.displayName}:
         |
         |$report
         |
       """.stripMargin

    mailer.sendText(recipients, subject, text)
  }

}

object ReportMailer {

  def apply(site: Site, smtpHost: String, recipients: List[InternetAddress]): ReportMailer =
    new ReportMailer(Mailer(site, smtpHost), recipients) {}

  def forTesting(site: Site): ReportMailer =
    new ReportMailer(Mailer.forTesting(site), Nil) {}
}
