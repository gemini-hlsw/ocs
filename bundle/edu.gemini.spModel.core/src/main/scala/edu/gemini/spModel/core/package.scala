package edu.gemini.spModel

import scala.math.Numeric.Implicits._
import scala.util.control.NonFatal
import scalaz._, Scalaz._
import scala.collection.immutable.NumericRange

package object core {

  /** Turns any non-fatal exceptions in the given block into lefts.
    * This is the same as scalaz `\/.fromTryCatchNonFatal`, so we should
    * switch to that at some point after we update to a newer version.
    */
  def catchingNonFatal[T](a: => T): Throwable \/ T =
    try {
      \/-(a)
    } catch {
      case NonFatal(t) => -\/(t)
    }

  type RA = RightAscension
  val  RA: RightAscension.type = RightAscension

  type Dec = Declination
  val  Dec: Declination.type = Declination

  implicit def SPProgramIdToRichProgramId(id: SPProgramID): RichSpProgramId =
    RichSpProgramId(id)

  implicit val OrderSPProgramID: Order[SPProgramID] =
    Order.fromScalaOrdering(scala.math.Ordering.ordered[SPProgramID])

  implicit val OrderSemester: Order[Semester] =
    Order.fromScalaOrdering(scala.math.Ordering.ordered[Semester])

  /** Operations for maps of types that we can interpolate.  We use these for Ephemerides. */
  implicit class MoreMapOps[K, V](m: K ==>> V)(implicit O: Order[K], I: Interpolate[K,V]) {

    /** Perform an exact or interpolated lookup. */
    def iLookup(k: K): Option[V] =
      m.lookup(k) match {
        case Some(v) => Some(v)
        case None    =>
          val (lt, gt) = m.split(k)
          for {
            a <- lt.findMax
            b <- gt.findMin
            c <- I.interpolate(a, b, k)
          } yield c
      }

    /** Construct an exact or interpolated slice. */
    def iSlice(lo: K, hi: K): Option[K ==>> V] =
      ^(iLookup(lo), iLookup(hi)) { (lov, hiv) =>
        m.filterWithKey((k, _) => O.greaterThanOrEqual(k, lo) && O.lessThanOrEqual(k, hi)) + (lo -> lov) + (hi -> hiv)
      }

    /** Construct a table of (K, V) values on the given interval. */
    def iTable(lo: K, hi: K, step: K)(implicit ev: Integral[K]): Option[List[(K, V)]] =
      NumericRange.inclusive(lo, hi, step).toList.traverse(k => iLookup(k).strengthL(k))

  }

  /** For keys you can subtract we can find the closest matching pair.  */
  implicit class NumericKeyedMapOps[K, V](m: K ==>> V)(implicit N: Numeric[K]) {
    implicit val KOrder: Order[K] = Order.fromScalaOrdering(N)

    /** Find the closest matching pair, if any. */
    def lookupClosestAssoc(k: K): Option[(K, V)] =
      m.foldlWithKey(Option.empty[(K, V)]) {
        case (None, k, v)      => Some((k, v))
        case (a @ Some((k0, v0)), k1, v1) =>
          if ((k0 - k).abs <= (k1 - k).abs) a
          else Some((k1, v1))
      }

    /** Find the closest matching value, if any. This is O(N) */
    def lookupClosest(k: K): Option[V] =
      lookupClosestAssoc(k).map(_._2)

    /** Find the closest matching value, if any. */
    def lookupClosestKey(k: K): Option[K] =
      lookupClosestAssoc(k).map(_._1)

  }

}

