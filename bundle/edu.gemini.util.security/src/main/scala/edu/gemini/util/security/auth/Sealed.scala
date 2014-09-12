package edu.gemini.util.security.auth

import scalaz._
import Scalaz._
import javax.crypto.{SecretKey, Cipher, SecretKeyFactory, SealedObject}
import javax.crypto.spec.DESKeySpec
import java.security.MessageDigest

/** Typesafe wrapper for a `SealedObject`, sealed via DES. */
@SerialVersionUID(8279142247304203298L)
case class Sealed[+A](so: SealedObject) {

  import Sealed._

  /** Attempts to retrieve the sealed object. */
  def get(passPhrase: String): Exception \/ A = {
    val oldLoader = Thread.currentThread.getContextClassLoader
    try {
      Thread.currentThread.setContextClassLoader(getClass.getClassLoader) // TODO: may not be necessary
      so.getObject(key(passPhrase)).asInstanceOf[A].right
    } catch {
      case e: Exception => e.left
    } finally {
      Thread.currentThread.setContextClassLoader(oldLoader)
    }
  }

}

object Sealed {

  private lazy val algorithm = "DES" // 64-bit, exportable

  /** Seals an object using the provided pass phrase. */
  def seal[A <: java.io.Serializable : Manifest](a: A, passPhrase: String) =
    apply(new SealedObject(a, cipher(Cipher.ENCRYPT_MODE, passPhrase)))

  // HELPERS

  private def md5(s: String): Array[Byte] = {
    val md = MessageDigest.getInstance("MD5")
    md.update(s.getBytes())
    md.digest
  }

  private def key(s: String): SecretKey =
    SecretKeyFactory.getInstance(algorithm).generateSecret(new DESKeySpec(md5(s)))

  private def cipher(mode: Int, passPhrase: String): Cipher = {
    val c = Cipher.getInstance(algorithm)
    c.init(mode, key(passPhrase))
    c
  }

}
