package edu.gemini.dbTools.timingwindowcheck

import edu.gemini.dbTools.mailer.{Mailer, MailerType, ProgramAddresses}
import edu.gemini.pot.sp.SPObservationID
import edu.gemini.spModel.core.{SPProgramID, Site}
import scalaz._
import scalaz.effect.IO

sealed abstract class TimingWindowCheckMailer(mailer: Mailer) {

  def notifyPendingCheck(
    pid: SPProgramID,
    to:  ProgramAddresses,
    tws: NonEmptyList[SPObservationID]
  ): IO[Unit] = {

    val subject   =
      s"${pid.stringValue} Timing Window Still Pending"

    val text      =
      s"""
       """.stripMargin

    mailer.sendText(to.ngo ++ to.cs, subject, text)

  }

}

object TimingWindowCheckMailer {

  def apply(t: MailerType, site: Site, smtpHost: String): TimingWindowCheckMailer =
    new TimingWindowCheckMailer(Mailer.ofType(t, site, smtpHost)) {}

}