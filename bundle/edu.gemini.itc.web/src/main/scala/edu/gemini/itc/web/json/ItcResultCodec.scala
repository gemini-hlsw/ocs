package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._

// Extending ItcSpectroscopyResultCodec means:
//   1. The default SpcSeriesDataCodec val is always available for callers that
//      don't supply a custom one.
//   2. ItcSpectroscopyResultCodec (the implicit def) is inherited, so the isr
//      parameter below resolves automatically without extra imports by callers.
trait ItcResultCodec extends ItcSpectroscopyResultCodec {
  import edu.gemini.json.coproduct._
  import itcimagingresult._

  // implicit def (not val) so that isr — and transitively sds — are resolved
  // at the call site of asJson, not at object-initialisation time.
  implicit def ItcResultDecodeJson(implicit isr: CodecJson[ItcSpectroscopyResult]): CodecJson[ItcResult] =
    CoproductCodec[ItcResult]
      .withCase("ItcImagingResult",      ItcImagingResultCodec) { case a: ItcImagingResult      => a }
      .withCase("ItcSpectroscopyResult", isr)                   { case a: ItcSpectroscopyResult => a }
      .asCodecJson

}

object itcresult extends ItcResultCodec