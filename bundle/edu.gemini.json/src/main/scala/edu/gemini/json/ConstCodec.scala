package edu.gemini.json

import argonaut.Argonaut._
import argonaut.{DecodeResult, CodecJson}

trait ConstCodec {

  def constCodec[A](a: A): CodecJson[A] =
    CodecJson(_ => jEmptyObject, _ => DecodeResult.ok(a))

}

object const extends ConstCodec


