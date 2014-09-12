package edu.gemini.util.security.auth.keychain

sealed trait KeyFailure {
  def toException: KeyException =
    KeyException(this)
}

object KeyFailure {

//  case object UnsupportedPrincipalType extends KeyFailure
  case object InvalidPassword extends KeyFailure
  case class InvalidSignature(throwable: Throwable) extends KeyFailure
  case object InvalidVersion extends KeyFailure
  case class NotifyFailure(throwable: Throwable) extends KeyFailure

  case object KeychainLocked extends KeyFailure
  case object BadPassword extends KeyFailure
  case object IllegalLockState extends KeyFailure

}

case class KeyException(failure: KeyFailure) extends Exception

