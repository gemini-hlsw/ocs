package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import scalaz.\/

trait DisjunctionCodec {

  implicit def disjunctionEncodeJson[A: EncodeJson, B: EncodeJson]: EncodeJson[A \/ B] =
    EncodeJson.EitherEncodeJson[A, B].contramap(_.toEither)

  implicit def disjunctionDecodeJson[A: DecodeJson, B: DecodeJson]: DecodeJson[A \/ B] =
    DecodeJson.EitherDecodeJson[A, B].map(\/.fromEither)

}

object disjunction extends DisjunctionCodec