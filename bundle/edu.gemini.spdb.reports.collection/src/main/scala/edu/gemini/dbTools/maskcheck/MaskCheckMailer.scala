package edu.gemini.dbTools.maskcheck

import edu.gemini.auxfile.api.AuxFile
import edu.gemini.dbTools.mailer.{ Mailer, MailerType, ProgramAddresses }
import edu.gemini.spModel.core.{ Site, SPProgramID }

import scalaz._
import Scalaz._
import scalaz.effect.IO

sealed abstract class MaskCheckMailer(mailer: Mailer) {

  def notifyPendingCheck(
    pid:   SPProgramID,
    to:    ProgramAddresses,
    files: List[AuxFile]
  ): Action[Unit] = {

    val subject   =
      s"${pid.stringValue} Mask Checks Still Pending"

    val text      =
      s"""
         |Mask definition files require checking in ${pid.stringValue}:
         |
         |${files.map(_.getName).mkString("\t", "\n\t", "")}
       """.stripMargin

    EitherT(mailer.sendText(to.ngo ++ to.cs, subject, text).catchLeft)

  }

}

object MaskCheckMailer {

  def apply(t: MailerType, site: Site, smtpHost: String): MaskCheckMailer =
    new MaskCheckMailer(Mailer.ofType(t, site, smtpHost)) {}

}