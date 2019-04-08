package edu.gemini

import argonaut.Argonaut._
import argonaut.{DecodeResult, CodecJson}

package object json {

  object all
    extends ArrayCodec
       with ConstCodec
       with CoproductCodec
       with DisjunctionCodec
       with KeyedCodec

}
