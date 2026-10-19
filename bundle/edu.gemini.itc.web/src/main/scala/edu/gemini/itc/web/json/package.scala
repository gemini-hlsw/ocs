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

  implicit val ItcReadModeCodec: CodecJson[ItcReadMode] =
    CodecJson(
      r => jString(r.name),
      c => c.as[String].flatMap(n => ItcReadMode.fromName(n).fold(DecodeResult.fail[ItcReadMode](s"Unknown read mode: $n", c.history))(DecodeResult.ok))
    )

  implicit val IntegrationTimeCalculationCodec: CodecJson[IntegrationTime] =
    casecodec4(IntegrationTime.apply, IntegrationTime.unapply)(
      "exposureTime",
      "frames",
      "coadds",
      "readMode"
    )

  implicit val AllIntegrationTimesCodec: CodecJson[AllIntegrationTimes] =
    casecodec2(AllIntegrationTimes.apply, AllIntegrationTimes.unapply)(
      "detectors",
      "selected"
    )

}
