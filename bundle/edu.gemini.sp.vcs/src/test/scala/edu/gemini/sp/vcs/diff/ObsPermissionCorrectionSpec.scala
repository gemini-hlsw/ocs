package edu.gemini.sp.vcs.diff


import edu.gemini.pot.sp.{ISPObservation, SPNodeKey}
import edu.gemini.pot.sp.Conflict._
import edu.gemini.pot.sp.version._
import edu.gemini.spModel.obs.ObsPhase2Status
import edu.gemini.spModel.obs.ObsPhase2Status._
import edu.gemini.spModel.obscomp.SPNote
import edu.gemini.spModel.rich.pot.sp._
import org.specs2.matcher.MatchResult



class ObsPermissionCorrectionSpec extends VcsSpecification {

  import TestEnv._

  private def bothAre(stat: ObsPhase2Status, k: SPNodeKey, env: TestEnv): MatchResult[Any] =
    (env.local.getObsPhase2Status(k) must_== stat) and (env.remote.getObsPhase2Status(k) must_== stat)

  private def localIs(stat: ObsPhase2Status, k: SPNodeKey, env: TestEnv): MatchResult[Any] =
    env.local.getObsPhase2Status(k) must_== stat

  "observation permission correction" should {
    "reset observations created with an advanced status" in withVcs { env =>
      val obsKey = env.local.addObservation()
      env.local.setObsPhase2Status(obsKey, PHASE_2_COMPLETE)
      val initialVersion = env.local.nodeVersions(obsKey)

      afterPull(env, PiUserPrincipal) {
        localIs(PI_TO_COMPLETE, obsKey, env) and
          (env.local.nodeVersions(obsKey) must_== initialVersion.incr(env.local.lifespanId)) and
          localHasNote(new CreatePermissionFail(obsKey), env)
      }
    }

    "allow a staff person to create an observation with an advanced status" in withVcs { env =>
      val obsKey = env.local.addObservation()
      env.local.setObsPhase2Status(obsKey, PHASE_2_COMPLETE)
      val initialVersion = env.local.nodeVersions(obsKey)

      afterSync(env, StaffUserPrincipal) {
        bothAre(PHASE_2_COMPLETE, obsKey, env) and
          (env.local.nodeVersions(obsKey) must_== initialVersion)
      }
    }

    "replace an inappropriately deleted observation" in withVcs { env =>
      env.remote.setObsPhase2Status(ObsKey, PHASE_2_COMPLETE)
      unsafeSync(env)

      // delete the ready observation
      env.local.delete(ObsKey)
      val initialVersion = env.local.prog.getVersion

      // pull and see it has been resurrected
      afterPull(env, PiUserPrincipal) {
        (env.local.prog.children.exists(_.key == ObsKey) must beTrue) and
          (env.local.prog.getVersion must_== initialVersion.incr(env.local.lifespanId)) and
          localHasNote(new DeletePermissionFail(ObsKey), env)
      }
    }

    "allow a staff person to delete an observation with advanced status" in withVcs { env =>
      env.remote.setObsPhase2Status(ObsKey, PHASE_2_COMPLETE)
      unsafeSync(env)

      // delete the ready observation
      env.local.delete(ObsKey)
      val initialVersion = env.local.prog.getVersion

      // sync and see it is still gone
      afterSync(env, StaffUserPrincipal) {
        (env.local.prog.children.exists(_.key == ObsKey) must beFalse) and
          (env.local.prog.getVersion must_== initialVersion)
      }
    }

    "replace an inappropriately deleted observation in the program if its group is also deleted" in withVcs { env =>
      val grpKey = env.remote.addGroup()
      val obsKey = env.remote.addObservation(grpKey)
      env.remote.setObsPhase2Status(obsKey, PHASE_2_COMPLETE)
      unsafeSync(env)

      // delete the group along with its observation
      env.local.delete(grpKey)

      // sync and see it has been resurrected
      afterPull(env, PiUserPrincipal) {
        (env.local.prog.children.exists(_.key == obsKey) must beTrue) and
          localHasNote(new DeletePermissionFail(obsKey), env)
      }
    }

    "copy moved nodes when replacing deleted observations" in withVcs { env =>
      env.remote.setObsPhase2Status(ObsKey, PHASE_2_COMPLETE)
      val noteKey = env.remote.addNote("abc", ObsKey)
      unsafeSync(env)

      // edit the note title
      env.local.updateDataObject[SPNote](noteKey)(_.setTitle("123"))

      // move the note to the program node
      env.local.move(noteKey, Key)

      // delete the observation
      env.local.delete(ObsKey)

      // sync and see it has been resurrected and the note came back.  the
      // moved note is still there under the program but now has a new key
      afterPull(env, PiUserPrincipal) {
        val restoredObs  = env.local.obs(ObsKey)
        val restoredNote = restoredObs.findObsComponentByType(SPNote.SP_TYPE).get
        val dupNote      = env.local.prog.getObsComponents.get(0)

        (restoredNote.key must_== noteKey) and
          (restoredNote.getDataObject.getTitle must_== "abc") and
          (dupNote.key must_!= noteKey) and
          (dupNote.getDataObject.getTitle must_== "123") and
          localHasNote(new DeletePermissionFail(ObsKey), env)
      }
    }

    "rollback any version changes to an inappropriately deleted observation when replacing" in withVcs { env =>
      env.remote.setObsPhase2Status(ObsKey, PHASE_2_COMPLETE)

      val noteKey     = env.remote.addNote("abc", ObsKey)
      val noteVersion = env.remote.nodeVersions(noteKey)
      unsafeSync(env)

      // edit the note
      env.local.updateDataObject[SPNote](noteKey)(_.setTitle("123"))

      // delete the observation with its edited note
      env.local.delete(ObsKey)

      // sync and see it has been resurrected and the note came back but with
      // the same version information, but of course the old title
      afterPull(env, PiUserPrincipal) {
        val restoredObs  = env.local.obs(ObsKey)
        val restoredNote = restoredObs.findObsComponentByType(SPNote.SP_TYPE).get

        (restoredNote.key must_== noteKey) and
          (restoredNote.getDataObject.getTitle must_== "abc") and
          (restoredNote.getVersion must_== noteVersion)
      }
    }

    "reset remotely deleted observations with an inappropriately advanced status" in withVcs { env =>
      env.remote.delete(ObsKey)
      env.local.setObsPhase2Status(ObsKey, PHASE_2_COMPLETE)
      afterPull(env, PiUserPrincipal) {
        localIs(PI_TO_COMPLETE, ObsKey, env) and
        localHasNote(new UpdatePermissionFail(ObsKey), env)
      }
    }

    "allow a staff person to advance the status of a remotely deleted observation" in withVcs { env =>
      env.remote.delete(ObsKey)
      env.local.setObsPhase2Status(ObsKey, PHASE_2_COMPLETE)
      afterPull(env, StaffUserPrincipal) { localIs(PHASE_2_COMPLETE, ObsKey, env) }
    }

    "move inappropriately edited observations to a conflict folder" in withVcs { env =>
      // edit locally
      val noteKey = env.local.addNote("abc", ObsKey)

      // change status remotely
      env.remote.setObsPhase2Status(ObsKey, PHASE_2_COMPLETE)

      afterPull(env, PiUserPrincipal) {
        val conflictFolder = env.local.prog.getConflictFolder
        val localObs       = conflictFolder.children.head.asInstanceOf[ISPObservation]
        val localNote      = localObs.findObsComponentByType(SPNote.SP_TYPE)

        (localObs.key must_!= ObsKey) and
          (localNote.exists(_.key == noteKey) must beTrue) and
          localIs(PHASE_2_COMPLETE, ObsKey, env) and
          localIs(PI_TO_COMPLETE, localObs.key, env) and
          localHasNote(new UpdatePermissionFail(localObs.key), env)
      }
    }

    "incorporate remote version information in observation copies" in withVcs { env =>
      // add a note and sync
      val noteKey = env.remote.addNote("abc", ObsKey)
      unsafeSync(env)

      // edit remotely again, then delete the note altogether remotely.
      env.remote.updateDataObject[SPNote](noteKey)(_.setTitle("123"))
      val rVersions = env.remote.nodeVersions(noteKey)
      env.remote.delete(noteKey)
      env.remote.setObsPhase2Status(ObsKey, PHASE_2_COMPLETE)

      // edit locally
      env.local.updateDataObject[SPNote](noteKey)(_.setTitle("foo"))
      val lVersions = env.local.nodeVersions(noteKey)

      // now we expect that the note is not duplicated in the new observation
      // that is created and that it has version information from both sides
      afterPull(env, PiUserPrincipal) {
        env.local.nodeVersions(noteKey) must_== rVersions.sync(lVersions)
      }
    }

    "keep remote version information in restored observations" in withVcs { env =>
      // add a note and sync
      val noteKey = env.remote.addNote("abc", ObsKey)
      unsafeSync(env)

      // edit remotely again
      env.remote.updateDataObject[SPNote](noteKey)(_.setTitle("123"))
      val rVersions = env.remote.nodeVersions(noteKey)
      env.remote.setObsPhase2Status(ObsKey, PHASE_2_COMPLETE)

      // edit locally
      env.local.updateDataObject[SPNote](noteKey)(_.setTitle("foo"))

      // now the remote version of the node is restored with its remote version,
      // and a copy of the note is made with a starting version
      afterPull(env, PiUserPrincipal) {
        val localObs = env.local.prog.getConflictFolder.children.head
        val newNote  = localObs.children.find(_.getDataObject.getType == SPNote.SP_TYPE).get

        (env.local.nodeVersions(noteKey) must_== rVersions) and
          (newNote.getVersion must_== EmptyNodeVersions.incr(env.local.prog.getLifespanId)) and
          (env.local.descendant(noteKey).getDataObject.getTitle must_== "123") and
          (newNote.getDataObject.getTitle must_== "foo") and
          localHasNote(new UpdatePermissionFail(localObs.key), env)
      }
    }

    "renumber duplicated observations" in withVcs { env =>
      val newObsKey = env.remote.addObservation() // observation 2
      unsafeSync(env)

      // edit observation 1 locally
      env.local.setObsDataObject(ObsKey){ _.setTitle("abc") }

      // set observation 1 to ready remotely
      env.remote.setObsPhase2Status(ObsKey, PHASE_2_COMPLETE)

      // now the remote version of the node is restored with its remote version
      // and should still be observation 1.  a copy of the locally edited
      // observation must be added to the conflict folder with observation
      // number 3
      afterPull(env, PiUserPrincipal) {
        val obs3 = env.local.prog.findDescendant(_.getDataObject.getTitle == "abc").get.asInstanceOf[ISPObservation]
        (env.local.obs(ObsKey).getObservationNumber must_== 1) and
          (env.local.obs(newObsKey).getObservationNumber must_== 2) and
          (obs3.getObservationNumber must_== 3) and
          localHasNote(new UpdatePermissionFail(obs3.key), env)
      }
    }

  }

}
