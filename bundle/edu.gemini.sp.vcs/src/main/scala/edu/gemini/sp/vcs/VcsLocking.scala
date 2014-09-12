package edu.gemini.sp.vcs

import edu.gemini.pot.spdb.{IDBDatabaseService, Locking, ProgramSummoner}
import edu.gemini.pot.spdb.Locking.{commit,discard}
import edu.gemini.pot.spdb.ProgramSummoner._
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.pot.sp.{ISPNode, ISPProgram}
import edu.gemini.sp.vcs.VcsFailure._
import edu.gemini.spModel.rich.pot.sp._

import scalaz._
import Scalaz._
import java.security.Principal


object VcsLocking {
  type MergeOp[T] = (IDBDatabaseService, ISPProgram, ISPProgram, Set[Principal]) => TryVcs[T]
}

import VcsLocking._

case class VcsLocking(odb: IDBDatabaseService) {
  private val locking = new Locking(odb)

  def copy(id: SPProgramID): TryVcs[ISPProgram] =
    locking.copy(id).left.map(SummonFailure).disjunction

  private def toTry[T](e: Either[ProgramSummoner.Failure, TryVcs[T]]): TryVcs[T] =
    e.left.map(f => SummonFailure(f).left).merge

  def merge[T](sum: ProgramSummoner, input: ISPProgram, user: Set[Principal])(op: MergeOp[T]): TryVcs[T] =
    toTry(locking.writeCopy(sum, input.getProgramKey, input.getProgramID, copy => {
      val res = op(odb, input, copy, user)
      res.fold(_ => discard(res), _ => commit(res))
    }))

  def create(input: ISPProgram): TryVcs[ISPProgram] =
    toTry(locking.write(CreateOrFail, input.getProgramKey, input.getProgramID, prog => {
      val cp = EmptyNodeCopier(odb.getFactory, prog)
      def copy(in: ISPNode, ex: ISPNode) {
        ex.setDataObject(in.getDataObject)
        val inChildren = in.children
        val exChildren = inChildren map { child => cp(child).get }
        inChildren.zip(exChildren) foreach { case (inChild, exChild) =>
          copy(inChild, exChild)
        }
        ex.children = exChildren
      }
      copy(input, prog)
      prog.renumberObservationsToMatch(input)
      prog.setVersions(input.getVersions)
      prog.right
    }))
}