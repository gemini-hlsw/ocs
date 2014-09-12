package edu.gemini.util.security.principal

import java.security.Principal
import edu.gemini.spModel.core._

/** Sealed marker trait for Gemini principals. */
sealed abstract class GeminiPrincipal(val getName:String) extends Principal with Serializable
object GeminiPrincipal {
  // nothing here yet, but this object is pimped elsewhere
}

case class AffiliatePrincipal(affiliate:Affiliate) extends GeminiPrincipal(affiliate.displayValue)

case class ProgramPrincipal(program:SPProgramID) extends GeminiPrincipal(program.toString)

case class StaffPrincipal(facility: String) extends GeminiPrincipal(facility)
object StaffPrincipal {
  def Gemini = apply("Gemini")
}

// UserPrincipal should be case-insensitive so we force it to lowercase on construction

class UserPrincipal private (val email:String) extends GeminiPrincipal(email) {
  override def toString: String = s"UserPrincipal($email)"
  override def hashCode: Int = email.hashCode
  override def equals(a: Any): Boolean =
    a match {
      case other: UserPrincipal => other.email == email
      case _ => false
    }
}

object UserPrincipal {
  def apply(email: String): UserPrincipal = new UserPrincipal(email.toLowerCase)
  def unapply(p: UserPrincipal): Some[String] = Some(p.email)
}

case class VisitorPrincipal(program:SPProgramID) extends GeminiPrincipal(program.toString)

