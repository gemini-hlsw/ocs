package edu.gemini.sp.vcs.diff

import java.io.File

import edu.gemini.pot.sp.{ISPProgram, SPNodeKey}
import edu.gemini.pot.spdb.{DBLocalDatabase, IDBDatabaseService}
import edu.gemini.sp.vcs.diff.VcsFailure.{IdClash, Forbidden, NotFound, VcsException}
import edu.gemini.sp.vcs.log.{VcsEventSet, VcsEvent, VcsOp, VcsLog}
import edu.gemini.spModel.core.{Peer, Affiliate, SPProgramID}
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.gemini.obscomp.SPProgram.PIInfo
import edu.gemini.util.security.principal.{ProgramPrincipal, GeminiPrincipal, StaffPrincipal}

import java.security.Principal

import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import scalaz._
import Scalaz._

case class TestPeer(odb: IDBDatabaseService, server: VcsServer, service: Principal => VcsService) {
  def vcs(p: Principal): Vcs =
    new Vcs(VcsAction(Set(p)), server, _ => service(p))

  val staffVcs: Vcs = vcs(StaffPrincipal.Gemini)

  def shutdown(): Unit = odb.getDBAdmin.shutdown()

  // Lookup the test program
  def prog: ISPProgram = odb.lookupProgram(TestEnv.Key)

  // Get the current title of the test program
  def progTitle: String = prog.getDataObject.getTitle

  def progTitle_=(t: String): Unit =
    prog.getDataObject <| (_.setTitle(t)) |> prog.setDataObject

  // Create a new program but don't add it to the database
  def newProgram(id: SPProgramID): ISPProgram = {
    val fact = odb.getFactory
    val key  = new SPNodeKey()
    fact.createProgram(key, id)
  }

  def addProgram(p: ISPProgram): Unit =
    odb.put(p)

  // Create and add a new program to the database
  def addNewProgram(id: SPProgramID): ISPProgram =
    newProgram(id) <| addProgram
}

case class TestEnv(local: TestPeer, remote: TestPeer) {
  def shutdown(): Unit = {
    local.shutdown()
    remote.shutdown()
  }
}

object TestEnv {
  val Key       = new SPNodeKey()
  val Q1        = SPProgramID.toProgramID("GS-2015B-Q-1")
  val Title     = "The Stranger"
  val PiInfo    = new PIInfo("Albert", "Camus", "acamus@ualger.dz", "", Affiliate.UNITED_STATES)

  val DummyPeer = new Peer("foo", 1234)

  val Q2        = SPProgramID.toProgramID("GS-2015B-Q-2")
  val Q3        = SPProgramID.toProgramID("GS-2015B-Q-3")

  def progPrincipal(id: SPProgramID): Set[Principal] =
    Set(ProgramPrincipal(id))
}

object MockVcsLog extends VcsLog {
  override def log(op: VcsOp, pid: SPProgramID, principals: Set[GeminiPrincipal]): VcsEvent =
    VcsEvent(0, op, 0, pid, principals)

  override def archive(f: File): Unit = ()

  override def selectByProgram(pid: SPProgramID, offset: Int, size: Int): (List[VcsEventSet], Boolean) =
    (Nil, false)
}


trait VcsSpecification extends Specification {

  private def newTestEnv: TestEnv = {
    val localOdb  = DBLocalDatabase.createTransient()
    val remoteOdb = DBLocalDatabase.createTransient()

    // Initialize the remote database with a test program
    import TestEnv._
    val rp = remoteOdb.getFactory.createProgram(Key, Q1)
    rp.setDataObject {
      rp.getDataObject.asInstanceOf[SPProgram] <| (_.setTitle(Title)) <| (_.setPIInfo(PiInfo))
    }
    remoteOdb.put(rp)

    // Copy the test program into the local database
    val lp = localOdb.getFactory.copyWithNewLifespanId(rp)
    localOdb.put(lp)

    val localServer  = new VcsServer(localOdb,  MockVcsLog)
    val remoteServer = new VcsServer(remoteOdb, MockVcsLog)

    val local  = TestPeer(localOdb,  localServer,  p => new remoteServer.SecureVcsService(Set(p)))
    val remote = TestPeer(remoteOdb, remoteServer, p => new localServer.SecureVcsService(Set(p)))

    TestEnv(local, remote)
  }

  def withVcs[A](body: TestEnv => A): A = {
    val env = newTestEnv
    try {
      body(env)
    } finally {
      env.shutdown()
    }
  }

  def expect[A](act: VcsAction[A])(pf: PartialFunction[TryVcs[A], MatchResult[_]]): MatchResult[_] = {
    import VcsAction._

    val t = act.unsafeRun
    if (pf.isDefinedAt(t))
      pf(t)
    else {
      t match {
        case -\/(VcsException(ex)) => ex.printStackTrace()
      }
      ko("unexpected result: " + t)
    }
  }

  def notFound[A](act: VcsAction[A], id: SPProgramID): MatchResult[_] =
    expect(act) { case -\/(NotFound(x)) => x must_== id }

  def forbidden[A](act: VcsAction[A]): MatchResult[_] =
    expect(act) { case -\/(Forbidden(m)) => ok(m) }

  def idClash[A](act: VcsAction[A], id: SPProgramID): MatchResult[_] =
    expect(act) { case -\/(IdClash(i,_,_)) => i must_== id }

  def exception[A](act: VcsAction[A], msg: String): MatchResult[_] =
    expect(act) {
      case -\/(VcsException(rte: RuntimeException)) => rte.getMessage must_== msg
    }
}
