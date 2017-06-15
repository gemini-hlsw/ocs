package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.version.{LifespanId, EmptyNodeVersions, NodeVersions}
import edu.gemini.pot.sp._
import edu.gemini.pot.spdb.{DBLocalDatabase, IDBDatabaseService}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.sp.vcs2.TestEnv._
import edu.gemini.sp.vcs2.VcsFailure.{IdClash, Forbidden, NotFound, VcsException}
import edu.gemini.sp.vcs.log.{VcsEventSet, VcsEvent, VcsOp, VcsLog}
import edu.gemini.spModel.core.{Peer, Affiliate, SPProgramID}
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.gemini.obscomp.SPProgram.PIInfo
import edu.gemini.spModel.obs.{ObsPhase2Status, SPObservation, ObsQaState}
import edu.gemini.spModel.obscomp.SPNote
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.util.security.principal.{UserPrincipal, GeminiPrincipal, StaffPrincipal}

import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import java.io.File
import java.security.Principal
import java.util.concurrent.atomic.AtomicBoolean

import scala.reflect.ClassTag
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

  def lifespanId: LifespanId = prog.getLifespanId

  def nodeVersions(k: SPNodeKey): NodeVersions =
    prog.toStream.find(_.key == k).map(_.getVersion).getOrElse(EmptyNodeVersions)

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

  def delete(child: SPNodeKey): Unit = {
    val p = descendant(child).getParent
    p.children = p.children.filterNot(_.key == child)
  }

  def move(child: SPNodeKey, to: SPNodeKey): Unit = {
    val c  = descendant(child)
    val p = c.getParent
    p.children = p.children.filterNot(_.key == child)

    val t = descendant(to)
    t.children = c :: t.children
  }

  def addGroup(): SPNodeKey = {
    val grp = odb.getFactory.createGroup(prog, null)
    prog.addGroup(grp)
    grp.key
  }

  def addObservation(to: SPNodeKey = TestEnv.Key): SPNodeKey = {
    val obs  = odb.getFactory.createObservation(prog, null)
    val cont = prog.findDescendant(_.key == to).get.asInstanceOf[ISPObservationContainer]
    cont.addObservation(obs)
    obs.key
  }

  def addNote(title: String, to: SPNodeKey = TestEnv.Key): SPNodeKey = {
    val note = odb.getFactory.createObsComponent(prog, SPNote.SP_TYPE, null)
    note.getDataObject.asInstanceOf[SPNote] <| (_.setTitle(title)) |> (dob => note.setDataObject(dob))

    val cont = prog.findDescendant(_.key == to).get.asInstanceOf[ISPObsComponentContainer]
    cont.addObsComponent(note)
    note.key
  }

  def descendant(k: SPNodeKey): ISPNode = prog.findDescendant(_.key == k).get

  // probably i don't need to bother with the explicit runtime cast here
  def getDataObject[A <: ISPDataObject](k: SPNodeKey)(implicit ev: ClassTag[A]): A =
    ev.runtimeClass.cast(descendant(k).getDataObject).asInstanceOf[A]

  def updateDataObject[A <: ISPDataObject](k: SPNodeKey)(f: A => Unit)(implicit ev: ClassTag[A]): Unit =
    getDataObject[A](k) <| (a => f(a)) |> (dob => descendant(k).setDataObject(dob))

  def obs(k: SPNodeKey): ISPObservation = descendant(k).asInstanceOf[ISPObservation]

  def getObsDataObject(k: SPNodeKey): SPObservation = getDataObject[SPObservation](k)

  def setObsDataObject(k: SPNodeKey)(update: SPObservation => Unit): Unit =
    updateDataObject(k)(update)

  def getQaState(k: SPNodeKey): ObsQaState =
    getObsDataObject(k).getOverriddenObsQaState

  def setQaState(k: SPNodeKey, qa: ObsQaState): Unit =
    setObsDataObject(k)(_.setOverriddenObsQaState(qa))

  def getObsPhase2Status(k: SPNodeKey): ObsPhase2Status =
    getObsDataObject(k).getPhase2Status

  def setObsPhase2Status(k: SPNodeKey, stat: ObsPhase2Status): Unit =
    setObsDataObject(k)(_.setPhase2Status(stat))

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

  override def selectLastSyncTimestamps(): SPProgramID ==>> Map[GeminiPrincipal, Long] =
    ==>>.empty
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

    val localServer  = new VcsServer(localOdb)
    val remoteServer = new VcsServer(remoteOdb)

    val local  = TestPeer(localOdb,  localServer,  p => new remoteServer.SecureVcsService(Set(p), MockVcsLog))
    val remote = TestPeer(remoteOdb, remoteServer, p => new localServer.SecureVcsService(Set(p), MockVcsLog))

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

  // force a sync to setup a test
  def unsafeSync(env: TestEnv): Unit = {
    import VcsAction._

    env.local.vcs(StaffUserPrincipal).sync(Q1, DummyPeer, new AtomicBoolean(false)).unsafeRun match {
      case \/-(_) => ()
      case -\/(f) => sys.error(VcsFailure.explain(f, Q1, "?", Some(DummyPeer)))
    }
  }

  def afterPull(env: TestEnv, p: Principal)(mr: => MatchResult[Any]): MatchResult[_] =
    expect(env.local.vcs(p).pull(Q1, DummyPeer, new AtomicBoolean(false))) { case \/-(_) => mr }

  def afterSync(env: TestEnv, p: Principal)(mr: => MatchResult[Any]): MatchResult[_] =
    expect(env.local.vcs(p).sync(Q1, DummyPeer, new AtomicBoolean(false))) { case \/-(_) => mr }

  def expect[A](act: VcsAction[A])(pf: PartialFunction[TryVcs[A], MatchResult[_]]): MatchResult[_] = {
    import VcsAction._

    val t = act.unsafeRun
    if (pf.isDefinedAt(t))
      pf(t)
    else {
      t match {
        case -\/(VcsException(ex)) => ex.printStackTrace()
        case _                     => // ignore, report the error below
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

  def localHasNote(k: SPNodeKey, n: Conflict.Note, env: TestEnv): MatchResult[_] =
    env.local.descendant(k).getConflicts.notes.asScalaList.contains(n) must beTrue

}
