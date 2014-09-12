package edu.gemini.util.security.auth.keychain

import edu.gemini.util.security.principal._
import scalaz._

/** Public interface for key service. */
trait KeyService {

  /** Action to retrieve the key for the specified principal using the given password. */
  def tryKey(principal: GeminiPrincipal, pass: String): \/[KeyFailure, Key]

  /** Action to validates the given key. May fail with `InvalidSignature` or `InvalidVersion`. */
  def validateKey(key: Key): \/[KeyFailure, Unit]

  /** Action to set a user's password to a random value and notify via email. */
  def resetPasswordAndNotify(u: UserPrincipal): \/[KeyFailure, Unit]

  /** Action to simply verify that the server exists. */
  def testConnection(): \/[KeyFailure, Unit] =
    \/-(())

  /** Unsafe Java interface. */
  def asJava = asJavaStub
  object asJavaStub {
    def tryUserKey(email: String, pass: String): Boolean =
      KeyService.this.tryKey(UserPrincipal(email), pass).isRight
  }

}


