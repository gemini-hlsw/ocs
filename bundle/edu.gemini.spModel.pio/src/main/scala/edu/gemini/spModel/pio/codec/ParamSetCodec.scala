package edu.gemini.spModel.pio.codec

import scalaz._, Scalaz._

import scala.collection.JavaConverters._
import edu.gemini.spModel.pio._
import edu.gemini.spModel.pio.xml.PioXmlFactory

trait ParamSetCodec[A] { outer =>
  
  def encode(key: String, a: A): ParamSet
  
  def decode(ps: ParamSet): PioError \/ A

  def unsafeDecode(ps: ParamSet): A =
    outer.decode(ps).fold(e => throw new PioParseException(e.toString), a => a)

  def xmap[B](f: A => B, g: B => A): ParamSetCodec[B] =
    new ParamSetCodec[B] {
      def encode(key: String, b: B) = outer.encode(key, g(b))
      def decode(ps: ParamSet)  = outer.decode(ps).map(f)
    }

  def withParam[B](key: String, lens: A @> B)(implicit pc: ParamCodec[B]): ParamSetCodec[A] =
    new ParamSetCodec[A] {
      def encode(key0: String, a: A): ParamSet = {
        val ps = outer.encode(key0, a)
        val p  = pc.encode(key, lens.get(a))
        ps.addParam(p)
        ps
      }
      def decode(ps: ParamSet): PioError \/ A =
        for {
          a <- outer.decode(ps)
          p <- Option(ps.getParam(key)) \/> MissingKey("withParam: " + key)
          b <- pc.decode(p)
        } yield lens.set(a, b)
    }

  def withOptionalParam[B](key: String, lens: A @> Option[B])(implicit pc: ParamCodec[B]): ParamSetCodec[A] =
    new ParamSetCodec[A] {
      def encode(key0: String, a: A): ParamSet = {
        val ps = outer.encode(key0, a)
        lens.get(a).foreach { a =>
          val p  = pc.encode(key, a)
          ps.addParam(p)
        }
        ps
      }
      def decode(ps: ParamSet): PioError \/ A =
        outer.decode(ps) flatMap { a =>
          Option(ps.getParam(key)) match {
            case None     => \/-(lens.set(a, none))
            case Some(ps) => pc.decode(ps).map(b => lens.set(a, some(b)))
          }
        }
    }

  def withParamSet[B](key: String, lens: A @> B)(implicit psc: ParamSetCodec[B]): ParamSetCodec[A] =
    new ParamSetCodec[A] {
      def encode(key0: String, a: A): ParamSet = {
        val ps0 = outer.encode(key0, a)
        val ps1 = psc.encode(key, lens.get(a))
        ps0.addParamSet(ps1)
        ps0
      }
      def decode(ps: ParamSet): PioError \/ A =
        for {
          a <- outer.decode(ps)
          p <- Option(ps.getParamSet(key)) \/> MissingKey("withParamSet: " + key)
          b <- psc.decode(p)
        } yield lens.set(a, b)
    }

  def withOptionalParamSet[B](key: String, lens: A @> Option[B])(implicit psc: ParamSetCodec[B]): ParamSetCodec[A] =
    new ParamSetCodec[A] {
      def encode(key0: String, a: A): ParamSet =
        outer.encode(key0, a) <| { ps => 
          lens.get(a).foreach { b =>
            psc.encode(key, b) <| ps.addParamSet
          }
        }
      def decode(ps: ParamSet): PioError \/ A =
        outer.decode(ps).flatMap { a =>
          Option(ps.getParamSet(key)) match {
            case None    => \/-(lens.set(a, none))
            case Some(p) => psc.decode(p).map(b => lens.set(a, some(b)))
          }
        }
    }

  def withManyParamSet[B](key: String, lens: A @> List[B])(implicit psc: ParamSetCodec[B]): ParamSetCodec[A] =
    new ParamSetCodec[A] {
      def encode(key0: String, a: A): ParamSet = {
        val ps0 = outer.encode(key0, a)
        lens.get(a).foreach { b =>
          val ps1 = psc.encode(key, b)
          ps0.addParamSet(ps1)
        }
        ps0
      }
      def decode(ps: ParamSet): PioError \/ A =
        outer.decode(ps).flatMap { a =>
          ps.getParamSets(key).asScala.toList.traverseU(psc.decode).map(lens.set(a, _))
        }
    }

}

object ParamSetCodec {

  val pf = new PioXmlFactory

  def apply[A](implicit ev: ParamSetCodec[A]): ParamSetCodec[A] = ev

  def initial[A](empty: A): ParamSetCodec[A] =
    new ParamSetCodec[A] {
      def encode(key: String, a: A): ParamSet = pf.createParamSet(key)
      def decode(ps: ParamSet): PioError \/ A = empty.right
    }

  implicit val InvariantParamSetCodec: InvariantFunctor[ParamSetCodec] =
    new InvariantFunctor[ParamSetCodec] {
      def xmap[A, B](ma: ParamSetCodec[A], f: (A) => B, g: (B) => A): ParamSetCodec[B] =
        ma.xmap(f, g)
    }

}
