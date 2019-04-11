package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import _root_.squants.motion.Velocity
import _root_.squants.radio.{ Irradiance, SpectralIrradiance }

trait SquantsCodec {
  import edu.gemini.json.keyed._

  implicit val VelocityCodec: CodecJson[Velocity] =
    keyedCodec[Velocity, String](_.toString, (s: String) => Velocity(s).toOption)

  implicit val IrradianceCodec: CodecJson[Irradiance] =
    keyedCodec[Irradiance, String](_.toString, (s: String) => Irradiance(s).toOption)

  implicit val SpectralIrradianceCodec: CodecJson[SpectralIrradiance] =
    keyedCodec[SpectralIrradiance, String](_.toString, (s: String) => SpectralIrradiance(s).toOption)

}

object squants extends SquantsCodec