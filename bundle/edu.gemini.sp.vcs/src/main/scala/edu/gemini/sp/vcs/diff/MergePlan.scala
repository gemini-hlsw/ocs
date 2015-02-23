package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version.{VersionMap, NodeVersions}
import edu.gemini.sp.vcs.diff.NodeDetail.Obs
import edu.gemini.sp.vcs.diff.VcsFailure._
import edu.gemini.spModel.rich.pot.sp._

import scalaz._
import Scalaz._

/** Describes the modifications required for a local program to complete a
  * merge.
  */
case class MergePlan(update: Tree[MergeNode], delete: Set[Missing]) {

  /** "Encode" for serialization. The issue is that `scalaz.Tree` is not
    * `Serializable` but we need to send `MergePlan`s over `trpc`.
    */
  def encode: MergePlan.Transport = {
    def encodeTree(t: Tree[MergeNode]): MergePlan.TreeTransport =
      MergePlan.TreeTransport(t.rootLabel, t.subForest.toList.map(encodeTree))

    MergePlan.Transport(encodeTree(update), delete)
  }

  /** Gets the `VersionMap` of the provided program as it will be after the
    * updates in this plan have been applied. */
  def vm(p: ISPProgram): VersionMap = {
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

    p.getVersions ++ vmUpdates
  }

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
    def edit(t: Tree[(MergeNode, ISPNode)]): Unit = {
      t.rootLabel match {

        case (Modified(_, nv, dob, det), n) =>
          n.setDataObject(dob)

          // If it is an observation, set the observation number.
          (det, n) match {
            case (Obs(num), o: ISPObservation) => o.setObservationNumber(num)
            case _                             => // not an observation
          }

          // Edit then set the children.
          t.subForest.foreach(edit)
          n.children = t.subForest.toList.map(_.rootLabel._2)

        case (Unmodified(_), _) => // do nothing
      }
    }

    val nodeMap = p.nodeMap

    // Pair up MergeNodes with their corresponding ISPNode, creating any missing
    // ISPNodes as necessary.
    val mergeTree = update.map { mn =>
      nodeMap.get(mn.key).fold(create(mn)) { _.right }.map {n => (mn, n) }
    }.sequenceU


    // Apply the changes which mutates the program p.  This is a bit awkward
    // but avoids mutating in map.
    \/.fromTryCatch {
      mergeTree.foreach { mt =>
        edit(mt)
        p.setVersions(vm(p))
      }
      mergeTree.map(_ => ())
    }.valueOr(ex => VcsException(ex).left)
  }
}

object MergePlan {

  /** A serializable Tree[MergeNode].  Sadly scalaz.Tree is not serializable. */
  case class TreeTransport(mn: MergeNode, children: List[TreeTransport]) {
    def decode: Tree[MergeNode] = Tree.node(mn, children.map(_.decode).toStream)
  }

  /** A serializable MergePlan.  Sadly the Tree[MergeNode] contained in the
    * MergePlan is not serializable.
    */
  case class Transport(update: TreeTransport, delete: Set[Missing]) {
    def decode: MergePlan = MergePlan(update.decode, delete)
  }
}
