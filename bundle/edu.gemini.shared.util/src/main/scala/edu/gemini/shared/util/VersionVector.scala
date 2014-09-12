package edu.gemini.shared.util

import scala.collection.immutable.ListMap
import scala.collection.JavaConverters._
import scala.math

// Idea scala plugin marks this as unused but is needed
import scala.math.Numeric.Implicits._


object VersionVector {
  def empty[K, V : Integral] = VersionVector(ListMap.empty[K, V])
  def apply[K, V : Integral](elems: (K, V)*): VersionVector[K, V] =
    VersionVector(ListMap(elems: _*))


  def javaInt[K](): VersionVector[K, java.lang.Integer]  = empty(IntegerIsIntegral)
  def javaInt[K](m: java.util.Map[K, java.lang.Integer]) = VersionVector(ListMap.empty ++ m.asScala)(IntegerIsIntegral)
}

case class VersionVector[K, V : Integral](clocks: Map[K, V]) extends PartiallyOrdered[VersionVector[K, V]] {
  private def intg = implicitly[Integral[V]]
  private def zero = intg.zero
  private def one  = intg.one

  /**
   * Gets the version value associated with the given key.  A key not known
   * to this version vector is assumed to be 0.
   */
  def apply(k: K): V = clocks.getOrElse(k, zero)

  def +(kv: (K, V)) = updated(kv)
  def -(k: K) = if (clocks.contains(k)) VersionVector(clocks - k) else this
  def updated(kv: (K, V)) = VersionVector(clocks.updated(kv._1, kv._2))

  /**
   * Increment the version value associated with the given key.  A key not known
   * to this version vector will be set to 1.
   */
  def incr(k: K) = VersionVector(clocks.updated(k, this(k) + one))

  /**
   * Returns true if there are no entries in the version vector (that is, if
   * all values are implicitly 0).
   */
  def isEmpty = clocks.size == 0

  /**
   * Increment the version value associated with the given key making it at
   * least the maximum value for any key in the version vector.
   */
//  def maxIncr(k: K) =
//    if (isExclusiveMax(k)) incr(k) else setExclusiveMax(k)

  /**
   * Makes the version value associated with the given key exclusively larger
   * than any other version value in the vector, if not already so.
   */
//  def setExclusiveMax(k: K) = {
//    val otherMaxV = (this - k).max
//    if (intg.gt(this(k), otherMaxV))
//      this
//    else
//      VersionVector(clocks.updated(k, otherMaxV + one))
//  }

  /**
   * Returns true if the version value associated with the given key is
   * larger than all other version values in the vector.
   */
//  def isExclusiveMax(k: K): Boolean = intg.gt(this(k), (this - k).max)

  /**
   * Makes the version value associated with the given key equal to the
   * largest version value in the vector, if not already so.
   */
//  def setMax(k: K) = {
//    val maxV = this.max
//    if (intg.eq(this(k), maxV))
//      this
//    else
//      VersionVector(clocks.updated(k, maxV))
//  }

  /**
   * Gets the max version value in this vector.
   */
//  def max: V = if (clocks.size == 0) zero else clocks.values.max

  /**
   * Combines this vector version with <code>that</code> one.  The vector
   * returned contains all the keys in either vector where the value in each
   * case is the max of the value in either for that key.
   */
  def sync(that: VersionVector[K, V]) =
    VersionVector((clocks/:that.clocks) {(m, tup) =>
      val (k, v) = tup
      m.updated(k, intg.max(v, m.getOrElse(k, zero)))
    })


  /**
   * Tries to determine what order two version vectors come in, if possible. Two
   * version vectors are incompatible and considered to be in conflict if they
   * cannot be ordered (in which case None is returned).
   */
  def tryCompareTo[B >: VersionVector[K, V]](that: B)(implicit ev: (B) => math.PartiallyOrdered[B]): Option[Int] =
    that match {
      case vv: VersionVector[K, V] => tryCompareVectors(vv)
      case _ => None
    }

  private def combinedKeys(that: VersionVector[K, V]): Set[K] =
    clocks.keySet | that.clocks.keySet

  private def tryCompareVectors(that: VersionVector[K, V]): Option[Int] = {
    val zero: Option[Int] = Some(0)
    (zero/:combinedKeys(that)) {(res, k) =>
      res flatMap { i =>
        val cur = intg.compare(this(k), that(k)).signum
        i.signum match {
          case 0 => Some(cur)
          case x if (cur == 0) || (cur == x) => res
          case _ => None
        }
      }
    }
  }
}