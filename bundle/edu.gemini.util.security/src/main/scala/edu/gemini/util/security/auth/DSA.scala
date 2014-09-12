package edu.gemini.util.security.auth

import java.security.{SecureRandom, KeyPairGenerator, Signature}

trait DSA {

  protected def signature = Signature.getInstance("DSA")

  protected def keyPairGenerator = {
    val x = KeyPairGenerator.getInstance("DSA")
    x.initialize(512, new SecureRandom)
    x
  }

}
