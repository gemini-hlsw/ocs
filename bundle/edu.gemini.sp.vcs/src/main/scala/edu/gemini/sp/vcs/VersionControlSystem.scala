package edu.gemini.sp.vcs

import edu.gemini.spModel.core.SPProgramID
import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.pot.spdb.ProgramSummoner.{IdNotFound, LookupOrFail}
import edu.gemini.sp.vcs.VcsFailure._
import edu.gemini.sp.vcs.log.VcsEventSet

import scalaz._
import Scalaz._
import java.security.Principal

case class VersionControlSystem(odb: IDBDatabaseService, remote: VcsServer) {

  private def lookup(id: SPProgramID): TryVcs[ISPProgram] =
    Option(odb.lookupProgramByID(id)).\/>(SummonFailure(IdNotFound(id)))

  def nodeStatus(id: SPProgramID): TryVcs[(ISPProgram, NodeData.StatusMap)] =
    for {
      localProgram <- lookup(id)
      remoteJvm    <- remote.version(id)
    } yield (localProgram, NodeData.statusMap(localProgram, remoteJvm))

  def programStatus(id: SPProgramID): TryVcs[ProgramStatus] =
    for {
      localProgram <- lookup(id)
      remoteJvm    <- remote.version(id)
    } yield ProgramStatus(localProgram.getVersions, remoteJvm)

  def checkout(id: SPProgramID): TryVcs[ISPProgram] =
    for {
      remoteProgram <- remote.fetch(id)
      localProgram  <- VcsLocking(odb).create(remoteProgram)
    } yield localProgram

  def update(id: SPProgramID, user: Set[Principal]): TryVcs[ISPProgram]   =
    for {
      remoteProgram <- remote.fetch(id)
      localProgram  <- VcsLocking(odb).merge(LookupOrFail, remoteProgram, user)(Update)
    } yield localProgram

  def commit(id: SPProgramID): TryVcs[ISPProgram] =
    for {
      prog <- VcsLocking(odb).copy(id)
      _    <- remote.store(prog)
    } yield prog

  def log(id:SPProgramID, offset:Int, length:Int):TryVcs[(List[VcsEventSet], Boolean)] =
    remote.log(id, offset, length)

}
