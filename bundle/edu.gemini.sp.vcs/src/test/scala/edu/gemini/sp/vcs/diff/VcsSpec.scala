package edu.gemini.sp.vcs.diff


import edu.gemini.sp.vcs.diff.VcsAction._
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

    "fail if the indicated program has different keys locally vs remotely" in withVcs { env =>
      env.local.addNewProgram(Q2)
      env.remote.addNewProgram(Q2)
      idClash(env.local.staffVcs.pull(Q2, DummyPeer), Q2)
    }
  }
}
