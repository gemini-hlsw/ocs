package edu.gemini.itc.web

import argonaut._, Argonaut._

package object json {

  def constCodec[A](a: A): CodecJson[A] =
    CodecJson(_ => jEmptyObject, _ => DecodeResult.ok(a))

}