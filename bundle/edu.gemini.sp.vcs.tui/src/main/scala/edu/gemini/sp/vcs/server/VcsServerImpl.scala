package edu.gemini.sp.vcs.server

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.sp.version._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.pot.spdb.ProgramSummoner.{IdNotFound, LookupOrCreate}
import edu.gemini.sp.vcs._
import edu.gemini.sp.vcs.OldVcsFailure._
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.util.security.permission.ProgramPermission
import edu.gemini.util.security.policy.ImplicitPolicy

import scalaz._
import Scalaz._

import java.security.Principal
import edu.gemini.sp.vcs.log._
import edu.gemini.util.security.principal.GeminiPrincipal

/**
 *
 */
case class VcsServerImpl(odb: IDBDatabaseService, log: VcsLog, user: Set[Principal]) extends VcsServer {

  def geminiPrincipals: Set[GeminiPrincipal] =
    user.collect { case p: GeminiPrincipal => p }

  private def prog(id: SPProgramID): OldVcsFailure \/ ISPProgram =
    Option(odb.lookupProgramByID(id)).\/>(SummonFailure(IdNotFound(id)))

  private def accessControlled[T](id: SPProgramID)(body: => TryVcs[T]): TryVcs[T] =
    if (ImplicitPolicy.hasPermission(odb, user, new ProgramPermission.Read(id)).unsafePerformIO()) {
      body
    } else {
      val idStr = ~Option(id).map(_.toString)
      Forbidden("You don't have permission to access program '%s'".format(idStr)).left
    }

  def version(id: SPProgramID): TryVcs[VersionMap] =
    prog(id).map(_.getVersions)

  def fetch(id: SPProgramID): TryVcs[ISPProgram] =
    accessControlled(id) {
      for {
        a <- prog(id)
        _ <- \/-(log.log(OpFetch, id, geminiPrincipals))
      } yield a
    }

  def store(p: ISPProgram): TryVcs[VersionMap] =
    accessControlled(p.getProgramID) {
      for {
        a <- VcsLocking(odb).merge(LookupOrCreate, p, user)(Commit)
        _ <- \/-(log.log(OpStore, p.getProgramID, geminiPrincipals))
      } yield a
    }

  def log(p: SPProgramID, offset: Int, length: Int): OldVcsFailure.TryVcs[(List[VcsEventSet], Boolean)] =
    try {
      log.selectByProgram(p, offset, length).right
    } catch {
      case e:Exception => OldVcsException(e).left
    }
}

