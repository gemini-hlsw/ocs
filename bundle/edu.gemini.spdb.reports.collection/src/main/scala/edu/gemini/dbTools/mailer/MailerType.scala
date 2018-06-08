package edu.gemini.dbTools.mailer

import java.io.Serializable

import scalaz._
import Scalaz._


/**
 * Distinguishes production and test mailers.
 */
sealed trait MailerType extends Product with Serializable

object MailerType {

  case object Production extends MailerType
  case object Test       extends MailerType

  def fromString(s: String): Option[MailerType] =
    s match {
      case "production" => Some(Production)
      case "test"       => Some(Test)
      case _            => None
    }

  implicit val EqualMailerType: Equal[MailerType] =
    Equal.equalA

}
