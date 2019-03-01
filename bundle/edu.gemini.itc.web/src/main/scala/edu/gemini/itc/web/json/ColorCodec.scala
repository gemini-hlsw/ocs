package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import java.awt.Color

trait ColorCodec {

  implicit val ColorCodec: CodecJson[Color] =
    codec4[Int, Int, Int, Int, Color](
      (r, g, b, a) => new Color(r, g, b, a),
      c            => (c.getRed, c.getGreen, c.getBlue, c.getAlpha)
    )("red", "green", "blue", "alpha")

}

object color extends ColorCodec