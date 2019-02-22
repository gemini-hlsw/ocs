package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._

trait ItcServiceResultCodec {

  implicit val ItcServiceResultCodec: CodecJson[ItcService.Result] =
    CodecJson(
      EncodeJson.of[ItcService.Result].encode,
      DecodeJson.of[ItcService.Result].decode
    )

}

object itcserviceresult extends ItcServiceResultCodec