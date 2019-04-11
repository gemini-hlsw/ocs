package edu.gemini.json


import argonaut._, Argonaut._

trait CoproductCodec {

  /**
   * Builder for codecs for coproduct types. No idea why there's not something like this
   * built in. Usage:
   *
   * {{{
   *
   *  sealed trait Foo
   *  case class Bar(n: Int) extends Foo
   *  case class Baz(s: String, b: Boolean) extends Foo
   *
   *  lazy val FooCodec: CodecJson[Foo] =
   *    CoproductCodec[Foo]
   *      .withCase("Bar", casecodec1(Bar.apply, Bar.unapply)("n")) { case b: Bar => b }
   *      .withCase("Baz", casecodec2(Baz.apply, Baz.unapply)("s", "b")) { case b: Baz => b }
   *      .asCodecJson
   *
   * }}}
   */
  class CoproductCodec[A] private { prev =>

    def encode(a: A): Json =
      sys.error("Can't encode. No case was provided for $a.")

    def decode(c: HCursor): DecodeResult[A] =
      DecodeResult.fail[A]("Invalid tagged type", c.history)

    def asCodecJson: CodecJson[A] =
      CodecJson(encode, decode)

    def withCase[B <: A](
      tag:    String,
      codec:  CodecJson[B])(
      select: PartialFunction[A, B]
    ): CoproductCodec[A] =
      new CoproductCodec[A] {

        override def encode(a: A) =
          select.lift(a) match {
            case Some(b) => Json(tag := codec.encode(b))
            case None => prev.encode(a)
          }

        override def decode(c: HCursor): DecodeResult[A] =
          ((c --\ tag).hcursor match {
            case Some(c) => codec.decode(c)
            case None    => DecodeResult.fail[A]("Invalid tagged type", c.history)
          }) ||| prev.decode(c)

      }

  }

  object CoproductCodec {
    def apply[A] = new CoproductCodec[A]
  }

}

object coproduct extends CoproductCodec
