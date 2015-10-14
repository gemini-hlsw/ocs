package edu.gemini.spModel.pio.codec

import scalaz._, Scalaz._

import scala.collection.JavaConverters._
import edu.gemini.spModel.pio._
import edu.gemini.spModel.pio.xml.PioXmlFactory

object CodecSyntax {

  class ParamSetCodecOps[A](a: A)(implicit ev: ParamSetCodec[A]) {
    def encode(key: String): ParamSet = ev.encode(key, a)
  }

  class ParamSetOps(ps: ParamSet) {
    def decode[A](implicit ev: ParamSetCodec[A]): PioError \/ A = ev.decode(ps)
  }

}