package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.spModel.core.Wavelength

trait WavelengthCodec {
  import keyed._

  implicit val wavelengthCodec: CodecJson[Wavelength] =
    keyedCodec(_.toString, (s: String) => Wavelength(s).toOption)

}

object wavelength extends WavelengthCodec