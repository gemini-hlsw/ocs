package edu.gemini.sp.vcs

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version._
import edu.gemini.pot.spdb.{DBLocalDatabase, IDBDatabaseService}
import edu.gemini.sp.vcs.VcsFailure._
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.obs.{ObsPhase2Status, SPObservation}
import edu.gemini.spModel.obscomp.{ProgramNote, SPNote}
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.util.ReadableNodeName
import edu.gemini.util.security.principal.{UserPrincipal, AffiliatePrincipal, StaffPrincipal, ProgramPrincipal}

import org.junit.Assert._

import java.util.UUID
import java.security.Principal

import edu.gemini.spModel.core.{Affiliate, SPProgramID}
import edu.gemini.sp.vcs.log.VcsEventSet
import edu.gemini.spModel.obsrecord.ObsExecStatus
import edu.gemini.pot.spdb.ProgramSummoner.LookupOrFail

import scala.collection.JavaConverters._
import scalaz._
import Scalaz.{id => _, _}

object TestingEnvironment {

  case class ProgContext(name: String, id: SPProgramID, odb: IDBDatabaseService, vcs: VersionControlSystem) {
    def uuid: UUID             = odb.getUuid
    def sp: ISPProgram         = odb.lookupProgramByID(id)
    def lifespanId: LifespanId = sp.getLifespanId

    def delete(key: SPNodeKey): Unit = {
      val n = find(key)
      val p = n.getParent
      p.children = p.children.filter(_.getNodeKey != key)
    }

    def getTitle(of: SPNodeKey = sp.getNodeKey): String =
      find(of).getDataObject.getTitle

    def setTitle(text: String, of: SPNodeKey = sp.getNodeKey): Unit = {
      val n   = find(of)
      val obj = n.getDataObject
      obj.setTitle(text)
      n.setDataObject(obj)
    }

    private def addNote(text: String, to: SPNodeKey, noteType: SPComponentType): ISPNode = {
      val oc = find(to).asInstanceOf[ISPObsComponentContainer]

      val note = odb.getFactory.createObsComponent(sp, noteType, null)
      val obj  = note.getDataObject.asInstanceOf[SPNote]
      obj.setNote(text)
      note.setDataObject(obj)
      oc.addObsComponent(note)
      note
    }

    def addNote(text: String, to: SPNodeKey = sp.getNodeKey): ISPNode =
      addNote(text, to, SPNote.SP_TYPE)

    def addProgramNote(text: String, to: SPNodeKey = sp.getNodeKey): ISPNode =
      addNote(text, to, ProgramNote.SP_TYPE)

    def setNoteText(text: String, key: SPNodeKey): Unit = {
      val n   = find(key)
      val obj = n.getDataObject.asInstanceOf[SPNote]
      obj.setNote(text)
      n.setDataObject(obj)
    }

    def addGroup(): ISPGroup = {
      val grp = odb.getFactory.createGroup(sp, null)
      sp.addGroup(grp)
      grp
    }

    def addObservation(to: SPNodeKey = sp.getNodeKey): ISPObservation = {
      val oc  = find(to).asInstanceOf[ISPObservationContainer]
      val obs = odb.getFactory.createObservation(sp, null)
      oc.addObservation(obs)
      obs
    }

    def getDataObjectField[D <: ISPDataObject, T](k: SPNodeKey, f: D => T): T =
      f(find(k).getDataObject.asInstanceOf[D])

    def getObsField[T](k: SPNodeKey, f: SPObservation => T): T =
      getDataObjectField(k, f)

    def getPhase2Status(k: SPNodeKey): ObsPhase2Status =
      getObsField(k, _.getPhase2Status)

    def setDataObjectField[D <: ISPDataObject](k: SPNodeKey, up: D => Unit): Unit = {
      val n = find(k)
      val dataObj = n.getDataObject.asInstanceOf[D]
      up(dataObj)
      n.setDataObject(dataObj)
    }

    def setObsField(k: SPNodeKey, up: SPObservation => Unit): Unit =
      setDataObjectField(k, up)

    def setPhase2Status(k: SPNodeKey, status: ObsPhase2Status): Unit =
      setObsField(k, _.setPhase2Status(status))

    def getExecStatusOverride(k: SPNodeKey): edu.gemini.shared.util.immutable.Option[ObsExecStatus] =
      getObsField(k, _.getExecStatusOverride)

    def setExecStatusOverride(k: SPNodeKey, status: edu.gemini.shared.util.immutable.Option[ObsExecStatus]): Unit =
      setObsField(k, _.setExecStatusOverride(status))

    def conflictNotes(k: SPNodeKey = sp.getNodeKey): List[Conflict.Note] =
      find(k).getConflicts.notes.toList.asScala.toList

    def find(k: SPNodeKey): ISPNode = {
      def find(k: SPNodeKey, nl: List[ISPNode]): ISPNode =
        nl match {
          case Nil    => throw new RuntimeException("not found: " + k)
          case h :: t => if (h.getNodeKey == k) h
                         else find(k, h.children ++ t)
        }
      find(k, List(sp))
    }

    def findObs(k: SPNodeKey): ISPObservation =
      find(k).asInstanceOf[ISPObservation]

    def move(key: SPNodeKey, to: SPNodeKey): Unit = {
      val n = find(key)
      val p = n.getParent
      p.children = p.children.filterNot(_.getNodeKey == key) // remove from parent
      val t = find(to)
      t.children = n :: t.children
    }

    def copyFrom(that: ProgContext): Unit = {
      odb.remove(sp)
      odb.put(odb.getFactory.copyWithSameKeys(that.sp))
    }
  }

  case class TestVcsServerImpl(odb: IDBDatabaseService, user: Set[Principal]) extends VcsServer {
    private def prog(id: SPProgramID): ISPProgram = odb.lookupProgramByID(id)
    def version(id: SPProgramID): TryVcs[VersionMap] = prog(id).getVersions.right
    def fetch(id: SPProgramID): TryVcs[ISPProgram] = prog(id).right
    def store(p: ISPProgram): TryVcs[VersionMap] = VcsLocking(odb).merge(LookupOrFail, p, user)(Commit)
    def log(p: SPProgramID, offset: Int, length: Int): VcsFailure.TryVcs[(List[VcsEventSet], Boolean)] = (Nil, false).right
  }

  def withTestEnv(p: Principal)(block: TestingEnvironment => Unit): Unit = {
    val env = new TestingEnvironment(p)
    try {
      block(env)
    } finally {
      env.shutdown()
    }
  }

  def withPiTestEnv(block: TestingEnvironment => Unit): Unit =
    withTestEnv(ProgramPrincipal(id))(block)

  def syncTest(p: Principal)(block: TestingEnvironment => Unit): Unit =
    withTestEnv(p) { env =>
      block(env)
      env.versionsEqual()
    }

  def doAs[T](p: Principal)(block: Set[Principal] => T): T =
    block(Set(p))

  def doAsStaff[T](block: Set[Principal] => T): T =
    doAs(StaffPrincipal.Gemini)(block)

  def doAsNgo[T](block: Set[Principal] => T): T =
    doAs(AffiliatePrincipal(Affiliate.CHILE))(block)

  def piSyncTest(block: TestingEnvironment => Unit): Unit =
    syncTest(ProgramPrincipal(id))(block)

  def staffUserSyncTest(email: String)(block: TestingEnvironment => Unit): Unit =
    syncTest(UserPrincipal(email))(block)

  def staffSyncTest(block: TestingEnvironment => Unit): Unit =
    syncTest(StaffPrincipal.Gemini)(block)

  def childKeys(n: ISPNode): List[SPNodeKey]  = n.children.map(_.getNodeKey)
  def keys(l: ISPNode*): List[SPNodeKey] = l.map(_.getNodeKey).toList


  def formatNodeVersions(vv: NodeVersions, f: LifespanId => String): String =
    vv.clocks.keys.toList.sortBy(f).map(u => "%s -> %s".format(f(u), vv.clocks(u))).mkString(", ")

  def formatTree(n: ISPNode, f: ISPNode => String, indent: String): String =
    "%s%s [%s]".format(indent, f(n), n.getNodeKey) +
      (if (n.children.size > 0) "\n" else "") +
      n.children.map(c => formatTree(c, f, indent + "  ")).mkString("\n")

  def formatTree(n: ISPNode, indent: String = ""): String =
    formatTree(n, ReadableNodeName.format, indent)

  def showTree(n: ISPNode, f: ISPNode => String, indent: String) {
    println(formatTree(n, f, indent))
  }

  def showTree(n: ISPNode, indent: String = "") {
    println(formatTree(n, indent))
  }

  def allKeys(n: ISPNode): Set[SPNodeKey] = {
    def allKeys(s: Set[SPNodeKey], n: ISPNode): Set[SPNodeKey] =
      ((s+n.getNodeKey)/:n.children) { (s,c) => allKeys(s,c) }
    allKeys(Set.empty, n)
  }

  def formatState(title: String, ctxList: List[ProgContext]): String = {
    val dbNames = (Map.empty[LifespanId, String]/:ctxList) { (m,c) => m + (c.lifespanId -> c.name) }

    def formatNode(n: ISPNode): String = {
      val jvm = n.getProgram.getVersions
      val vv  = nodeVersions(jvm, n.getNodeKey)
      "%s: %s".format(ReadableNodeName.format(n), formatNodeVersions(vv, dbNames))
    }

    def formatDeletedNodeVersions(n: ISPNode): String = {
      val keySet = allKeys(n)
      val vm     = n.getProgram.getVersions

      (vm.keys.filterNot(keySet.contains) map { k =>
        "%s -> %s".format(k, formatNodeVersions(vm(k), dbNames))
      }).mkString("\n")
    }

    def formatCtx(ctx: ProgContext): String =
      "%s\n%s\n* Deleted\n%s\n".format(ctx.name, formatTree(ctx.sp, formatNode, ""), formatDeletedNodeVersions(ctx.sp))

    "\n\n********** %s **********\n%s".format(title, ctxList.map(formatCtx).mkString("\n\n"))
  }


  val key = new SPNodeKey()
  val id  = SPProgramID.toProgramID("GS-2013A-Q-1")
}

import TestingEnvironment._

case class TestingEnvironment(user: Set[Principal]) {

  lazy val javaUser: java.util.Set[Principal] = user.asJava

  def this(p: Principal) = this(Set(p))
  def this() = this(Set.empty[Principal])

  private val odbCentral = DBLocalDatabase.createTransient()
  odbCentral.put(odbCentral.getFactory.createProgram(key, id))

  private def initContext(name: String, odbClone: IDBDatabaseService = DBLocalDatabase.createTransient(), syncWith: IDBDatabaseService = odbCentral): ProgContext = {
    val vcs = VersionControlSystem(odbClone, TestVcsServerImpl(syncWith, user))

    // Side effect :/ put a copy of the empty program into the local database.
    if (odbClone.lookupProgramByID(id) == null) {
      val \/-(sp) = VcsLocking(odbClone).create(odbCentral.lookupProgramByID(id))
      odbClone.put(sp)
    }

    ProgContext(name, id, odbClone, vcs)
  }

  def useContext(name: String)(block: ProgContext => Unit): Unit = {
    val ctx = initContext(name)
    try {
      block(ctx)
    } finally {
      ctx.odb.getDBAdmin.shutdown()
    }
  }

  val cloned  = initContext("clone")
  val central = initContext("central", odbCentral, cloned.odb)

  assertEquals(cloned.sp.getVersions, central.sp.getVersions)

  def nodeVersionsEqual(key: SPNodeKey, fR: NodeVersions => NodeVersions = identity, fL: NodeVersions => NodeVersions = identity): Unit =
    assertEquals(fR(nodeVersions(central.sp.getVersions, key)), fL(nodeVersions(cloned.sp.getVersions, key)))

  def versionsEqual(): Unit =
    assertEquals(central.sp.getVersions, cloned.sp.getVersions)

  def update(user: Set[Principal]): Unit =
    assertTrue(cloned.vcs.update(id, user).isRight)

  def commit(): Unit =
    assertTrue(cloned.vcs.commit(id).isRight)

  def cantCommit(expected: VcsFailure): Unit =
    cloned.vcs.commit(id) match {
      case -\/(actual) => assertEquals(expected, actual)
      case _ => fail("Shouldn't be able to commit, expecting: " + expected)
    }

  def showState(title: String, ctxList: List[ProgContext] = List(central, cloned)): Unit =
    println(formatState(title, ctxList))

  def shutdown(): Unit = {
    cloned.odb.getDBAdmin.shutdown()
    central.odb.getDBAdmin.shutdown()
  }
}
