package edu.gemini.spModel.rich.pot

import edu.gemini.pot.sp._
import edu.gemini.spModel.core.{RichSpProgramId, SPProgramID}
import edu.gemini.spModel.data.ISPDataObject

import scalaz._
import Scalaz._

package object sp {
  @inline implicit def progIdWrapper(id: SPProgramID): RichSpProgramId               = new RichSpProgramId(id)
  @inline implicit def remoteNodeWrapper(node: ISPNode): RichNode                    = new RichNode(node)
  @inline implicit def progWrapper(prog: ISPProgram): RichProgram                    = new RichProgram(prog)
  @inline implicit def groupWrapper(grp: ISPGroup): RichGroup                        = new RichGroup(grp)
  @inline implicit def obsWrapper(obs: ISPObservation): RichObservation              = new RichObservation(obs)
  @inline implicit def seqComponentWrapper(seq: ISPSeqComponent): RichSeqComponent   = new RichSeqComponent(seq)
  @inline implicit def templateGroupWrapper(tg: ISPTemplateGroup): RichTemplateGroup = new RichTemplateGroup(tg)

  private def locking[A <: ISPNode, B](a: A)(lock: A => Unit, unlock: A => Unit, f: A => B): B = {
    lock(a)
    try { f(a) } finally { unlock(a) }
  }

  def readLocking[A <: ISPNode, B](a: A)(f: A => B): B =
    locking(a)(_.getProgramReadLock(), _.returnProgramReadLock(), f)

  def writeLocking[A <: ISPNode, B](a: A)(f: A => B): B =
    locking(a)(_.getProgramWriteLock(), _.returnProgramWriteLock(), f)

  implicit val ShowNode =
    Show.shows[ISPNode] {
      case p: ISPProgram       => s"${p.key} Program ${~Option(p.getProgramID).map(_.toString)}"
      case o: ISPObservation   => s"${o.key} Observation ${o.getObservationNumber}"
      case oc: ISPObsComponent => s"${oc.key} ObsComp(${oc.getType})"
      case sc: ISPSeqComponent => s"${sc.key} SeqComp(${sc.getType})"
      case n                   => s"${n.key} ${n.getClass.getSimpleName}"
    }

  def drawNodeTree(n: ISPNode)(implicit ev: Show[ISPNode]): String = {
    val t = Tree.unfoldTree(n)(n0 => (n0, () => n0.children.toStream))
    t.drawTree(ev).zipWithIndex.collect { case (s, n0) if n0 % 2 == 0 => s}.mkString("\n")
  }

  implicit def SpNodeKeyOrder: Order[SPNodeKey] =
    Order.order((k0, k1) => Ordering.fromInt(k0.compareTo(k1)))

  implicit class RichDataObject[A <: ISPDataObject](dob: A) {
    def copy: A = dob.clone(dob)
  }
}