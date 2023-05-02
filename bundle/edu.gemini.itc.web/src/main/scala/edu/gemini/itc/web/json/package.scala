package edu.gemini.itc.web

import argonaut._, Argonaut._
import edu.gemini.itc.shared._

package object json {

  def constCodec[A](a: A): CodecJson[A] =
    CodecJson(_ => jEmptyObject, _ => DecodeResult.ok(a))

  implicit val ExposureCalculationCodec: CodecJson[ExposureCalculation] =
    casecodec3(ExposureCalculation.apply, ExposureCalculation.unapply)(
      "exposureTime",
      "exposures",
      "signalToNoise"
    )

}
