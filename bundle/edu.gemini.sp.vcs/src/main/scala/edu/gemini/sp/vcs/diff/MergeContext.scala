package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.version.VersionComparison.{Conflicting, Newer, Older, Same}
import edu.gemini.pot.sp.version._
import edu.gemini.pot.sp.{ISPNode, ISPProgram, SPNodeKey}
import edu.gemini.sp.vcs.diff.Diff.{Present, Missing}
import edu.gemini.spModel.rich.pot.sp._

import scalaz._
import Scalaz._

/** Provides a common interface to queries we must make during the merge process
  * about the local and remote versions of a program.  The local context is
  * based upon the local `ISPProgram` which the remote context is extracted from
  * the `Diff`s we collect from the remote program.
  */
sealed trait ProgContext[A] {
  def get(k: SPNodeKey): Option[A]

  def parent(k: SPNodeKey): Option[SPNodeKey]

  def vm: VersionMap
  def version(k: SPNodeKey): NodeVersions =
    vm.getOrElse(k, EmptyNodeVersions)

  def isKnown(k: SPNodeKey): Boolean =
    vm.contains(k)

  def isDeleted(k: SPNodeKey): Boolean
  def isTreeModified(k: SPNodeKey): Boolean
}

object ProgContext {

  final case class Local(prog: ISPProgram, remoteVm: VersionMap) extends ProgContext[ISPNode] {
    val nodeMap = prog.nodeMap
    val vm = prog.getVersions

    def get(k: SPNodeKey): Option[ISPNode] =
      nodeMap.get(k)

    def parent(k: SPNodeKey): Option[SPNodeKey] =
      for {
        n <- nodeMap.get(k)
        p <- Option(n.getParent)
      } yield p.key

    def isDeleted(k: SPNodeKey): Boolean =
      isKnown(k) && !nodeMap.contains(k)

    def isTreeModified(k: SPNodeKey): Boolean =
      nodeMap.get(k).exists { n =>
        VersionComparison.compare(version(k), remoteVm.get(k) | EmptyNodeVersions) match {
          case Same        => n.children.map(_.key).exists(isTreeModified)
          case Newer       => true
          case Older       => n.children.map(_.key).exists(isTreeModified)
          case Conflicting => true
        }
      }
  }

  final case class Remote(diffs: List[Diff], remoteVm: VersionMap, root: SPNodeKey, localVm: VersionMap) extends ProgContext[Diff] {
    val diffMap: Map[SPNodeKey, Diff] = diffs.map(d => d.key -> d).toMap

    def vm: VersionMap = remoteVm

    def get(k: SPNodeKey): Option[Diff] =
      diffMap.get(k)

    def parent(k: SPNodeKey): Option[SPNodeKey] = remoteParents.get(k)

    private val remoteParents: Map[SPNodeKey, SPNodeKey] = {
      def go(rem: List[SPNodeKey], parents: Map[SPNodeKey, SPNodeKey]): Map[SPNodeKey, SPNodeKey] = {
        rem match {
          case Nil       => parents
          case (k :: ks) =>
            diffMap.get(k) match {
              case None                          =>
                go(ks, parents)
              case Some(Missing(_, _))           =>
                go(ks, parents)
              case Some(Present(_, _, _, cs, _)) =>
                go(cs ++ ks, (parents/:cs) { (pm, c) => pm + (c -> k) })
            }
        }
      }

      go(List(root), Map.empty)
    }

    def isDeleted(k: SPNodeKey): Boolean =
     isKnown(k) && diffMap.get(k).exists(_.isMissing)

    def isTreeModified(k: SPNodeKey): Boolean =
      diffMap.get(k).exists {
        case Missing(_, _)                  => false
        case Present(_, nv, _, children, _) =>
          VersionComparison.compare(localVm.get(k) | EmptyNodeVersions, nv) match {
            case Same        => children.exists(isTreeModified)
            case Newer       => children.exists(isTreeModified)
            case Older       => true
            case Conflicting => true
          }
      }
  }
}

/** Holds the local and remote context for convenience. */
final case class MergeContext(local: ProgContext.Local, remote: ProgContext.Remote)

object MergeContext {
  def apply(prog: ISPProgram, diffs: List[Diff]): MergeContext = {
    val localVm  = prog.getVersions

    // Start with the local version map and apply the differences.  Generally if
    // a diff has EmptyNodeVersions it's because it doesn't exist in the remote
    // program.  TODO: However, for some ridiculous reason that I hope to fix
    // the root node is explicitly reset to EmptyNodeVersions after creation.  Both
    // version maps should contain an entry for the root so we work around this
    // here for now.
    val remoteVm = (localVm/:diffs) { (vm, diff) =>
      if ((diff.nv === EmptyNodeVersions) && /*hack*/ diff.key != prog.key)
        vm - diff.key
      else
        vm.updated(diff.key, diff.nv)
    }

    MergeContext(ProgContext.Local(prog, remoteVm), ProgContext.Remote(diffs, remoteVm, prog.key, localVm))
  }
}