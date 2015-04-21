package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.version._
import edu.gemini.pot.sp.{ISPNode, ISPProgram, SPNodeKey}
import edu.gemini.spModel.rich.pot.sp._

import scalaz._
import Scalaz._

/** Provides a common interface to queries we must make during the merge process
  * about the local and remote versions of a program.  The local context is
  * based upon the local `ISPProgram` which the remote context is extracted from
  * the `Diff`s we collect from the remote program. */
sealed trait ProgContext {
  def parent(k: SPNodeKey): Option[SPNodeKey]

  def vm: VersionMap
  def version(k: SPNodeKey): NodeVersions = vm.getOrElse(k, EmptyNodeVersions)

  /** Determines whether the given node has been seen before in this program,
    * regardless of whether it is currently present. */
  def isKnown(k: SPNodeKey): Boolean      = vm.contains(k)

  /** Determines whether the given node is still present in this program.
    * Note that `isPresent` implies `isKnown` but not vice versa. */
  def isPresent(k: SPNodeKey): Boolean

  /** Determines whether the given node was previously known in this program
    * but has been deleted.  This is as opposed to a node that is missing but
    * has never been seen before. Here `isDeleted` does not imply `!isKnown`. */
  def isDeleted(k: SPNodeKey): Boolean    = isKnown(k) && !isPresent(k)
}

object ProgContext {

  final case class Local(prog: ISPProgram) extends ProgContext {
    val nodeMap = prog.nodeMap
    val vm = prog.getVersions

    def get(k: SPNodeKey): Option[ISPNode] =
      nodeMap.get(k)

    def parent(k: SPNodeKey): Option[SPNodeKey] =
      for {
        n <- nodeMap.get(k)
        p <- Option(n.getParent)
      } yield p.key

    def isPresent(k: SPNodeKey): Boolean = nodeMap.contains(k)
  }

  final case class Remote(diff: ProgramDiff, remoteVm: VersionMap) extends ProgContext {
    val plan = diff.plan

    val diffMap: Map[SPNodeKey, Tree[MergeNode]] =
      plan.update.foldTree(Map.empty[SPNodeKey, Tree[MergeNode]]) { (t,m) =>
        m + (t.rootLabel.key -> t)
      }

    def vm: VersionMap = remoteVm

    def get(k: SPNodeKey): Option[Tree[MergeNode]] =
      diffMap.get(k)

    def parent(k: SPNodeKey): Option[SPNodeKey] = remoteParents.get(k)

    private val remoteParents: Map[SPNodeKey, SPNodeKey] =
      plan.update.foldTree(Map.empty[SPNodeKey, SPNodeKey]) { (t, m) =>
        val parentKey = t.rootLabel.key
        (m/:t.subForest) { (m2, c) => m2 + (c.rootLabel.key -> parentKey) }
      }

    def isPresent(k: SPNodeKey): Boolean = diffMap.contains(k)
  }
}

/** Holds the local and remote context for convenience. */
final case class MergeContext(local: ProgContext.Local, remote: ProgContext.Remote) {
  def syncVersion(k: SPNodeKey): NodeVersions = local.version(k).sync(remote.version(k))
}

object MergeContext {
  def apply(prog: ISPProgram, diff: ProgramDiff): MergeContext = {
    val localVm  = prog.getVersions

    // Start with the local version map and apply the differences.
    val remoteVm0 = diff.plan.update.sFoldRight(localVm) { (mn,vm) =>
      mn match {
        case m: Modified => vm.updated(mn.key, m.nv)
        case _           => vm
      }
    }

    val remoteVm = (remoteVm0/:diff.plan.delete) { (vm, miss) =>
      if (miss.nv === EmptyNodeVersions) vm - miss.key
      else vm.updated(miss.key, miss.nv)
    }

    MergeContext(ProgContext.Local(prog), ProgContext.Remote(diff, remoteVm))
  }
}