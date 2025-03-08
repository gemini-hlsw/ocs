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

  implicit val ExposureCalculationCodec: CodecJson[TotalExposure] =
    casecodec2(TotalExposure.apply, TotalExposure.unapply)(
      "exposureTime",
      "exposures"
    )

  implicit val AllExposureCalculationCodec: CodecJson[AllExposures] =
    casecodec2(AllExposures.apply, AllExposures.unapply)(
      "exposuresPerCCD",
      "selected"
    )

}
