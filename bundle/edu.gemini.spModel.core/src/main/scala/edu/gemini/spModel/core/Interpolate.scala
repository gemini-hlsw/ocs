package edu.gemini.spModel.core

import scalaz._, Scalaz._

/** Typeclass for linear interpolation given two points. */
sealed trait Interpolate[A, B] { outer =>

  def interpolate(a: (A, B), b: (A, B), k: A): B

  // contravariant in A
  def contramap[C](f: C => A): Interpolate[C, B] =
    new Interpolate[C, B] {
      def interpolate(a: (C, B), b: (C, B), k: C): B
        = outer.interpolate(a.leftMap(f), b.leftMap(f), f(k))
    }

  // invariant in B
  def xmap[C](f: B => C, g: C => B): Interpolate[A, C] =
    new Interpolate[A, C] {
      def interpolate(a: (A, C), b: (A, C), k: A): C =
        f(outer.interpolate(a.rightMap(g), b.rightMap(g), k))
    }

  // pair with another Interpolate of the same key type
  def zip[C](in: Interpolate[A, C]): Interpolate[A, (B, C)] =
    new Interpolate[A, (B, C)] {
      def interpolate(a: (A, (B, C)), b: (A, (B, C)), k: A): (B, C) =
        (outer.interpolate(a.map(_._1), b.map(_._1), k),
            in.interpolate(a.map(_._2), b.map(_._2), k))
    }

}

object Interpolate {

  /** Convenience method for summoning an instance. */
  def apply[A, B](implicit ev: Interpolate[A, B]): Interpolate[A, B] = ev

  implicit def contravariantInterpolate[T]: Contravariant[({ type l[a] = Interpolate[a, T] })#l] =
    new Contravariant[({ type l[a] = Interpolate[a, T] })#l] {
      def contramap[A, B](r: Interpolate[A, T])(f: B => A): Interpolate[B, T] = r.contramap(f)
    }

  // doh, no InvariantFunctor in scalaz 7.0.3
  // implicit def invariantInterpolate[A]: InvariantFunctor[({ type l[b] = Interpolate[A, b]})#l] =
  //   ???

  implicit def zipInterpolate[T]: Zip[({ type l[b] = Interpolate[T, b] })#l] =
    new Zip[({ type l[b] = Interpolate[T, b] })#l] {
      def zip[A, B](a: => Interpolate[T,A], b: => Interpolate[T,B]): Interpolate[T,(A, B)] = a.zip(b)
    }

  implicit val LongDoubleInterpolation: Interpolate[Long, Double] =
    new Interpolate[Long, Double] {
      def interpolate(a: (Long, Double), b: (Long, Double), c: Long): Double = {
        val ((n1, d1), (n2, d2)) = (a, b)
        d1 + (d2 - d1) * (c.toDouble - n1.toDouble) / (n2.toDouble - n1.toDouble)
      }
    }

  implicit val InterpolateCoordinates: Interpolate[Long, Coordinates] =
    new Interpolate[Long, Coordinates] {
      def interpolate(a: (Long, Coordinates), b: (Long, Coordinates), c: Long): Coordinates = {
        val ((n1, c1), (n2, c2)) = (a, b)
        val f =  ((c.toDouble - n1.toDouble) / (n2.toDouble - n1.toDouble))
        val (da, db) = c1 diff c2
        val da0 = Angle.signedDegrees(da.toDegrees) * f
        val db0 = Angle.signedDegrees(db.toDegrees) * f
        c1.offset(Angle.fromDegrees(da0), Angle.fromDegrees(db0))
      }
    }

}