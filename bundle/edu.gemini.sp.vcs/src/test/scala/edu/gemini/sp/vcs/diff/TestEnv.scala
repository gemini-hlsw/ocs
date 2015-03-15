package edu.gemini.sp.vcs.diff

import java.io.File

import edu.gemini.pot.sp.{ISPObservation, ISPProgram, SPNodeKey}
import edu.gemini.pot.spdb.{DBLocalDatabase, IDBDatabaseService}
import edu.gemini.sp.vcs.diff.VcsFailure.{IdClash, Forbidden, NotFound, VcsException}
import edu.gemini.sp.vcs.log.{VcsEventSet, VcsEvent, VcsOp, VcsLog}
import edu.gemini.spModel.core.{Peer, Affiliate, SPProgramID}
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.gemini.obscomp.SPProgram.PIInfo
import edu.gemini.spModel.obs.{SPObservation, ObsQaState}
import edu.gemini.util.security.principal.{UserPrincipal, GeminiPrincipal, StaffPrincipal}

import java.security.Principal

import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

case class TestPeer(odb: IDBDatabaseService, server: VcsServer, service: Principal => VcsService) {
  def vcs(p: Principal): Vcs =
    new Vcs(VcsAction(Set(p)), server, _ => service(p))

  val superStaffVcs: Vcs = vcs(StaffPrincipal.Gemini)

  def shutdown(): Unit = odb.getDBAdmin.shutdown()

  def spDataObject: SPProgram = prog.getDataObject.asInstanceOf[SPProgram]

  // Lookup the test program
  def prog: ISPProgram = odb.lookupProgram(TestEnv.Key)

  def set(update: SPProgram => Unit): Unit =
    spDataObject <| update |> prog.setDataObject

  // Get the rollover status of the test program (a staff-protected field)
  def rollover: Boolean = spDataObject.getRolloverStatus

  // Set the rollover status of the test program (a staff-protected field)
  def rollover_=(roll: Boolean): Unit = set(_.setRolloverStatus(roll))

  // Get the staff contact
  def contact: String = spDataObject.getContactPerson

  // Set the contact person
  def contact_=(c: String): Unit = set(_.setContactPerson(c))

  // Get the current title of the test program
  def progTitle: String = prog.getDataObject.getTitle

  def progTitle_=(t: String): Unit = set(_.setTitle(t))

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

  def addObservation(): SPNodeKey = {
    val obs = odb.getFactory.createObservation(prog, null)
    prog.addObservation(obs)
    obs.getNodeKey
  }

  def getObsDataObject(k: SPNodeKey): SPObservation =
    obs(k).getDataObject.asInstanceOf[SPObservation]

  def setObsDataObject(k: SPNodeKey, update: SPObservation => Unit): Unit =
    getObsDataObject(k) <| update |> obs(k).setDataObject

  def getQaState(k: SPNodeKey): ObsQaState =
    getObsDataObject(k).getOverriddenObsQaState

  def setQaState(k: SPNodeKey, qa: ObsQaState): Unit =
    setObsDataObject(k, _.setOverriddenObsQaState(qa))

  def obs(k: SPNodeKey): ISPObservation = {
    // this is a test method. we're asserting that the observation with this key
    // exists.  if not the test case fails, which is what should happen.
    prog.getAllObservations.asScala.find(_.getNodeKey == k).get
  }
}

case class TestEnv(local: TestPeer, remote: TestPeer) {
  def shutdown(): Unit = {
    local.shutdown()
    remote.shutdown()
  }
}

object TestEnv {
  val DummyPeer = new Peer("foo", 1234)

  val Key       = new SPNodeKey()
  val ObsKey    = new SPNodeKey()
  val Q1        = SPProgramID.toProgramID("GS-2015B-Q-1")
  val Q2        = SPProgramID.toProgramID("GS-2015B-Q-2")
  val Q3        = SPProgramID.toProgramID("GS-2015B-Q-3")

  val Title     = "The Stranger"
  val PiEmail   = "acamus@ualger.dz"
  val PiInfo    = new PIInfo("Albert", "Camus", PiEmail, "", Affiliate.UNITED_STATES)

  val PiUserPrincipal    = UserPrincipal(PiEmail)
  val StaffEmail         = "joe_astro@gemini.edu"
  val StaffUserPrincipal = UserPrincipal(StaffEmail)
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
      rp.getDataObject.asInstanceOf[SPProgram] <|
        (_.setTitle(Title))                    <|
        (_.setPIInfo(PiInfo))                  <|
        (_.setContactPerson(StaffEmail))
    }
    rp.addObservation(remoteOdb.getFactory.createObservation(rp, ObsKey))
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
