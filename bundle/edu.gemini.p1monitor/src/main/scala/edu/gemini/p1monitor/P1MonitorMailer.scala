package edu.gemini.p1monitor

import config.P1MonitorConfig
import javax.mail.internet.{MimeMessage, InternetAddress}
import java.util.logging.{Level, Logger}
import java.util.Properties
import javax.mail.{Transport, Message, Session}
import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.immutable
import edu.gemini.p1monitor.P1Monitor._

import scalaz._
import Scalaz._

class P1MonitorMailer(cfg: P1MonitorConfig) {
  val LOG = Logger.getLogger(this.getClass.getName)
  val sender = new InternetAddress("noreply@gemini.edu")

  def notify(dirName: String, files: ProposalFileGroup) {
    val proposal = files.xml.map(ProposalIo.read)

    //construct email subject
    val subject = proposal.map { prop =>
        s"New ${getSiteString(prop.observations)} ${getTypeString(prop.proposalClass)} Proposal: ${getReferenceString(prop.proposalClass)}"
      }.getOrElse("")

    //construct email body
    var body = ""
    proposal foreach {
      prop => {
        body += "A new "
        body += getTypeString(prop.proposalClass)
        body += " proposal has been received"
        body += " (" + getReferenceString(prop.proposalClass) + ")"
        body += ":\n\n"

        body += "\t" + prop.title + "\n"
        body += "\t" + prop.investigators.pi + "\n\n"

        body += "\t" + getInstrumentsString(prop) + "\n"
        body += "\t" + prop.proposalClass.requestedTime.format() + " requested\n\n"

        val proposalVariable = getReferenceString(prop.proposalClass).split("-").tail.mkString("_")
        body += "Review the PDF summary at:\n"
        body += "\thttp://" + cfg.getHost + ":" + cfg.getPort + "/fetch?dir=" + dirName + "&type=" + getTypeName(prop.proposalClass) + "&proposal=" + proposalVariable + "&format=pdf\n\n"

        body += "Download the proposal from:\n"
        body += "\thttp://" + cfg.getHost + ":" + cfg.getPort + "/fetch?dir=" + dirName + "&type=" + getTypeName(prop.proposalClass) + "&proposal=" + proposalVariable + "&format=xml\n\n"

        body += "Download the proposal's attachment from:\n"
        body += "\thttp://" + cfg.getHost + ":" + cfg.getPort + "/fetch?dir=" + dirName + "&type=" + getTypeName(prop.proposalClass) + "&proposal=" + proposalVariable + "&format=attachment\n\n"
      }
    }
    files.xml foreach {
      xml => {
        body += "Find it in the backend server at:\n"
        body += "\t" + xml.getAbsolutePath + "\n"
      }
    }

    //send email
    sendMail(dirName, subject, body)

  }

  private def sendMail(dirName: String, subject: String, body: String) {

    // Log the email we will send.
    LOG.log(Level.INFO, s"Sending email:\n\nSubject:\n$subject \n\nBody: \n$body")

    // Create and update the mime message.
    val msg = createMessage()
    msg.setFrom(sender)
    msg.setSubject(subject)

    msg.setText(body)

    addAddresses(msg, Message.RecipientType.TO, cfg.getDirectory(dirName).to)
    addAddresses(msg, Message.RecipientType.CC, cfg.getDirectory(dirName).cc)
    addAddresses(msg, Message.RecipientType.BCC, cfg.getDirectory(dirName).bcc)

    // Send it.
    Transport.send(msg)
  }

  private def createMessage() = {
    val sessionProps = new Properties()
    sessionProps.put("mail.transport.protocol", "smtp")
    sessionProps.put("mail.smtp.host", cfg.getSmtp)
    val session = Session.getInstance(sessionProps, null)

    new MimeMessage(session)
  }

  private def addAddresses(msg: MimeMessage, recType: Message.RecipientType, addrs: Traversable[InternetAddress]) {
    for (addr <- addrs) {
      msg.addRecipient(recType, addr)
    }
  }

  private def getSiteString(observations: List[Observation]): String = observations.map { obs =>
      obs.blueprint.map(_.site)
    }.flatten.distinct.mkString(", ")

  private def getReferenceString(propClass: ProposalClass): String = {
    val string = propClass match {
      case pc: SpecialProposalClass       => pc.sub.response.map(_.receipt.id).mkString(" ")
      case ft: FastTurnaroundProgramClass => ft.sub.response.map(_.receipt.id).mkString(" ")
      case q:  QueueProposalClass         => ~q.subs.left.getOrElse(Nil).map(_.response.map(_.receipt.id).mkString(" ")).headOption
      case _                              => ""
    }
    string.trim
  }

  private def getTypeString(propClass: ProposalClass): String = propClass match {
      case pc: immutable.SpecialProposalClass       => pc.sub.specialType.value()
      case ft: immutable.FastTurnaroundProgramClass => "Fast Turnaround"
      case q: immutable.QueueProposalClass          => "Queue"
      case _                                        => ""
    }

  private def getTypeName(propClass: ProposalClass): String = propClass match {
      case pc: immutable.SpecialProposalClass       => pc.sub.specialType
      case ft: immutable.FastTurnaroundProgramClass => "FT"
      case q:  immutable.QueueProposalClass         => ~q.subs.left.getOrElse(Nil).collect {
          case s if cfg.map.keys.toList.contains(s.partner.value()) => s.partner.value().toUpperCase
        }.headOption
      case _                                        => ""
    }

  private def getInstrumentsString(prop: Proposal): String = prop.observations.map {
      obs => obs.blueprint match {
        case Some(bp: GeminiBlueprintBase) => s"${bp.instrument.site.name} (${bp.instrument.id})"
        case _                             => ""
      }
    }.distinct.mkString(", ")

}