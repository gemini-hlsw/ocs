package edu.gemini.util.security.auth

import java.security.{PrivateKey, PublicKey, SignedObject}
import scalaz._
import Scalaz._

/**
 * Typesafe wrapper for signed objects; construct using factory on companion with an existing SignedObject, or
 * a Serializable and a private key.
 */
@SerialVersionUID(-7392948081399954977L)
class Signed[+A] private (private val so: SignedObject)(implicit mf: Manifest[A]) extends DSA with java.io.Serializable {

  {
    // Barf if the signed object and type param are out of sync. This should never happen, heh-heh
    val (c, o) = (mf.runtimeClass, so.getObject)
    require(c.isInstance(o), s"Expected ${c.getName}, found ${o.getClass.getName}.")
  }

  def get: A = so.getObject.asInstanceOf[A]

  def verifies(publicKey: PublicKey): Boolean =
    so.verify(publicKey, signature)

  def verify(publicKey: PublicKey): Exception \/ Unit =
    verifies(publicKey).fold(().right, new SecurityException("Signature verfication failed.").left)

  override def toString = s"Signed($get)"

  override def hashCode: Int =
    so.getObject.hashCode

  override def equals(a:Any) = a match {
    case b: Signed[_] =>
      b.so.getObject == so.getObject && b.so.getSignature.corresponds(so.getSignature)(_ == _)
    case _ => false
  }

}

object Signed extends DSA {

  def fromSignedObject[A](so: SignedObject)(implicit mf: Manifest[A]): Exception \/ Signed[A] =
    \/.fromTryCatchNonFatal(new Signed[A](so)).leftMap {
      case e:Exception => e
      case t => throw t
    }

  def sign[A <: java.io.Serializable : Manifest](a: A, privateKey: PrivateKey): Exception \/ Signed[A] =
    fromSignedObject[A](new SignedObject(a, privateKey, signature))

}

