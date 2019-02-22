package edu.gemini.itc.web.json

import argonaut._, Argonaut._

trait ArrayCodec {

  implicit def arrayEncodeJson[A: EncodeJson]: EncodeJson[Array[A]] =
    EncodeJson.of[List[A]].contramap(_.toList)

  implicit def arrayDecodeJson[A: DecodeJson: reflect.ClassTag]: DecodeJson[Array[A]] =
    DecodeJson.of[List[A]].map(_.toArray)

}

object array extends ArrayCodec