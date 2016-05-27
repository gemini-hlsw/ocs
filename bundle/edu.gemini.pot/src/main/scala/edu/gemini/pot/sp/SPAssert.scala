package edu.gemini.pot.sp

import edu.gemini.spModel.rich.pot.sp.remoteNodeWrapper
import scala.annotation.tailrec
import scala.collection.JavaConverters._

object SPAssert {
  /** Asserts that adding `newChild` to `parent` will not duplicate observation
    * numbers in the program tree.
    *
    * @throws SPTreeStateException if the assertion doesn't hold
    */
  def addsNoDuplicateObs(parent: ISPContainerNode, newChild: ISPNode): Unit =
    setsNoDuplicateObs(parent, newChild :: parent.children)

  /** Asserts that setting the children of `parent` to `children` will not
    * duplicate observation numbers in the program tree.
    *
    * @throws SPTreeStateException if the assertion doesn't hold
    */
  def setsNoDuplicateObs[T <: ISPNode](parent: ISPContainerNode, children: java.util.List[T]): Unit =
    setsNoDuplicateObs(parent, children.asScala.toList)

  // Scala implementation used by assertion methods designed for use in Java code.
  private def setsNoDuplicateObs[T <: ISPNode](parent: ISPContainerNode, children: List[T]): Unit = {
    @tailrec
    def go(rem: List[ISPNode], all: Set[Int], dups: Set[Int]): Set[Int] =
      rem match {
        case Nil    => dups
        case h :: t => h match {
          case o: ISPObservation   =>
            val n = o.getObservationNumber
            go(t, all + n, if (all(n)) dups + n else dups)

          case c: ISPContainerNode =>
            val cs = if (c == parent) children else c.children
            go(cs ++ t, all, dups)

          case _                   =>
            go(t, all, dups)
        }
      }

    val dups = go(List(parent.getProgram), Set.empty, Set.empty)

    val msg = dups.size match {
      case 0 => None
      case 1 => Some("There is an existing observation " + dups.mkString(", "))
      case _ => Some("There are existing observations " + dups.mkString(", "))
    }
    msg.foreach { s => throw new SPTreeStateException(s) }
  }
}