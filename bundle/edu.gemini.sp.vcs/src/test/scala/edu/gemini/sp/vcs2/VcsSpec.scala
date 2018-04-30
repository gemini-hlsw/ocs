package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.pot.sp.version.VersionMap
import edu.gemini.sp.vcs2.ProgramLocationSet.{Both, LocalOnly, Neither, RemoteOnly}
import edu.gemini.sp.vcs2.VcsAction._
import edu.gemini.sp.vcs2.VcsFailure.NeedsUpdate
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.obscomp.SPNote
import edu.gemini.util.security.principal.ProgramPrincipal
import java.util.concurrent.atomic.AtomicBoolean

import org.specs2.specification.core.Fragments

import scalaz._

class VcsSpec extends VcsSpecification {
  import TestEnv._

  val cancelled    = new AtomicBoolean(true)
  val notCancelled = new AtomicBoolean(false)

  "checkout" should {
    "fail if the indicated program doesn't exist remotely" in withVcs { env =>
      notFound(env.local.superStaffVcs.checkout(Q2, DummyPeer, notCancelled), Q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { env =>
      env.remote.addNewProgram(Q2)
      forbidden(env.local.vcs(ProgramPrincipal(Q1)).checkout(Q2, DummyPeer, notCancelled))
    }

    "transfer a program from the remote database to the local database" in withVcs { env =>
      // make the new program and store it remotely
      val remoteQ2 = env.remote.addNewProgram(Q2)

      // run checkout on the local peer
      env.local.superStaffVcs.checkout(Q2, DummyPeer, notCancelled).unsafeRun

      val localQ2 = env.local.odb.lookupProgramByID(Q2)
      localQ2.getLifespanId must be_!=(remoteQ2.getLifespanId)
    }

    "do nothing if cancelled" in withVcs { env =>
      // make the new program and store it remotely
      env.remote.addNewProgram(Q2)

      // run checkout on the local peer
      env.local.superStaffVcs.checkout(Q2, DummyPeer, cancelled).unsafeRun
      env.local.odb.lookupProgramByID(Q2) must beNull
    }
  }

  "revert" should {
    "fail if the indicated program doesn't exist remotely" in withVcs { env =>
      env.local.addNewProgram(Q2)
      notFound(env.local.superStaffVcs.revert(Q2, DummyPeer, notCancelled), Q2)
    }

    "fail if the indicated program doesn't exist locally" in withVcs { env =>
      env.remote.addNewProgram(Q2)
      notFound(env.local.superStaffVcs.revert(Q2, DummyPeer, notCancelled), Q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { env =>
      val key = new SPNodeKey()
      env.local.addNewProgramWithKey(key, Q2)
      env.remote.addNewProgramWithKey(key, Q2)
      forbidden(env.local.vcs(ProgramPrincipal(Q1)).revert(Q2, DummyPeer, notCancelled))
    }

    "revert changes made since the last sync" in withVcs { env =>
      env.local.progTitle = "to be reverted"
      env.local.superStaffVcs.revert(Q1, DummyPeer, notCancelled).unsafeRun
      env.local.progTitle shouldEqual Title
    }

    "do nothing if cancelled" in withVcs { env =>
      env.local.progTitle = "to be reverted"
      env.local.superStaffVcs.revert(Q1, DummyPeer, cancelled).unsafeRun
      env.local.progTitle shouldEqual "to be reverted"
    }
  }

  "add" should {
    "fail if the indicated program doesn't exist locally" in withVcs { env =>
      notFound(env.local.superStaffVcs.add(Q2, DummyPeer), Q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { env =>
      env.local.addNewProgram(Q2)
      forbidden(env.local.vcs(ProgramPrincipal(Q1)).add(Q2, DummyPeer))
    }

    "transfer a program from the local database to the remote database" in withVcs { env =>
      // make the new program and store it locally
      val localQ2 = env.local.addNewProgram(Q2)

      // run add to send it to the remote peer
      env.local.superStaffVcs.add(Q2, DummyPeer).unsafeRun

      val remoteQ2 = env.remote.odb.lookupProgramByID(Q2)

      localQ2.getLifespanId must be_!=(remoteQ2.getLifespanId)
    }
  }

  "pull" should {
    "fail if the indicated program doesn't exist locally" in withVcs { env =>
      env.remote.addNewProgram(Q2)
      notFound(env.local.superStaffVcs.pull(Q2, DummyPeer, notCancelled), Q2)
    }

    "fail if the indicated program doesn't exist remotely" in withVcs { env =>
      env.local.addNewProgram(Q2)
      notFound(env.local.superStaffVcs.pull(Q2, DummyPeer, notCancelled), Q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { env =>
      forbidden(env.local.vcs(ProgramPrincipal(Q2)).pull(Q1, DummyPeer, notCancelled))
    }

    "fail if the indicated program has different keys locally vs remotely" in withVcs { env =>
      env.local.addNewProgram(Q2)
      env.remote.addNewProgram(Q2)
      idClash(env.local.superStaffVcs.pull(Q2, DummyPeer, notCancelled), Q2)
    }

    "do nothing if the local version is the same" in withVcs { env =>
      expect(env.local.superStaffVcs.pull(Q1, DummyPeer, notCancelled)) {
        case \/-((Neither,_)) => ok("")
      }
    }

    "do nothing if the local version is newer" in withVcs { env =>
      env.local.progTitle = "The Myth of Sisyphus"

      expect(env.local.superStaffVcs.pull(Q1, DummyPeer, notCancelled)) {
        case \/-((Neither,_)) => ok("")
      } and (env.local.progTitle must_== "The Myth of Sisyphus")
    }

    "merge the updates if the remote version is newer" in withVcs { env =>
      env.remote.progTitle = "The Myth of Sisyphus"

      expect(env.local.superStaffVcs.pull(Q1, DummyPeer, notCancelled)) {
        case \/-((LocalOnly,_)) => ok("")
      } and (env.local.progTitle must_== "The Myth of Sisyphus")
    }

    "do nothing if cancelled" in withVcs { env =>
      env.remote.progTitle = "The Myth of Sisyphus"

      // run checkout on the local peer
      env.local.superStaffVcs.pull(Q1, DummyPeer, cancelled).unsafeRun
      env.local.progTitle must_== "The Stranger"
    }
  }

  "push" should {
    "fail if the indicated program doesn't exist locally" in withVcs { env =>
      env.remote.addNewProgram(Q2)
      notFound(env.local.superStaffVcs.push(Q2, DummyPeer, notCancelled), Q2)
    }

    "fail if the indicated program doesn't exist remotely" in withVcs { env =>
      env.local.addNewProgram(Q2)
      notFound(env.local.superStaffVcs.push(Q2, DummyPeer, notCancelled), Q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { env =>
      forbidden(env.local.vcs(ProgramPrincipal(Q2)).push(Q1, DummyPeer, notCancelled))
    }

    "fail if the indicated program has different keys locally vs remotely" in withVcs { env =>
      env.local.addNewProgram(Q2)
      env.remote.addNewProgram(Q2)
      idClash(env.local.superStaffVcs.push(Q2, DummyPeer, notCancelled), Q2)
    }

    "do nothing if the local version is the same" in withVcs { env =>
      expect(env.local.superStaffVcs.push(Q1, DummyPeer, notCancelled)) {
        case \/-((Neither,_)) => ok("")
      }
    }

    "fail with NeedsUpdate if the local version is older" in withVcs { env =>
      env.remote.progTitle = "The Myth of Sisyphus"

      expect(env.local.superStaffVcs.push(Q1, DummyPeer, notCancelled)) {
        case -\/(NeedsUpdate) => ok("")
      } and (env.local.progTitle must_== Title)
    }

    "merge the updates if the local version is newer" in withVcs { env =>
      env.local.progTitle = "The Myth of Sisyphus"

      expect(env.local.superStaffVcs.push(Q1, DummyPeer, notCancelled)) {
        case \/-((RemoteOnly,_)) => ok("")
      } and (env.remote.progTitle must_== "The Myth of Sisyphus")
    }

    "do nothing if cancelled" in withVcs { env =>
      env.local.progTitle = "The Myth of Sisyphus"

      // run checkout on the local peer
      env.local.superStaffVcs.push(Q1, DummyPeer, cancelled).unsafeRun
      env.remote.progTitle must_== "The Stranger"
    }

    // TODO: pending tests with conflicts, which must be rejected
  }

  def syncFragments(name: String, syncMethod: (Vcs, SPProgramID) => VcsAction[(ProgramLocationSet,VersionMap)]): Fragments = {
    name should {
      "fail if the indicated program doesn't exist locally" in withVcs { env =>
        env.remote.addNewProgram(Q2)
        notFound(syncMethod(env.local.superStaffVcs, Q2), Q2)
      }

      "fail if the indicated program doesn't exist remotely" in withVcs { env =>
        env.local.addNewProgram(Q2)
        notFound(syncMethod(env.local.superStaffVcs, Q2), Q2)
      }

      "fail if the user doesn't have access to the program" in withVcs { env =>
        forbidden(syncMethod(env.local.vcs(ProgramPrincipal(Q2)), Q1))
      }

      "fail if the indicated program has different keys locally vs remotely" in withVcs { env =>
        env.local.addNewProgram(Q2)
        env.remote.addNewProgram(Q2)
        idClash(syncMethod(env.local.superStaffVcs, Q2), Q2)
      }

      "do nothing if both versions are the same" in withVcs { env =>
        expect(syncMethod(env.local.superStaffVcs, Q1)) {
          case \/-((Neither,_)) => ok("")
        }
      }

      "merge the remote updates if the remote version is newer" in withVcs { env =>
        env.remote.progTitle = "The Myth of Sisyphus"

        expect(syncMethod(env.local.superStaffVcs, Q1)) {
          case \/-((LocalOnly,_)) => ok("")
        } and (env.local.progTitle must_== "The Myth of Sisyphus")
      }

      "send the local updates if the local version is newer" in withVcs { env =>
        env.local.progTitle = "The Myth of Sisyphus"

        expect(syncMethod(env.local.superStaffVcs, Q1)) {
          case \/-((RemoteOnly,_)) => ok("")
        } and (env.remote.progTitle must_== "The Myth of Sisyphus")
      }

      "merge local and remote updates if both have been modified" in withVcs { env =>
        val group = env.local.odb.getFactory.createGroup(env.local.prog, null)
        env.local.prog.addGroup(group)

        val note = env.remote.odb.getFactory.createObsComponent(env.remote.prog, SPNote.SP_TYPE, null)
        env.remote.prog.addObsComponent(note)

        expect(syncMethod(env.local.superStaffVcs, Q1)) {
          case \/-((Both,_)) => ok("")
        } and (env.remote.prog.getGroups.get(0).getNodeKey must_== group.getNodeKey) and
          (env.local.prog.getObsComponents.get(0).getNodeKey must_== note.getNodeKey)
      }

    }
  }

  syncFragments("sync", (vcs, pid) => vcs.sync(pid, DummyPeer, notCancelled))
  syncFragments("retrySync", (vcs, pid) => vcs.retrySync(pid, DummyPeer, notCancelled, 10))

  "cancelled sync" should {
    "do nothing" in withVcs { env =>
      val group = env.local.odb.getFactory.createGroup(env.local.prog, null)
      env.local.prog.addGroup(group)

      val note = env.remote.odb.getFactory.createObsComponent(env.remote.prog, SPNote.SP_TYPE, null)
      env.remote.prog.addObsComponent(note)

      env.local.superStaffVcs.sync(Q1, DummyPeer, cancelled).unsafeRun
      (env.remote.prog.getGroups.size must_== 0) and
        (env.local.prog.getObsComponents.size must_== 0)
    }
  }

  // TODO: pending tests with conflicts, which must be rejected
  // TODO: not really testing the "retry" part of "retrySync"

}
