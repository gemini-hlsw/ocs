package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp._
import edu.gemini.sp.vcs.diff.VcsFailure._
import edu.gemini.spModel.rich.pot.sp._

import scalaz._
import Scalaz._

/** Describes the modifications required for a local program to complete a
  * merge.
  */
case class MergePlan(update: Tree[MergeNode], delete: Set[Missing]) {

  /** Accepts a program and edits it according to this merge plan. */
  def merge(f: ISPFactory, p: ISPProgram): TryVcs[Unit] = {
    def create(mn: MergeNode): TryVcs[ISPNode] =
      mn match {
        case Modified(k, _, dob, _) =>
          NodeFactory.mkNode(f, p, dob.getType, Some(k)).toRightDisjunction {
            Unexpected("Could not create science program node of type: " + dob.getType)
          }
        case Unmodified(k)          =>
          Unexpected(s"Unmodified node with key $k not found in program ${p.getProgramID}.").left
      }

    // Edit the ISPNode, applying the changes in the MergeNode if any.
    def edit(t: Tree[(MergeNode, ISPNode)]): ISPNode =
      t.rootLabel match {
        case (Modified(_, nv, dob, det), n) =>
          val children = t.subForest.toList.map(edit)
          n <| (_.children = children) <| (_.setDataObjectAndVersion(dob, nv))
        case (Unmodified(_), n) =>
          n
      }

    val nodeMap = p.nodeMap

    // Pair up MergeNodes with their corresponding ISPNode, creating any missing
    // ISPNodes as necessary.
    val mergeTree = update.map { mn =>
      nodeMap.get(mn.key).fold(create(mn)) { _.right }.map {n => (mn, n) }
    }.sequenceU

    // apply the changes
    mergeTree.map { mt =>
      val prog = edit(mt).getProgram
      val vm   = (prog.getVersions/:delete) { (vm, missing) =>
        vm.updated(missing.key, missing.nv)
      }
      prog.setVersions(vm)
    }
  }
}
