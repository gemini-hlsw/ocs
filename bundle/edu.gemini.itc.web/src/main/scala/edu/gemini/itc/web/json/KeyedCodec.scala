package edu.gemini.itc.web.json

import argonaut._, Argonaut._

trait KeyedCodec {
  import KeyedCodec.IsEnum

  /** Codec for type A uniquely associated with a key type K. */
  def keyedCodec[A, K](tag: A => K, lookup: K => Option[A])(
    implicit E: EncodeJson[K],
             D: DecodeJson[K]
  ): CodecJson[A] =
    CodecJson(
      a => E.encode(tag(a)),
      c => D.decode(c).flatMap { t =>
        lookup(t) match {
          case Some(a) => DecodeResult.ok(a)
          case None    => DecodeResult.fail(s"Invalid key: $t", c.history)
        }
      }
    )

  implicit def enumEncode[E: IsEnum]: CodecJson[E] =
    ???

  implicit def enumDecode[E: IsEnum]: DecodeJson[E] =
    ???

  def enumCodec[E: IsEnum]: CodecJson[E] = {
    val (e, d) = (enumEncode, enumDecode)
    CodecJson(e.encode, d.decode)
  }


}

object KeyedCodec {

  /** Witness that `A` is an Keyederated type. */
  trait IsEnum[A]
  object IsEnum {
    def apply[A](implicit ev: IsEnum[A]): ev.type = ev
    implicit def fromEnum[E <: Enum[_]]: IsEnum[E] =
      new IsEnum[E] {
      }
  }

}

object keyed extends KeyedCodec
