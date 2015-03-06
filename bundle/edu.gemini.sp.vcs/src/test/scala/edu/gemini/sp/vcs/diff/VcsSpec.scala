package edu.gemini.sp.vcs.diff


import edu.gemini.sp.vcs.diff.VcsAction._
import edu.gemini.sp.vcs.diff.VcsFailure.NeedsUpdate
import edu.gemini.spModel.obscomp.SPNote
import edu.gemini.util.security.principal.ProgramPrincipal


import scalaz._
import Scalaz._

class VcsSpec extends VcsSpecification {
  import TestEnv._

  "checkout" should {
    "fail if the indicated program doesn't exist remotely" in withVcs { env =>
      notFound(env.local.staffVcs.checkout(Q2, DummyPeer), Q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { env =>
      env.remote.addNewProgram(Q2)
      forbidden(env.local.vcs(ProgramPrincipal(Q1)).checkout(Q2, DummyPeer))
    }

    "transfer a program from the remote database to the local database" in withVcs { env =>
      // make the new program and store it remotely
      env.remote.addNewProgram(Q2)

      // run checkout on the local peer
      env.local.staffVcs.checkout(Q2, DummyPeer).unsafeRun

      env.local.odb.lookupProgramByID(Q2) must not beNull
    }
  }

  "add" should {
    "fail if the indicated program doesn't exist locally" in withVcs { env =>
      notFound(env.local.staffVcs.add(Q2, DummyPeer), Q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { env =>
      env.local.addNewProgram(Q2)
      forbidden(env.local.vcs(ProgramPrincipal(Q1)).add(Q2, DummyPeer))
    }

    "transfer a program from the local database to the remote database" in withVcs { env =>
      // make the new program and store it locally
      env.local.addNewProgram(Q2)

      // run add to send it to the remote peer
      env.local.staffVcs.add(Q2, DummyPeer).unsafeRun

      env.remote.odb.lookupProgramByID(Q2) must not beNull
    }
  }

  "pull" should {
    "fail if the indicated program doesn't exist locally" in withVcs { env =>
      env.remote.addNewProgram(Q2)
      notFound(env.local.staffVcs.pull(Q2, DummyPeer), Q2)
    }

    "fail if the indicated program doesn't exist remotely" in withVcs { env =>
      env.local.addNewProgram(Q2)
      notFound(env.local.staffVcs.pull(Q2, DummyPeer), Q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { env =>
      forbidden(env.local.vcs(ProgramPrincipal(Q2)).pull(Q1, DummyPeer))
    }

    "fail if the indicated program has different keys locally vs remotely" in withVcs { env =>
      env.local.addNewProgram(Q2)
      env.remote.addNewProgram(Q2)
      idClash(env.local.staffVcs.pull(Q2, DummyPeer), Q2)
    }

    "do nothing if the local version is the same" in withVcs { env =>
      expect(env.local.staffVcs.pull(Q1, DummyPeer)) {
        case \/-(false) => ok("")
      }
    }

    "do nothing if the local version is newer" in withVcs { env =>
      env.local.progTitle = "The Myth of Sisyphus"

      expect(env.local.staffVcs.pull(Q1, DummyPeer)) {
        case \/-(false) => ok("")
      } and (env.local.progTitle must_== "The Myth of Sisyphus")
    }

    "merge the updates if the remote version is newer" in withVcs { env =>
      env.remote.progTitle = "The Myth of Sisyphus"

      expect(env.local.staffVcs.pull(Q1, DummyPeer)) {
        case \/-(true) => ok("")
      } and (env.local.progTitle must_== "The Myth of Sisyphus")
    }
  }

  "push" should {
    "fail if the indicated program doesn't exist locally" in withVcs { env =>
      env.remote.addNewProgram(Q2)
      notFound(env.local.staffVcs.push(Q2, DummyPeer), Q2)
    }

    "fail if the indicated program doesn't exist remotely" in withVcs { env =>
      env.local.addNewProgram(Q2)
      notFound(env.local.staffVcs.push(Q2, DummyPeer), Q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { env =>
      forbidden(env.local.vcs(ProgramPrincipal(Q2)).push(Q1, DummyPeer))
    }

    "fail if the indicated program has different keys locally vs remotely" in withVcs { env =>
      env.local.addNewProgram(Q2)
      env.remote.addNewProgram(Q2)
      idClash(env.local.staffVcs.push(Q2, DummyPeer), Q2)
    }

    "do nothing if the local version is the same" in withVcs { env =>
      expect(env.local.staffVcs.push(Q1, DummyPeer)) {
        case \/-(false) => ok("")
      }
    }

    "fail with NeedsUpdate if the local version is older" in withVcs { env =>
      env.remote.progTitle = "The Myth of Sisyphus"

      expect(env.local.staffVcs.push(Q1, DummyPeer)) {
        case -\/(NeedsUpdate) => ok("")
      } and (env.local.progTitle must_== Title)
    }

    "merge the updates if the local version is newer" in withVcs { env =>
      env.local.progTitle = "The Myth of Sisyphus"

      expect(env.local.staffVcs.push(Q1, DummyPeer)) {
        case \/-(true) => ok("")
      } and (env.remote.progTitle must_== "The Myth of Sisyphus")
    }
  }

  "sync" should {
    "fail if the indicated program doesn't exist locally" in withVcs { env =>
      env.remote.addNewProgram(Q2)
      notFound(env.local.staffVcs.sync(Q2, DummyPeer), Q2)
    }

    "fail if the indicated program doesn't exist remotely" in withVcs { env =>
      env.local.addNewProgram(Q2)
      notFound(env.local.staffVcs.sync(Q2, DummyPeer), Q2)
    }

    "fail if the user doesn't have access to the program" in withVcs { env =>
      forbidden(env.local.vcs(ProgramPrincipal(Q2)).sync(Q1, DummyPeer))
    }

    "fail if the indicated program has different keys locally vs remotely" in withVcs { env =>
      env.local.addNewProgram(Q2)
      env.remote.addNewProgram(Q2)
      idClash(env.local.staffVcs.sync(Q2, DummyPeer), Q2)
    }

    "do nothing if both versions are the same" in withVcs { env =>
      expect(env.local.staffVcs.sync(Q1, DummyPeer)) {
        case \/-(ProgramLocation.Neither) => ok("")
      }
    }

    "merge the remote updates if the remote version is newer" in withVcs { env =>
      env.remote.progTitle = "The Myth of Sisyphus"

      expect(env.local.staffVcs.sync(Q1, DummyPeer)) {
        case \/-(ProgramLocation.LocalOnly) => ok("")
      } and (env.local.progTitle must_== "The Myth of Sisyphus")
    }

    "send the local updates if the local version is newer" in withVcs { env =>
      env.local.progTitle = "The Myth of Sisyphus"

      expect(env.local.staffVcs.sync(Q1, DummyPeer)) {
        case \/-(ProgramLocation.RemoteOnly) => ok("")
      } and (env.remote.progTitle must_== "The Myth of Sisyphus")
    }

    "merge local and remote updates if both have been modified" in withVcs { env =>
      env.local.progTitle = "The Myth of Sisyphus"

      val note = env.remote.odb.getFactory.createObsComponent(env.remote.prog, SPNote.SP_TYPE, null)
      env.remote.prog.addObsComponent(note)

      expect(env.local.staffVcs.sync(Q1, DummyPeer)) {
        case \/-(ProgramLocation.Both) => ok("")
      } and (env.remote.progTitle must_== "The Myth of Sisyphus") and
        (env.local.prog.getObsComponents.get(0).getNodeKey must_== note.getNodeKey)
    }
  }
}
