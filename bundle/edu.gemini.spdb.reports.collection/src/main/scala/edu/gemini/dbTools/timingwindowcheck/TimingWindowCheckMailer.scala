package edu.gemini.dbTools.timingwindowcheck

import edu.gemini.dbTools.mailer.{Mailer, MailerType, ProgramAddresses}
import edu.gemini.pot.sp.SPObservationID
import edu.gemini.spModel.core.{ProgramId, SPProgramID, Site}

import javax.mail.internet.InternetAddress

import scalaz._
import scalaz.effect.IO

sealed abstract class TimingWindowCheckMailer(mailer: Mailer) {

  def notifyExpiredWindows(
    pid: SPProgramID,
    to:  ProgramAddresses,
    tws: NonEmptyList[SPObservationID]
  ): IO[Unit] = {

    // QA exploder address corresponding to the site indicated by the program id.
    val qaExploder: Option[InternetAddress] =
      ProgramId.parse(pid.stringValue).site.map {
        case Site.GN => new InternetAddress("gnqc@gemini.edu")
        case Site.GS => new InternetAddress("gsqc@gemini.edu")
      }

    val recipients: List[InternetAddress] =
      qaExploder.toList ++ to.pi ++ to.ngo ++ to.cs

    val s: String =
      if (tws.tail.isEmpty) "" else "s"

    val subject   =
      s"${pid.stringValue} Timing Window$s Expired"

    val text      =
      s"""
       |Timing window$s recently expired in ${pid.stringValue} observation$s:
       |
       |${tws.list.toList.sortBy(_.getObservationNumber).mkString("\t", "\n\t", "\n")}
       """.stripMargin

    mailer.sendText(recipients, subject, text)

  }

}

object TimingWindowCheckMailer {

  def apply(t: MailerType, site: Site, smtpHost: String): TimingWindowCheckMailer =
    new TimingWindowCheckMailer(Mailer.ofType(t, site, smtpHost)) {}

}