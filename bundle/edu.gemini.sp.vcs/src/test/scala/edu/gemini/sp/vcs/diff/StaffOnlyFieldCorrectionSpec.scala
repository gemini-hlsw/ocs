package edu.gemini.sp.vcs.diff

import java.security.Principal

import edu.gemini.spModel.obs.ObsQaState
import edu.gemini.util.security.principal.{StaffPrincipal, VisitorPrincipal}
import org.specs2.matcher.MatchResult

import scalaz._

class StaffOnlyFieldCorrectionSpec extends VcsSpecification {

  import TestEnv._

  def afterSync(env: TestEnv, p: Principal)(mr: => MatchResult[Any]): MatchResult[_] = {
    expect(env.local.vcs(p).sync(Q1, DummyPeer)) {
      case \/-(_) => mr
    }
  }

  "staff only field correction" should {
    "allow super-staff to change rollover status" in withVcs { env =>
      env.local.rollover = true

      (env.remote.rollover should beFalse) and afterSync(env, StaffPrincipal.Gemini) {
        env.remote.rollover should beTrue
      }
    }

    "allow visitor to change rollover status" in withVcs { env =>
      env.local.rollover = true

      (env.remote.rollover should beFalse) and afterSync(env, new VisitorPrincipal(Q1)) {
        env.remote.rollover should beTrue
      }
    }

    "allow normal staff to change rollover status" in withVcs { env =>
      env.local.rollover = true

      (env.remote.rollover should beFalse) and afterSync(env, StaffUserPrincipal) {
        env.remote.rollover should beTrue
      }
    }

    "not allow non-staff to change rollover status" in withVcs { env =>
      env.local.rollover = true

      afterSync(env, PiUserPrincipal) {
        (env.local.rollover should beFalse) and (env.remote.rollover should beFalse)
      }
    }

    "not allow non-staff to sneak a change to staff contact" in withVcs { env =>
      env.local.rollover = true
      env.local.contact  = PiEmail

      afterSync(env, PiUserPrincipal) {
        (env.local.rollover should beFalse) and
          (env.remote.rollover should beFalse) and
          (env.local.contact must_== StaffEmail) and
          (env.remote.contact must_== StaffEmail)
      }
    }

    "keep non-staff changes to non-protected fields" in withVcs { env =>
      env.local.rollover  = true
      env.local.progTitle = "The Plague"

      afterSync(env, PiUserPrincipal) {
        (env.remote.progTitle must_== "The Plague") and
          (env.remote.rollover should beFalse) and
          (env.local.rollover should beFalse)
      }
    }

    "allow staff to change QA state" in withVcs { env =>
      env.local.setQaState(ObsKey, ObsQaState.PASS)

      afterSync(env, StaffUserPrincipal) {
        env.remote.getQaState(ObsKey) must_== ObsQaState.PASS
      }
    }

    "not allow non-staff to change QA state" in withVcs { env =>
      env.local.setQaState(ObsKey, ObsQaState.PASS)

      afterSync(env, PiUserPrincipal) {
        (env.local.getQaState(ObsKey) must_== ObsQaState.UNDEFINED) and
        (env.remote.getQaState(ObsKey) must_== ObsQaState.UNDEFINED)
      }
    }

    "allow staff to create new observations with non-default QA state" in withVcs { env =>
      val obsKey = env.local.addObservation()
      env.local.setQaState(obsKey, ObsQaState.PASS)

      afterSync(env, StaffUserPrincipal) {
        env.remote.getQaState(obsKey) must_== ObsQaState.PASS
      }
    }

    "not allow non-staff to create new observations with non-default QA state" in withVcs { env =>
      val obsKey = env.local.addObservation()
      env.local.setQaState(obsKey, ObsQaState.PASS)

      afterSync(env, PiUserPrincipal) {
        (env.local.getQaState(obsKey) must_== ObsQaState.UNDEFINED) and
        (env.remote.getQaState(obsKey) must_== ObsQaState.UNDEFINED)
      }
    }
  }


}
