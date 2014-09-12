package jsky.app.ot.shared.vcs

import edu.gemini.pot.sp.ISPNode
import edu.gemini.pot.sp.version.{VersionMap, vmChecksum}
import edu.gemini.pot.spdb.{IDBFunctor, IDBDatabaseService, IDBQueryRunner}
import edu.gemini.pot.spdb.IDBFunctor.Priority
import edu.gemini.spModel.core.{Peer, SPProgramID}
import edu.gemini.util.trpc.client.TrpcClient
import edu.gemini.util.trpc.common.Try


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import edu.gemini.util.security.auth.keychain.KeyChain
import java.security.Principal

/**
 * The VersionMapFunctor is used to ask Peers for the VersionMap of one or
 * more programs in a single query rather than having a query per program.
 */
object VersionMapFunctor {
  case class VmUpdate(pid: SPProgramID, vm: VersionMap)

  // A pair of program id and, if known, the checksum of the corresponding
  // VersionMap that we have locally
  type ChecksumPid = (SPProgramID, Option[Long])

  def fun(pids: Seq[ChecksumPid], r: TrpcClient#Remote): List[VmUpdate] = {
    val fun = r[IDBQueryRunner].execute(VersionMapFunctor(pids), null)
    fun.exception.fold(fun.updates) { throw _ }
  }

  def future(kc: KeyChain, peer: Peer, pids: Seq[ChecksumPid]): Future[List[VmUpdate]] =
    TrpcClient(peer).withKeyChain(kc) future { r => fun(pids, r) }

  def exec(kc: KeyChain, peer: Peer, pids: Seq[ChecksumPid]): Try[List[VmUpdate]] =
    TrpcClient(peer).withKeyChain(kc) { r =>  fun(pids, r) }
}

import jsky.app.ot.shared.vcs.VersionMapFunctor._

case class VersionMapFunctor(pids: Seq[ChecksumPid]) extends IDBFunctor {
  private var updates: List[VmUpdate] = Nil
  private var exception: Option[Exception] = None

  def getPriority: Priority = Priority.medium

  def setException(ex: Exception): Unit = exception = Some(ex)

  def execute(db: IDBDatabaseService, node: ISPNode, ps: java.util.Set[Principal]): Unit =
    updates = (List.empty[VmUpdate]/:pids) { case (ups, (pid, check)) =>
        Option(db.lookupProgramByID(pid)).flatMap { p =>
          val vm = p.getVersions
          if (check.exists(_  == vmChecksum(vm))) None
          else Some(VmUpdate(p.getProgramID, vm))
        }.fold(ups) { _ :: ups }
    }
}