package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._

trait ItcResultCodec {
  import edu.gemini.json.coproduct._
  import itcimagingresult._
  import itcspectroscopyresult._

  implicit val ItcResultDecodeJson: CodecJson[ItcResult] =
    CoproductCodec[ItcResult]
      .withCase("ItcImagingResult",      ItcImagingResultCodec)      { case a: ItcImagingResult      => a }
      .withCase("ItcSpectroscopyResult", ItcSpectroscopyResultCodec) { case a: ItcSpectroscopyResult => a }
      .asCodecJson

}

object itcresult extends ItcResultCodec