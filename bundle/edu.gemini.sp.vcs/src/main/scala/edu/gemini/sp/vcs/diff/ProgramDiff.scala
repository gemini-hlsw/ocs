package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.version._
import edu.gemini.pot.sp.{ISPNode, ISPObservation, ISPProgram, SPNodeKey}
import edu.gemini.spModel.rich.pot.sp._

import scala.annotation.tailrec
import scalaz.Scalaz._

object ProgramDiff {

  import Diff.{missing, present}

  /** Returns a (super-set) of differences between two programs.
    *
    * It is a "super-set" because ultimately not every returned `Diff` may be
    * necessary in merging program versions.
    */
  def compare(local: ISPProgram, remoteVm: VersionMap): List[Diff] = {
    import scala.collection.breakOut
    def toList(keys: Set[SPNodeKey], f: SPNodeKey => Diff): List[Diff] =
      keys.map(f)(breakOut)

    val presentDiffs    = findDiffs(local, remoteVm)
    val localVm         = local.getVersions
    val localKeys       = localVm.keySet
    val remoteKeys      = remoteVm.keySet

    // Any remote keys that we don't have locally are missing.
    val missingKeys     = remoteKeys &~ localKeys
    val missingDiffs    = toList(missingKeys, k => missing(k, remoteVm(k)))

    // Take all the local keys, remove those that differ but are still active,
    // then remove all that don't differ.
    val presentDiffKeys = presentDiffs.map(_.key)(breakOut): Set[SPNodeKey]
    val removedKeys     = (localKeys &~ presentDiffKeys).filter { k =>
      remoteVm.get(k).forall(_ != localVm(k))
    }
    val removedDiffs    = toList(removedKeys, k => missing(k, localVm(k)))

    removedDiffs ++ missingDiffs ++ presentDiffs
  }

  private def diffOne(n: ISPNode, remoteVm: VersionMap): Option[Diff] = {
    lazy val someActive = some(present(n))
    remoteVm.get(n.getNodeKey).fold(someActive) { remoteNv =>
      if (n.getVersion == remoteNv) none else someActive
    }
  }

  // Observations are atomic.  If anything differs at all in either version copy
  // the entire observation.
  private def diffObs(o: ISPObservation, remoteVm: VersionMap): List[Diff] = {
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

    if (treeDiffers(o)) Diff.tree(o) else List.empty
  }

  // Differences in in-use nodes rooted at n.
  private def findDiffs(n: ISPNode, remoteVm: VersionMap): List[Diff] =
    n match {
      case o: ISPObservation => diffObs(o, remoteVm)
      case _                 =>
        val childDiffs = (List.empty[Diff]/:n.children) { (ns, child) =>
          findDiffs(child, remoteVm) ++ ns
        }

        // If there is even one descendant that differs, include this node
        // in the results.  Otherwise, only include it if it differs.
        if (childDiffs.isEmpty) diffOne(n, remoteVm).toList
        else present(n) :: childDiffs
    }
}
