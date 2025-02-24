package edu.gemini.p1monitor

import config.P1MonitorConfig

import javax.mail.internet.{InternetAddress, MimeMessage}
import java.util.logging.{Level, Logger}
import java.util.Properties
import javax.mail.{Message, Session, Transport}
import edu.gemini.model.p1.immutable._
import edu.gemini.p1monitor.P1Monitor._
import scalaz._
import Scalaz._

import javax.mail.Address

class P1MonitorMailer(cfg: P1MonitorConfig) {
  val LOG = Logger.getLogger(this.getClass.getName)
  val sender = new InternetAddress("noreply@gemini.edu")

  def notify(dirName: String, files: ProposalFileGroup): Unit = {
    val proposal = files.xml.map(ProposalIo.read)

    //construct email subject
    val subject = proposal.map { prop =>
      s"New ${getSiteString(prop.observations)} ${getTypeString(prop.proposalClass)} Proposal: ${getReferenceString(prop.proposalClass)}"
      }.getOrElse("")

    //construct email body
    val preBody = proposal.map { prop =>
        val proposalVariable = getReferenceString(prop.proposalClass).split("-").tail.mkString("_")

        s"""
          |A new ${getTypeString(prop.proposalClass)} proposal has been received (${getReferenceString(prop.proposalClass)})
          |
          |    ${prop.title}
          |    ${prop.investigators.pi}
          |
          |    ${getInstrumentsString(prop)}
          |    ${prop.proposalClass.requestedTime.format()} requested
          |
          |Review the PDF summary at
          |https://${cfg.getHost}/fetch/${getTypeName(dirName, prop.proposalClass)}/${Semester.current.display}/fetch?dir=$dirName&type=${getTypeName(dirName, prop.proposalClass)}&proposal=$proposalVariable&format=pdf
          |
          |Download the proposal from:
          |https://${cfg.getHost}/fetch/${getTypeName(dirName, prop.proposalClass)}/${Semester.current.display}/fetch?dir=$dirName&type=${getTypeName(dirName, prop.proposalClass)}&proposal=$proposalVariable&format=xml
          |
          |Download the proposal's attachment from:
          |https://${cfg.getHost}/fetch/${getTypeName(dirName, prop.proposalClass)}/${Semester.current.display}/fetch?dir=$dirName&type=${getTypeName(dirName, prop.proposalClass)}&proposal=$proposalVariable&format=attachment
        """.stripMargin
    }

    val secondAttachment = proposal.flatMap { prop =>
      val proposalVariable = getReferenceString(prop.proposalClass).split("-").tail.mkString("_")
      prop.meta.secondAttachment.map { at =>
         s"""
              |Download the proposal's second attachment from:
              |https://${cfg.getHost}/fetch/${getTypeName(dirName, prop.proposalClass)}/${Semester.current.display}/fetch?dir=$dirName&type=${getTypeName(dirName, prop.proposalClass)}&proposal=$proposalVariable&format=attachment2
              |
          """.stripMargin
        }
    }

    val body = files.xml.map { x =>
        s"""
          |Find it in the backend server at:
          |    ${x.getAbsolutePath}
          |""".stripMargin
      }

    //send email
    (body |+| preBody |+| secondAttachment).foreach(sendMail(dirName, subject, _))

  }

  private def sendMail(dirName: String, subject: String, body: String): Unit = {

    // Log the email we will send.
    LOG.log(Level.INFO, s"Sending email:\n\nSubject:\n$subject \n\nBody: \n$body")

    // Create and update the mime message.
    val msg = createMessage()
    msg.setFrom(sender)
    msg.setSubject(subject)

    msg.setText(body)

    setAddresses(msg, Message.RecipientType.TO, cfg.getDirectory(dirName).to)
    setAddresses(msg, Message.RecipientType.CC, cfg.getDirectory(dirName).cc)
    setAddresses(msg, Message.RecipientType.BCC, cfg.getDirectory(dirName).bcc)

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

  private def setAddresses(msg: MimeMessage, recType: Message.RecipientType, addrs: Traversable[InternetAddress]): Unit =
    msg.setRecipients(recType, addrs.toList.widen[Address].toArray)

  private def getSiteString(observations: List[Observation]): String = observations.flatMap { obs =>
    obs.blueprint.map(_.site)
  }.distinct.mkString(", ")

  private def getReferenceString(propClass: ProposalClass): String = {
    val string = propClass match {
      case pc: SpecialProposalClass                                  => pc.sub.response.map(_.receipt.id).mkString(" ")
      case ft: FastTurnaroundProgramClass                            => ft.sub.response.map(_.receipt.id).mkString(" ")
      case sip: SubaruIntensiveProgramClass                          => sip.sub.response.map(_.receipt.id).mkString(" ")
      case lp: LargeProgramClass                                     => lp.sub.response.map(_.receipt.id).mkString(" ")
      case t @ QueueProposalClass(_, _, _, Right(p), _, _, _, _, _)  => t.subs.right.get.response.map(_.receipt.id).mkString(" ")
      case t @ ClassicalProposalClass(_, _, _, Right(p), _, _, _, _) => t.subs.right.get.response.map(_.receipt.id).mkString(" ")
      case q:  GeminiNormalProposalClass                             => ~q.subs.left.getOrElse(Nil).flatMap(_.response.map(_.receipt.id)).headOption
      case _                                                         => ""
    }
    string.trim
  }

  private def getTypeString(propClass: ProposalClass): String            = propClass match {
      case pc: SpecialProposalClass                                     => pc.sub.specialType.value()
      case _:  FastTurnaroundProgramClass                               => "Fast Turnaround"
      case _:  SubaruIntensiveProgramClass                              => "Intensive Observing Program at Subaru"
      case _:  LargeProgramClass                                        => "Large Program"
      case QueueProposalClass(_, _, _, Right(_), _, _, _, _, _)         => "Exchange"
      case _:  QueueProposalClass                                       => "Queue"
      case ClassicalProposalClass(_, _, _, Right(_), _, _, _, _)        => "Exchange"
      case _:  ClassicalProposalClass                                   => "Classical"
      case ExchangeProposalClass(_, _, _, ExchangePartner.SUBARU, _, _) => "Subaru"
      case _                                                            => ""
    }

  private def getTypeName(dir: String, propClass: ProposalClass): String  = propClass match {
      case ft: FastTurnaroundProgramClass                                => "FT"
      case _:  SubaruIntensiveProgramClass                               => "SIP"
      case pc: SpecialProposalClass                                      => pc.sub.specialType
      case lp: LargeProgramClass                                         => "LP"
      case q:  GeminiNormalProposalClass                                 => dir.toUpperCase
      case ExchangeProposalClass(_, _, _, ExchangePartner.SUBARU, _, _)  => "SUBARU"
      case _                                                             => ""
    }

  private def getInstrumentsString(prop: Proposal): String = prop.observations.map {
      obs => obs.blueprint match {
        case Some(bp: GeminiBlueprintBase) => s"${bp.site.name} (${bp.instrument.id})"
        case _                             => ""
      }
    }.distinct.mkString(", ")

}
