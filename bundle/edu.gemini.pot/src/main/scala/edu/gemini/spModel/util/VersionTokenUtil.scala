package edu.gemini.spModel.util

import scala.collection.immutable.TreeMap

/**
 * Utilities for normalizing and merging lists of VersionTokens.
 */
object VersionTokenUtil {
  type VersionTokenData[A] = (VersionToken, A)

  private object Node {
    def noChildren[A]           = TreeMap.empty[Int, Node[A]]
    def oneChild[A](n: Node[A]) = TreeMap(n.segment -> n)
    def root[A]                 = new Node[A](0, Option.empty, noChildren)

    def apply[A](token: VersionToken, a: A): Node[A] = {
      def toNode(segment: Int, segments: List[Int]): Node[A] =
        segments match {
          case Nil    => Node(segment, Some(a), noChildren)
          case h :: t => Node(segment, Option.empty, oneChild(toNode(h, t)))
        }

      toNode(0, token.getSegments.toList)
    }
  }

  private case class Node[A](segment: Int, payload: Option[A], children: TreeMap[Int, Node[A]]) {
    def nextSegment: Int = children.lastOption.map(_._1).getOrElse(0) + 1
    def put(child: Node[A]): Node[A] = copy(children = children.updated(child.segment, child))

    def merge(that: Node[A]): Node[A] = {
      def mergeChild(parent: Node[A], newChild: Node[A]): Node[A] = {
        val child = parent.children.get(newChild.segment).fold(newChild) { n =>
          (n.payload, newChild.payload) match {
            case (None, Some(a))              => n.copy(payload = Some(a)).merge(newChild)
            case (Some(a), Some(b)) if a != b => newChild.copy(segment = parent.nextSegment)
            case _                            => n.merge(newChild)
          }
        }
        parent.put(child)
      }

      (this/:that.children.values) { mergeChild }
    }

    def unTree: List[VersionTokenData[A]] = {
      def traverse(n: Node[A], data: List[VersionTokenData[A]], parentSegments: Vector[Int]): List[VersionTokenData[A]] = {
        val seg = parentSegments :+ n.segment
        val res = (n.children.values:\data) { traverse(_,_,seg) }
        n.payload.fold(res) { a =>
          (VersionToken.apply(seg.toArray, n.nextSegment), a) :: res
        }
      }

      (children.valuesIterator:\List.empty[VersionTokenData[A]]) { traverse(_, _, Vector.empty) }
    }
  }

  private def mkTree[A](data: List[VersionTokenData[A]]): Node[A] =
    (Node.root[A]/:data) { case (n, (token, a)) => n.merge(Node(token, a)) }

  /**
   * Normalize a list of (VersionToken, A) pairs, putting them into order
   * according to VersionToken and removing duplicate tokens with the same
   * data and making duplicate tokens with different data unique by incrementing
   * the appropriate token segments.  Note, if there are two 1.1 VersionTokens,
   * one will be renumbered but any child 1.1.x values will remain children of
   * 1.1.  (There's no way to know *which* 1.1 they belonged to.)
   */
  def normalize[A](data: List[VersionTokenData[A]]): List[VersionTokenData[A]] =
    mkTree(data).unTree

  /**
   * Merge two lists of (VersionToken, A) into a single normalized list of
   * data.  If the second list contains a duplicate it will be renumbered
   * appropriately to make it unique, along with any child VersionTokens.  For
   * example List((1.1, a)) merged with List((1.1, b), (1.1.1, c)) will become
   * List((1.1, a), (1.2, b), (1.2.1, c)).
   */
  def merge[A](data0: List[VersionTokenData[A]], data1: List[VersionTokenData[A]]): List[VersionTokenData[A]] =
    mkTree(data0).merge(mkTree(data1)).unTree
}