package edu.gemini.sp.vcs.diff

import java.io.File
import java.security.Principal

import edu.gemini.pot.sp.{DataObjectBlob, ISPFactory, ISPProgram, SPNodeKey}
import edu.gemini.pot.sp.version._
import edu.gemini.pot.spdb.{IDBDatabaseService, DBLocalDatabase}
import edu.gemini.sp.vcs.diff.VcsAction._
import edu.gemini.sp.vcs.diff.VcsFailure._
import edu.gemini.sp.vcs.log.{VcsEventSet, VcsEvent, VcsOp, VcsLog}
import edu.gemini.spModel.core.{Affiliate, SPProgramID}
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.gemini.obscomp.SPProgram.PIInfo
import edu.gemini.util.security.principal.{ProgramPrincipal, UserPrincipal, StaffPrincipal, GeminiPrincipal}
import org.specs2.matcher.MatchResult
import org.specs2.mutable._

import VcsServerSpec._

import scalaz._
import Scalaz._

class VcsServerSpec extends Specification {

  val Key    = new SPNodeKey()
  val ProgId = SPProgramID.toProgramID("GS-2015B-Q-1")
  val Title  = "The Stranger"
  val PiInfo = new PIInfo("Albert", "Camus", "acamus@ualger.dz", "", Affiliate.UNITED_STATES)

  val StaffUser = Set(StaffPrincipal.Gemini): Set[Principal]
  val PiUser    = Set(UserPrincipal(PiInfo.getEmail)): Set[Principal]


  // I think the ForEach context is what I need, but it doesn't seem to exist in
  // this version of specs2.
  def withVcs[A](body: (VcsServer, IDBDatabaseService) => A): A = {
    val odb = DBLocalDatabase.createTransient()
    try {
      val p = odb.getFactory.createProgram(Key, ProgId)
      p.setDataObject {
        p.getDataObject.asInstanceOf[SPProgram] <| (_.setTitle(Title)) <| (_.setPIInfo(PiInfo))
      }
      odb.put(p)

      body(new VcsServer(odb, MockVcsLog), odb)
    } finally {
      odb.getDBAdmin.shutdown()
    }
  }

  def expect[A](act: VcsAction[A])(pf: PartialFunction[TryVcs[A], MatchResult[_]]): MatchResult[_] = {
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

  def exception[A](act: VcsAction[A], msg: String): MatchResult[_] =
    expect(act) {
      case -\/(VcsException(rte: RuntimeException)) => rte.getMessage must_== msg
    }

  def lookupTitle(odb: IDBDatabaseService): String =
    odb.lookupProgram(Key).getDataObject |> (_.getTitle)

  "read" should {
    "fail if the program id doesn't exist in the database" in withVcs { (vcs,_) =>
      val q2  = SPProgramID.toProgramID("GS-2015B-Q-2")
      notFound(vcs.read(q2, StaffUser) { identity }, q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { (vcs,_) =>
      val q2   = SPProgramID.toProgramID("GS-2015B-Q-2")
      val user = Set(ProgramPrincipal(q2)): Set[Principal]
      forbidden(vcs.read(ProgId, user) { identity })
    }

    "handle any exceptions that happen" in withVcs { (vcs,_) =>
      val act = vcs.read(ProgId, StaffUser) { _ => throw new RuntimeException("handle me") }
      exception(act, "handle me")
    }

    "pass the provided function the program associated with the id" in withVcs { (vcs,_) =>
      val act = vcs.read(ProgId, StaffUser) { _.getDataObject.getTitle }
      act.unsafeRun match {
        case \/-(s) => s must_== Title
        case x      => ko(x.toString)
      }
    }
  }

  "write" should {
    def dummyWrite(vcs: VcsServer, pid: SPProgramID, user: Set[Principal]): VcsAction[Boolean] =
      vcs.write[Boolean](pid, user, _ => VcsAction(true), identity, (_,_,_) => VcsAction(true))

    "fail if the program id doesn't exist in the database" in withVcs { (vcs,_) =>
      val q2  = SPProgramID.toProgramID("GS-2015B-Q-2")
      notFound(dummyWrite(vcs, q2, StaffUser), q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { (vcs,_) =>
      val q2   = SPProgramID.toProgramID("GS-2015B-Q-2")
      val user = Set(ProgramPrincipal(q2)): Set[Principal]
      forbidden(dummyWrite(vcs, ProgId, user))
    }

    "handle any exceptions that happen in evaluate" in withVcs { (vcs,_) =>
      val act = vcs.write[Boolean](ProgId, StaffUser,
        _ => throw new RuntimeException("handle me"),
        identity,
        (_,_,_) => VcsAction(true))
      exception(act, "handle me")
    }

    "handle any exceptions that happen in filter" in withVcs { (vcs,_) =>
      val act = vcs.write[Boolean](ProgId, StaffUser,
        _ => VcsAction(true),
        _ => throw new RuntimeException("handle me"),
        (_,_,_) => VcsAction(true))
      exception(act, "handle me")
    }

    "handle any exceptions that happen in update" in withVcs { (vcs,_) =>
      val act = vcs.write[Boolean](ProgId, StaffUser,
        _ => VcsAction(true),
        identity,
        (_,_,_) => throw new RuntimeException("handle me"))
      exception(act, "handle me")
    }

    val newTitle     = "The Myth of Sisyphus"
    val readTitle    = (p: ISPProgram) => VcsAction(p.getDataObject.getTitle)
    val titleMatches = (t: String)     => Title == t
    val updateTitle  = (_: ISPFactory, p: ISPProgram, _: String) => VcsAction {
      val dob = p.getDataObject <| (_.setTitle(newTitle))
      p.setDataObject(dob)
    }.as(())

    def ihc(odb: IDBDatabaseService): Long = System.identityHashCode(odb.lookupProgram(Key))

    "read from the provided program in evaluate" in withVcs { (vcs, odb) =>
      val t = vcs.write[String](ProgId, StaffUser, readTitle, titleMatches, updateTitle).unsafeRun
      (t.isRight must beTrue) and (lookupTitle(odb) must_== "The Myth of Sisyphus")
    }

    "do nothing if the filter returns false" in withVcs { (vcs,odb) =>
      val t = vcs.write[String](ProgId, StaffUser, readTitle, Function.const(false), updateTitle).unsafeRun
      (t.isRight must beTrue) and (lookupTitle(odb) must_== Title)
    }

    "not replace the program on failure" in withVcs { (vcs,odb) =>
      val hc = ihc(odb)
      vcs.write[String](ProgId, StaffUser, readTitle, Function.const(false), updateTitle).unsafeRun
      hc must_== ihc(odb)
    }

    "replace the program altogether on success" in withVcs { (vcs,odb) =>
      val hc = ihc(odb)
      vcs.write[String](ProgId, StaffUser, readTitle, titleMatches, updateTitle).unsafeRun
      hc mustNotEqual ihc(odb)
    }
  }

  "add" should {
    "fail if the user doesn't have access to the program" in withVcs { (vcs,odb) =>
      val q3   = SPProgramID.toProgramID("GS-2015B-Q-3")
      val user = Set(ProgramPrincipal(q3)): Set[Principal]

      val newKey  = new SPNodeKey()
      val q2      = SPProgramID.toProgramID("GS-2015B-Q-2")
      val newProg = odb.getFactory.createProgram(newKey, q2)

      forbidden(vcs.add(newProg, user))
    }

    "fail if a program with the same key already exists" in withVcs { (vcs,odb) =>
      val newId   = SPProgramID.toProgramID("GS-2015B-Q-2")
      val newProg = odb.getFactory.createProgram(Key, newId)
      val act     = vcs.add(newProg, StaffUser)
      expect(act) { case -\/(KeyAlreadyExists(_, _)) => ok("key exists") }
    }

    "fail if a program with the same id already exists" in withVcs { (vcs,odb) =>
      val newKey  = new SPNodeKey()
      val newProg = odb.getFactory.createProgram(newKey, ProgId)
      val act     = vcs.add(newProg, StaffUser)
      expect(act) { case -\/(IdAlreadyExists(_)) => ok("id exists") }
    }

    "fail if the new program doesn't have an id" in withVcs { (vcs,odb) =>
      val newKey  = new SPNodeKey()
      val newProg = odb.getFactory.createProgram(newKey, null)
      val act     = vcs.add(newProg, StaffUser)
      expect(act) { case -\/(MissingId) => ok("missing id") }
    }

    "add a new program to the database" in withVcs { (vcs, odb) =>
      val newKey  = new SPNodeKey()
      val newId   = SPProgramID.toProgramID("GS-2015B-Q-2")
      val newProg = odb.getFactory.createProgram(newKey, newId)
      val act     = vcs.add(newProg, StaffUser)
      act.unsafeRun

      odb.lookupProgram(newKey) must not beNull
    }
  }

  "fetchDiffs" should {
    "fetch diffs" in withVcs { (vcs, odb) =>
      val p         = odb.lookupProgram(Key)
      val vm        = p.getVersions
      val nv        = vm.getOrElse(Key, EmptyNodeVersions)
      val nv2       = nv.incr(LifespanId.random)
      val vm2       = vm.updated(Key, nv2)
      val diffState = DiffState(vm2, Set.empty)

      val svs = new vcs.SecureVcsService(StaffUser)
      svs.fetchDiffs(ProgId, diffState) match {
        case \/-(mpt) =>
          val mp = mpt.decode
          mp.update.rootLabel match {
            case Modified(k, n, dob, NodeDetail.Empty) =>
              (k must_== Key) and
                (n must_== nv) and
                (DataObjectBlob.same(dob, p.getDataObject) must beTrue)
            case _ => ko("expected a Modified root node matching the program")
          }
        case _ => ko("expected a merge plan here")
      }
    }
  }

  "storeDiffs" should {
    "do nothing if there are no diffs to store" in withVcs { (vcs, odb) =>
      val update = (Unmodified(Key): MergeNode).node()
      val mp     = MergePlan(update, Set.empty)
      val svs    = new vcs.SecureVcsService(StaffUser)
      svs.storeDiffs(ProgId, mp.encode) match {
        case \/-(false) => ok("ok, nothing done")
        case \/-(true)  => ko("updated anyway")
        case x          => ko("didn't expect: " + x)
      }
    }

    "update the program if there are differences" in withVcs { (vcs, odb) =>
      val p         = odb.lookupProgram(Key)
      val vm        = p.getVersions
      val nv        = vm.getOrElse(Key, EmptyNodeVersions)
      val nv2       = nv.incr(LifespanId.random)

      // create a merge plan with an updated title for the program node
      val dob = new SPProgram <| (_.setTitle("The Myth of Sisyphus"))
      val update = MergeNode.modified(Key, nv2, dob, NodeDetail.Empty).node()
      val mp     = MergePlan(update, Set.empty)

      val svs = new vcs.SecureVcsService(StaffUser)
      svs.storeDiffs(ProgId, mp.encode) match {
        case \/-(true)  => lookupTitle(odb) must_== "The Myth of Sisyphus"
        case \/-(false) => ko("update ignored")
        case x          => ko("didn't expect: " + x)
      }
    }
  }
}

object VcsServerSpec {

  object MockVcsLog extends VcsLog {
    override def log(op: VcsOp, pid: SPProgramID, principals: Set[GeminiPrincipal]): VcsEvent =
      VcsEvent(0, op, 0, pid, principals)

    override def archive(f: File): Unit = ()

    override def selectByProgram(pid: SPProgramID, offset: Int, size: Int): (List[VcsEventSet], Boolean) =
      (Nil, false)
  }
}
