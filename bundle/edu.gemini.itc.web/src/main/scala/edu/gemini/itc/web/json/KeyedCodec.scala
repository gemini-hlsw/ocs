package edu.gemini.itc.web.json

import argonaut._, Argonaut._

trait KeyedCodec {

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

  /** Codec for a Java enum associated with its `name` property. */
  def enumCodec[E <: Enum[E]](
    implicit ct: reflect.ClassTag[E]
  ): CodecJson[E] = {
    val all: Array[E] = ct.runtimeClass.asInstanceOf[Class[E]].getEnumConstants
    keyedCodec[E, String](
      e   => e.name,
      key => all.find(_.name == key)
    )
  }

}

object keyed extends KeyedCodec
