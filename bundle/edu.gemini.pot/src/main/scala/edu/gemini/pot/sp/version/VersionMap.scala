package edu.gemini.pot.sp.version

import edu.gemini.shared.util.VersionComparison

/**
 *
 */
object VersionMap {
  val ordering = new PartialOrdering[VersionMap]() {

    // If both are the same, keep the same value.  If either is zero, favor
    // the other.  Otherwise, they conflict.
    private def combine(i0: Int, i1: Int): Option[Int] = (i0, i1) match {
      case (a,b) if a == b => Some(a)
      case (a,0)           => Some(a)
      case (0,b)           => Some(b)
      case _               => None
    }

    def tryCompare(xvm: VersionMap, yvm: VersionMap): Option[Int] = {
      val zero: Option[Int] = Some(0)
      (zero/:(xvm.keySet ++ yvm.keySet)) { (iopt,key) =>
        for {
          a <- iopt
          b <- xvm.getOrElse(key, EmptyNodeVersions).tryCompareTo(yvm.getOrElse(key, EmptyNodeVersions))
          c <- combine(a, b)
        } yield c
      }
    }

    def lteq(xvm: VersionMap, yvm: VersionMap): Boolean = {
      // a less efficient (probably) but more concise (definitely) way to do this
      // tryCompare(x, y) exists { _ <= 0 }

      // It is <= if there is no (key, version vector) pair for which either
      // there is no corresponding version vector in y or for which the the
      // corresponding version vector is <
      !(xvm exists { case (key, xvv) =>
        yvm.get(key) forall { yvv => xvv.tryCompareTo(yvv) forall { _ > 0} }
      })
    }
  }

  def tryCompare(x: VersionMap, y: VersionMap): Option[Int] =
    ordering.tryCompare(x, y)

  def tryCompare(x: Option[VersionMap], y: Option[VersionMap]): Option[Int] =
    for {
      xVm <- x
      yVm <- y
      res <- tryCompare(xVm, yVm)
    } yield res

  def isNewer(newJvm: VersionMap, oldJvm: VersionMap): Boolean =
    VersionMap.tryCompare(newJvm, oldJvm).forall(_ > 0)

  def isNewer(newJvm: Option[VersionMap], oldJvm: Option[VersionMap]): Boolean =
    VersionMap.tryCompare(newJvm, oldJvm).forall(_ > 0)

  def compare(x: VersionMap, y: VersionMap): VersionComparison =
    VersionComparison(tryCompare(x,y))
}
