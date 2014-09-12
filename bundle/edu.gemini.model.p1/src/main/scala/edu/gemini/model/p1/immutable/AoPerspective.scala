package edu.gemini.model.p1.immutable

/**
 * Identifies whether adaptive optics are used and if so whether
 * natural or laser guide stars are used.
 */
sealed trait AoPerspective {
  def toBoolean: Boolean = true
}

case object AoNone extends AoPerspective {
  override def toBoolean: Boolean = false
}

case object AoLgs  extends AoPerspective
case object AoNgs  extends AoPerspective
