package edu.gemini.spModel.io.impl

import edu.gemini.pot.sp.SPNodeKey
import java.nio.charset.Charset
import javax.crypto.Cipher.{DECRYPT_MODE, ENCRYPT_MODE}
import javax.crypto.{SecretKeyFactory, Cipher, SecretKey}
import javax.crypto.spec.{PBEKeySpec, PBEParameterSpec}

// SW: Simple encryption support.  This may well be stupid since I haven't
// taken the time to study cryptography but it seems good enough to defeat
// casual observers of the exported XML.  The PBE "salt" is based on the node
// key so that the DES encryption produces quite different results for otherwise
// identical input.  Again this may be stupid, sorry.

object Encryption {
  val ParamName = "encryption"
  val UTF8 = Charset.forName("UTF-8")

  private val key: SecretKey = {
    val pass = "D41D8CD98F00B204E9800998ECF8427E"
    val kSpec = new PBEKeySpec(pass.toCharArray)
    val fac   = SecretKeyFactory.getInstance("PBEWithSHA1AndDESede")
    fac.generateSecret(kSpec)
  }

  private def cipher(k: SPNodeKey, mode: Int, in: Array[Byte]): Array[Byte] = {
    val pSpec = new PBEParameterSpec(k.uuid.toString.getBytes(UTF8), 1)
    val c = Cipher.getInstance("PBEWithSHA1AndDESede")
    c.init(mode, key, pSpec)
    c.doFinal(in, 0, in.length)
  }

  def encrypt(k: SPNodeKey, s: String): String = {
    val encrypted = cipher(k, ENCRYPT_MODE, s.getBytes(UTF8))
    javax.xml.bind.DatatypeConverter.printHexBinary(encrypted)
  }

  def decrypt(k: SPNodeKey, s: String): String = {
    val encrypted = javax.xml.bind.DatatypeConverter.parseHexBinary(s)
    new String(cipher(k, DECRYPT_MODE, encrypted), "UTF-8")
  }
}