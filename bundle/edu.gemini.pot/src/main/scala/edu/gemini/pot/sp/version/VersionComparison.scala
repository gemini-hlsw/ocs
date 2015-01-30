package edu.gemini.pot.sp.version

/** A comparison between two NodeVersions or VersionMaps.
  *
  * This class simply provides a more readable enum of the version comparison
  * outcomes.
  */
sealed trait VersionComparison

import scalaz._
import Scalaz._

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

  def compare(nv0: NodeVersions, nv1: NodeVersions): VersionComparison =
    VersionComparison(nv0.tryCompareTo(nv1))

  def compare(vm0: VersionMap, vm1: VersionMap): VersionComparison =
    VersionComparison(VersionMap.tryCompare(vm0, vm1))

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
