package edu.gemini.itc.web

import argonaut._, Argonaut._
import edu.gemini.itc.shared._

package object json {

  def constCodec[A](a: A): CodecJson[A] =
    CodecJson(_ => jEmptyObject, _ => DecodeResult.ok(a))

  implicit val SignalToNoiseAtCodec: CodecJson[SignalToNoiseAt] =
    casecodec3(SignalToNoiseAt.apply, SignalToNoiseAt.unapply)(
      "wavelength",
      "single",
      "final"
    )

  implicit val ExposureCalculationCodec: CodecJson[ExposureCalculation] =
    casecodec3(ExposureCalculation.apply, ExposureCalculation.unapply)(
      "exposureTime",
      "exposures",
      "signalToNoise"
    )

  implicit val AllExposureCalculationCodec: CodecJson[AllExposureCalculations] =
    casecodec2(AllExposureCalculations.apply, AllExposureCalculations.unapply)(
      "exposuresPerCCD",
      "selected"
    )

}
