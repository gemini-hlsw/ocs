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

  implicit val IntegrationTimeCalculationCodec: CodecJson[IntegrationTime] =
    casecodec2(IntegrationTime.apply, IntegrationTime.unapply)(
      "exposureTime",
      "exposures"
    )

  implicit val AllIntegrationTimesCodec: CodecJson[AllIntegrationTimes] =
    casecodec2(AllIntegrationTimes.apply, AllIntegrationTimes.unapply)(
      "detectors",
      "selected"
    )

}
