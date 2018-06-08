package edu.gemini.dbTools.maskcheck

import edu.gemini.auxfile.api.AuxFile
import edu.gemini.dbTools.mailer.{ Mailer, ProgramAddresses }
import edu.gemini.spModel.core.{ Site, SPProgramID }

import scalaz._
import Scalaz._
import scalaz.effect.IO

sealed abstract class MaskCheckMailer(mailer: Mailer) {

  def notifyPendingCheck(pid: SPProgramID, to: ProgramAddresses, files: List[AuxFile]): MC[Unit] = {

    val subject   = s"${pid.stringValue} Mask Checks"

    val text      =
      s"""
         |Mask definition files are pending checks in ${pid.stringValue}:
         |
         |${files.mkString("\n")}
       """.stripMargin

//    addresses.disjunction match {
//      case -\/(errors) =>
//        MC.fail(errors.toList.mkString(", "))
//
//      case \/-(pa)     =>
        EitherT(mailer.sendText(to.ngo ++ to.cs, subject, text).catchLeft)
//    }
  }

}

object MaskCheckMailer {

  def apply(site: Site, smtpHost: String): MaskCheckMailer =
    new MaskCheckMailer(Mailer(site, smtpHost)) {}

  def forTesting(site: Site): MaskCheckMailer =
    new MaskCheckMailer(Mailer.forTesting(site)) {}

}