package edu.gemini.spModel.rich.pot

import edu.gemini.pot.sp._
import edu.gemini.spModel.core.{RichSpProgramId, SPProgramID}

package object sp {
  implicit def progIdWrapper(id: SPProgramID)             = new RichSpProgramId(id)
  implicit def remoteNodeWrapper(node: ISPNode)     = new RichRemoteNode(node)
  implicit def progWrapper(prog: ISPProgram)              = new RichProgram(prog)
  implicit def groupWrapper(grp: ISPGroup)                = new RichGroup(grp)
  implicit def obsWrapper(obs: ISPObservation)            = new RichObservation(obs)
  implicit def seqComponentWrapper(seq: ISPSeqComponent)  = new RichSeqComponent(seq)
  implicit def templateGroupWrapper(tg: ISPTemplateGroup) = new RichTemplateGroup(tg)

  private def locking[A <: ISPNode, B](a: A)(lock: A => Unit, unlock: A => Unit, f: A => B): B = {
    lock(a)
    try { f(a) } finally { unlock(a) }
  }

  def readLocking[A <: ISPNode, B](a: A)(f: A => B): B =
    locking(a)(_.getProgramReadLock(), _.returnProgramReadLock(), f)

  def writeLocking[A <: ISPNode, B](a: A)(f: A => B): B =
    locking(a)(_.getProgramWriteLock(), _.returnProgramWriteLock(), f)
}