package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version.{VersionMap, NodeVersions, EmptyNodeVersions}
import edu.gemini.sp.vcs.diff.NodeDetail.Obs
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
    // Tries to create an ISPNode from the information in the MergeNode.
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
    def edit(t: Tree[(MergeNode, ISPNode)]): ISPNode = {
      t.rootLabel match {

        case (Modified(_, nv, dob, det), n) =>
          // If it is an observation, set the observation number.
          (det, n) match {
            case (Obs(num), o: ISPObservation) => o.setObservationNumber(num)
            case _                             => // not an observation
          }
          // Update the data object and children.
          n <| (_.setDataObject(dob)) <| (_.children = t.subForest.toList.map(edit))

        case (Unmodified(_), n) =>
          n
      }
    }

    val nodeMap = p.nodeMap

    // Pair up MergeNodes with their corresponding ISPNode, creating any missing
    // ISPNodes as necessary.
    val mergeTree = update.map { mn =>
      nodeMap.get(mn.key).fold(create(mn)) { _.right }.map {n => (mn, n) }
    }.sequenceU

    // Extract the updates to the VersionMap from the MergePlan.
    val vmUpdates: VersionMap = {
      val vm0 = update.foldRight(Map.empty[SPNodeKey, NodeVersions]) { (mn, m) =>
        mn match {
          case Modified(k, nv, _, _) => m.updated(k, nv)
          case _                     => m
        }
      }
      (vm0/:delete) { case (vm1, Missing(k, nv)) => vm1.updated(k, nv) }
    }

    // Apply the changes which mutates the program p.  This is a bit awkward
    // but avoids mutating in map.
    \/.fromTryCatch {
      mergeTree.foreach { mt =>
        edit(mt)
        p.setVersions(p.getVersions ++ vmUpdates)
      }
      mergeTree.map(_ => ())
    }.valueOr(ex => VcsException(ex).left)
  }
}
