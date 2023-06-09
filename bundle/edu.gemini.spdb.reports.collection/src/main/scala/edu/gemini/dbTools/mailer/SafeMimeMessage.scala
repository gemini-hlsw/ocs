package edu.gemini.dbTools.mailer

import javax.mail._
import javax.mail.Message.RecipientType.TO
import javax.mail.internet._

import scalaz._
import Scalaz._

/**
 * A simple MimeMessage wrapper created to avoid the `null` results from calling
 * MimeMessage methods.
 */
case class SafeMimeMessage(
  from:    InternetAddress,
  to:      List[InternetAddress],
  subject: String,
  content: String
) {

  /** Converts to a MimeMessage to be sent via a Java Mail API Transport. */
  def toMimeMessage(smtpHost: String): MimeMessage = {
    val props = new java.util.Properties         <|
      (_.put("mail.transport.protocol", "smtp")) <|
      (_.put("mail.smtp.host", smtpHost))

    new MimeMessage(Session.getInstance(props, null)) <|
      (_.setFrom(from))                                <|
      (_.setRecipients(TO, to.widen[Address].toArray)) <|
      (_.setSubject(subject))                          <|
      (_.setContent(content, "text/plain"))
  }

}

object SafeMimeMessage {
  val From: Lens[SafeMimeMessage, InternetAddress] =
    Lens.lensu((a, b) => a.copy(from = b), _.from)

  val To: Lens[SafeMimeMessage, List[InternetAddress]] =
    Lens.lensu((a, b) => a.copy(to = b), _.to)

  val Subject: Lens[SafeMimeMessage, String] =
    Lens.lensu((a, b) => a.copy(subject = b), _.subject)

  val Content: Lens[SafeMimeMessage, String] =
    Lens.lensu((a, b) => a.copy(content = b), _.content)
}
