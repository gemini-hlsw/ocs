package edu.gemini.spModel.rich.pot

import edu.gemini.pot.sp._
import edu.gemini.spModel.core.{RichSpProgramId, SPProgramID}

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
}