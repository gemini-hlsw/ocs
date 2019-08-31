package edu.gemini.spModel.core

import scalaz._
import Scalaz._

/**
 * Simple integer version token (e.g., 1.2.3) used to distinguish VO Table
 * versions.
 */
sealed abstract case class VersionToken private (toNel: NonEmptyList[Int]) {

  // Guaranteed by companion class, sanity checked here.
  assert(toNel.list.all(_ >= 0))

  /**
   * Formats a version token into a series of non-zero integers separated by
   * periods. For example "1.2.3".
   */
  def format: String =
    toNel.list.toList.mkString(".")

}

object VersionToken {

  /**
   * Creates a `VersionToken` from a `NonEmptyList` of `Int` assuming the are
   * all non-negative.
   */
  def fromNel(nel: NonEmptyList[Int]): Option[VersionToken] =
    if (nel.list.all(_ >= 0)) Some(new VersionToken(nel) {}) else None

  /**
   * Creates a `VersionToken` from a series of `Int`, assuming they are all
   * non-negative.
   */
  def fromIntegers(h: Int, t: Int*): Option[VersionToken] =
    fromNel(NonEmptyList(h, t: _*))

  /**
   * Creates a `VersionToken` from a series of `Int`, throwing an exception if
   * any are negative.
   */
  def unsafeFromIntegers(h: Int, t: Int*): VersionToken =
    fromIntegers(h, t: _*)
      .getOrElse(sys.error(s"negative version number(s): " + (h :: t.toList).mkString(", ")))

  /**
   * Parses a String version representation, assuming it is a series of
   * non-negative integers separated by periods.
   */
  def parse(s: String): Option[VersionToken] =
    \/.fromTryCatchNonFatal(s.split('.').map(_.toInt).toList)
      .toOption
      .flatMap {
        case i :: is => fromNel(NonEmptyList(i, is: _*))
        case _       => None
      }

  /**
   * Parses a String version representation, throwing an exception if invalid.
   */
  def unsafeParse(s: String): VersionToken =
    parse(s).getOrElse(sys.error(s"invalid version string '$s'"))


  implicit def VersionTokenOrder: Order[VersionToken] =
    new Order[VersionToken] {
      def order(a: VersionToken, b: VersionToken): Ordering =
        a.toNel.list.toList.zipAll(b.toNel.list.toList, 0, 0)
         .foldMap { case (a0, b0) => Ordering.fromInt(a0 - b0) }
    }
}