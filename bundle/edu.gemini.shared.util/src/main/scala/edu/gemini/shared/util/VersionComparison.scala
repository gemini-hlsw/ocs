package edu.gemini.shared.util

import scalaz.Scalaz._
import scalaz._

/** A comparison between two NodeVersions or VersionMaps.
  *
  * This class simply provides a more readable enum of the version comparison
  * outcomes.
  */
sealed trait VersionComparison

object VersionComparison {
  case object Same        extends VersionComparison
  case object Newer       extends VersionComparison
  case object Older       extends VersionComparison
  case object Conflicting extends VersionComparison

  def apply(o: Option[Int]): VersionComparison =
    o match {
      case Some(0)          => Same
      case Some(i) if i < 0 => Older
      case Some(i) if i > 0 => Newer
      case None             => Conflicting
    }

  def compare[K, V : Integral](vv0: VersionVector[K, V], vv1: VersionVector[K, V]): VersionComparison =
    VersionComparison(vv0.tryCompareTo(vv1))

  implicit def VersionComparisonEqual: Equal[VersionComparison] = Equal.equalA

  implicit val VersionComparisonMonoid: Monoid[VersionComparison] =
    new Monoid[VersionComparison] {
      val zero = Same
      def append(a: VersionComparison, b: => VersionComparison): VersionComparison =
        (a, b) match {
          case (x,    y) if x === y => x
          case (x, Same)            => x
          case (Same, x)            => x
          case _                    => Conflicting
        }
    }
}
