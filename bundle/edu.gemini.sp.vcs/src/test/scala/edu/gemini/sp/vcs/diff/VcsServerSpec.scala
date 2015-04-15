package edu.gemini.sp.vcs.diff


import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.sp.vcs.diff.VcsAction._
import edu.gemini.sp.vcs.diff.VcsFailure._
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.util.security.principal.{ProgramPrincipal, StaffPrincipal}

import java.security.Principal

import scala.language.postfixOps
import scalaz._
import Scalaz._

class VcsServerSpec extends VcsSpecification {

  import TestEnv._

  val StaffUser = Set(StaffPrincipal.Gemini): Set[Principal]

  "read" should {
    "fail if the program id doesn't exist in the database" in withVcs { env =>
      notFound(env.local.server.read(Q2, StaffUser) { identity }, Q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { env =>
      forbidden(env.local.server.read(Q1, Set(ProgramPrincipal(Q2))) { identity })
    }

    "handle any exceptions that happen" in withVcs { env =>
      val act = env.local.server.read(Q1, StaffUser) { _ => throw new RuntimeException("handle me") }
      exception(act, "handle me")
    }

    "pass the provided function the program associated with the id" in withVcs { env =>
      val act = env.local.server.read(Q1, StaffUser) { _.getDataObject.getTitle }
      act.unsafeRun match {
        case \/-(s) => s must_== Title
        case x      => ko(x.toString)
      }
    }
  }

  "write" should {
    def dummyWrite(env: TestEnv, pid: SPProgramID, user: Set[Principal]): VcsAction[Boolean] =
      env.local.server.write[Boolean](pid, user, _ => VcsAction(true), identity, (_,_,_) => VcsAction(true))

    "fail if the program id doesn't exist in the database" in withVcs { env =>
      notFound(dummyWrite(env, Q2, StaffUser), Q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { env =>
      forbidden(dummyWrite(env, Q1, Set(ProgramPrincipal(Q2))))
    }

    "handle any exceptions that happen in evaluate" in withVcs { env =>
      val act = env.local.server.write[Boolean](Q1, StaffUser,
        _ => throw new RuntimeException("handle me"),
        identity,
        (_,_,_) => VcsAction(true))
      exception(act, "handle me")
    }

    "handle any exceptions that happen in filter" in withVcs { env =>
      val act = env.local.server.write[Boolean](Q1, StaffUser,
        _ => VcsAction(true),
        _ => throw new RuntimeException("handle me"),
        (_,_,_) => VcsAction(true))
      exception(act, "handle me")
    }

    "handle any exceptions that happen in update" in withVcs { env =>
      val act = env.local.server.write[Boolean](Q1, StaffUser,
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

    "read from the provided program in evaluate" in withVcs { env =>
      val t = env.local.server.write[String](Q1, StaffUser, readTitle, titleMatches, updateTitle).unsafeRun
      (t.isRight must beTrue) and (env.local.progTitle must_== "The Myth of Sisyphus")
    }

    "do nothing if the filter returns false" in withVcs { env =>
      val t = env.local.server.write[String](Q1, StaffUser, readTitle, Function.const(false), updateTitle).unsafeRun
      (t.isRight must beTrue) and (env.local.progTitle must_== Title)
    }

    "not replace the program on failure" in withVcs { env =>
      val hc = ihc(env.local.odb)
      env.local.server.write[String](Q1, StaffUser, readTitle, Function.const(false), updateTitle).unsafeRun
      hc must_== ihc(env.local.odb)
    }

    "replace the program altogether on success" in withVcs { env =>
      val hc = ihc(env.local.odb)
      env.local.server.write[String](Q1, StaffUser, readTitle, titleMatches, updateTitle).unsafeRun
      hc mustNotEqual ihc(env.local.odb)
    }
  }

  "add" should {
    "fail if the user doesn't have access to the program" in withVcs { env =>
      val user    = Set(ProgramPrincipal(Q3): Principal)
      val newProg = env.local.newProgram(Q2)
      forbidden(env.local.server.add(newProg, user))
    }

    "fail if a program with the same key already exists" in withVcs { env =>
      val newProg = env.local.odb.getFactory.createProgram(Key, Q2)
      val act     = env.local.server.add(newProg, StaffUser)
      expect(act) { case -\/(KeyAlreadyExists(_, _)) => ok("key exists") }
    }

    "fail if a program with the same id already exists" in withVcs { env =>
      val newKey  = new SPNodeKey()
      val newProg = env.local.odb.getFactory.createProgram(newKey, Q1)
      val act     = env.local.server.add(newProg, StaffUser)
      expect(act) { case -\/(IdAlreadyExists(_)) => ok("id exists") }
    }

    "fail if the new program doesn't have an id" in withVcs { env =>
      val newKey  = new SPNodeKey()
      val newProg = env.local.odb.getFactory.createProgram(newKey, null)
      val act     = env.local.server.add(newProg, StaffUser)
      expect(act) { case -\/(MissingId) => ok("missing id") }
    }

    "add a new program to the database" in withVcs { env =>
      val newProg = env.local.newProgram(Q2)
      val act     = env.local.server.add(newProg, StaffUser)
      act.unsafeRun

      env.local.odb.lookupProgram(newProg.getProgramKey) must not beNull
    }
  }

  "fetchDiffs" should {
    "fetch diffs" in withVcs { env =>
      val vm        = env.local.prog.getVersions
      val nv        = vm.getOrElse(Key, EmptyNodeVersions)
      val nv2       = nv.incr(LifespanId.random)
      val vm2       = vm.updated(Key, nv2)
      val diffState = DiffState(Key, vm2, Set.empty)

      val svs = new env.local.server.SecureVcsService(StaffUser)
      svs.fetchDiffs(Q1, diffState) match {
        case \/-(pdt) =>
          val mp = pdt.decode.plan
          mp.update.rootLabel match {
            case Modified(k, n, dob, NodeDetail.Empty, Conflicts.EMPTY) =>
              (k must_== Key) and
                (n must_== nv) and
                (DataObjectBlob.same(dob, env.local.prog.getDataObject) must beTrue)
            case _ => ko("expected a Modified root node matching the program")
          }
        case _ => ko("expected a merge plan here")
      }
    }
  }

  "storeDiffs" should {
    "do nothing if there are no diffs to store" in withVcs { env =>
      val update = (Unmodified(Key): MergeNode).node()
      val mp     = MergePlan(update, Set.empty)
      val svs    = new env.local.server.SecureVcsService(StaffUser)
      svs.storeDiffs(Q1, mp.encode) match {
        case \/-(false) => ok("ok, nothing done")
        case \/-(true)  => ko("updated anyway")
        case x          => ko("didn't expect: " + x)
      }
    }

    "update the program if there are differences" in withVcs { env =>
      val vm        = env.local.prog.getVersions
      val nv        = vm.getOrElse(Key, EmptyNodeVersions)
      val nv2       = nv.incr(LifespanId.random)

      // create a merge plan with an updated title for the program node
      val dob    = new SPProgram <| (_.setTitle("The Myth of Sisyphus"))
      val update = MergeNode.modified(Key, nv2, dob, NodeDetail.Empty, Conflicts.EMPTY).node()
      val mp     = MergePlan(update, Set.empty)

      val svs = new env.local.server.SecureVcsService(StaffUser)
      svs.storeDiffs(Q1, mp.encode) match {
        case \/-(true)  => env.local.progTitle must_== "The Myth of Sisyphus"
        case \/-(false) => ko("update ignored")
        case x          => ko("didn't expect: " + x)
      }
    }
  }
}
