package edu.gemini.sp.vcs

import edu.gemini.pot.sp.version._
import edu.gemini.pot.sp.{ISPNode, ISPObservation, ISPProgram, SPNodeKey}
import edu.gemini.spModel.rich.pot.sp._

import scala.annotation.tailrec
import scalaz.Scalaz._

object ProgramDiff {

  /** Returns a (super-set) of differences between two programs.
    *
    * It is a "super-set" because ultimately not every returned `DiffNode` may
    * be necessary in merging program versions.
    */
  def compare(local: ISPProgram, remoteVm: VersionMap): Vector[DiffNode] = {
    import scala.collection.breakOut
    def toVector[T <: DiffNode](keys: Set[SPNodeKey], f: SPNodeKey => T): Vector[T] =
      keys.map(f)(breakOut): Vector[T]

    val inUse          = inUseDiffs(local, remoteVm)
    val localVm        = local.getVersions
    val localKeys      = localVm.keySet
    val remoteKeys     = remoteVm.keySet

    // Any remote keys that we don't have locally are missing.
    val missingKeys    = remoteKeys &~ localKeys
    val missing        = toVector(missingKeys, k => DiffNode(k, remoteVm(k), Diff.Missing))

    // Take all the local keys, remove those that differ but are still active,
    // then remove all that don't differ.
    val activeDiffKeys = inUse.map(_.key)(breakOut): Set[SPNodeKey]
    val removedKeys    = (localKeys &~ activeDiffKeys).filter { k =>
      remoteVm.get(k).forall(_ != localVm(k))
    }
    val removed        = toVector(removedKeys, k => DiffNode(k, localVm(k), Diff.Removed))

    inUse ++ missing ++ removed
  }

  private def diffOne(n: ISPNode, remoteVm: VersionMap): Option[DiffNode] = {
    lazy val someActive = some(DiffNode(n))
    remoteVm.get(n.getNodeKey).fold(someActive) { remoteNv =>
      if (n.getVersion == remoteNv) none else someActive
    }
  }

  // Observations are atomic.  If anything differs at all in either version copy
  // the entire observation.
  private def diffObs(o: ISPObservation, remoteVm: VersionMap): Vector[DiffNode] = {
    def nodeDiffers(n: ISPNode): Boolean =
      n.getVersion != remoteVm.getOrElse(n.getNodeKey, EmptyNodeVersions)

    def treeDiffers(root: ISPNode): Boolean = {
      @tailrec def go(nodes: List[ISPNode]): Boolean =
        nodes match {
          case Nil     => false
          case n :: ns => nodeDiffers(n) || go(n.children ++ ns)
        }

      go(List(root))
    }

    if (treeDiffers(o)) DiffNode.tree(o) else Vector.empty
  }

  // Differences in in-use nodes rooted at n.
  private def inUseDiffs(n: ISPNode, remoteVm: VersionMap): Vector[DiffNode] =
    n match {
      case o: ISPObservation => diffObs(o, remoteVm)
      case _                 =>
        val childDiffs = (Vector.empty[DiffNode]/:n.children) { (ns, child) =>
          ns ++ inUseDiffs(child, remoteVm)
        }

        // If there is even one descendant that differs, include this node
        // in the results.  Otherwise, only include it if it differs.
        if (childDiffs.isEmpty) diffOne(n, remoteVm).toVector
        else DiffNode(n) +: childDiffs
    }
}
