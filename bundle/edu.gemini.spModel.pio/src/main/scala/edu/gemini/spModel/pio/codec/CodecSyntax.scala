package edu.gemini.spModel.pio.codec

import scalaz._, Scalaz._

import edu.gemini.spModel.pio._

object CodecSyntax {

  implicit class ParamSetCodecOps[A](a: A)(implicit ev: ParamSetCodec[A]) {
    def encode(key: String): ParamSet = ev.encode(key, a)
  }

  implicit class ParamSetOps(ps: ParamSet) {
    def decode[A](implicit ev: ParamSetCodec[A]): PioError \/ A = ev.decode(ps)
  }

}