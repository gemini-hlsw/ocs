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

  // N.B. we need to define thes implicit encode/decode instances individually because code that
  // demands EncodeJson[A] for example will use a CodecJson[A] if one is present (because it's a
  // subclass of EncodeJson) but it *won't* try to derive one because it doesn't go out and search
  // for arbitrary subclass derivations. So anyway the rule is `implicit val ...: CodecJson[...]`
  // is ok but `implicit def` won't work … you need to split it into an encode+decode pair. Circe
  // is a fork of Argonaut but it got rid of Codec because of this common point of confusion.

  // You'll have to trust me here that we need to define the instances this way, with an
  // intermediate typeclass [E: IsEnum] rather than the more direct [E <: Enum[E] : ClassTag].

  implicit def enumEncode[E: IsEnum]: CodecJson[E] =
    enumCodec[E]

  implicit def enumDecode[E: IsEnum]: DecodeJson[E] =
    enumCodec[E]

  def enumCodec[E](implicit E: IsEnum[E]): CodecJson[E] =
    keyedCodec[E, String](E.key, E.lookup)

}

object KeyedCodec {

  /** Witness that `A` is an Keyederated type. */
  trait IsEnum[A] {
    def key(a: A): String
    def lookup(key: String): Option[A]
  }
  object IsEnum {

    // Java enums get an IsEnum for free, as long as we know the class.
    implicit def fromEnum[E <: Enum[_]](
      implicit ct: reflect.ClassTag[E]
    ): IsEnum[E] =
      new IsEnum[E] {
        def key(a: E): String   = a.name
        def lookup(key: String) =
          ct.runtimeClass
            .asInstanceOf[Class[E]] // yeah baby
            .getEnumConstants
            .find(_.name == key)
      }

  }

}

object keyed extends KeyedCodec
