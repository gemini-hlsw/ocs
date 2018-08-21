package edu.gemini.dbTools.timingwindowcheck

import edu.gemini.dbTools.mailer.{ Mailer, MailerType, ProgramAddresses }
import edu.gemini.spModel.core.{ Site, SPProgramID }

import scalaz._
import Scalaz._
import scalaz.effect.IO

sealed abstract class TimingWindowCheckMailer(mailer: Mailer) {

  def notifyPendingCheck(
    pid:   SPProgramID,
    to:    ProgramAddresses
  ): Action[Unit] = {

    val subject   =
      s"${pid.stringValue} Timing Window Still Pending"

    val text      =
      s"""
       """.stripMargin

    EitherT(mailer.sendText(to.ngo ++ to.cs, subject, text).catchLeft)

  }

}

object TimingWindowCheckMailer {

  def apply(t: MailerType, site: Site, smtpHost: String): TimingWindowCheckMailer =
    new TimingWindowCheckMailer(Mailer.ofType(t, site, smtpHost)) {}

}