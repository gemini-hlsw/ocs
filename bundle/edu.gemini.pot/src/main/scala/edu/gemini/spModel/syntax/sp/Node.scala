package edu.gemini.spModel.syntax.sp

import edu.gemini.pot.sp.ISPNode

import scalaz._
import Scalaz._
import scalaz.effect.IO


final class NodeOps(val self: ISPNode) {

  def silent[A](a: IO[A]): IO[A] =
    IO(self.setSendingEvents(false)) *> a ensuring IO(self.setSendingEvents(true))

  def locked[A](a: IO[A]): IO[A] =
    IO(self.getProgramWriteLock()) *> a ensuring IO(self.returnProgramWriteLock())

  def silentAndLocked[A](a: IO[A]): IO[A] =
    silent(locked(a))

}

trait ToNodeOps {
  implicit def ToNodeOps(n: ISPNode): NodeOps =
    new NodeOps(n)
}

object node extends ToNodeOps
