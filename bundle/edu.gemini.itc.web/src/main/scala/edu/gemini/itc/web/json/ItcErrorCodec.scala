package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._

trait ItcErrorCodec {

  implicit val ItcErrorCodec: CodecJson[ItcError] =
    casecodec1(ItcError.apply, ItcError.unapply)(
      "msg"
    )

}

object itcerror extends ItcErrorCodec