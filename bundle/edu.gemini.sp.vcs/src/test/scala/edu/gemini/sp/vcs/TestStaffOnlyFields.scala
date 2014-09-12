package edu.gemini.sp.vcs

import org.junit.Test
import org.junit.Assert._
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.obs.ObsQaState
import edu.gemini.spModel.obscomp.{SPNote, ProgramNote}
import edu.gemini.sp.vcs.VcsFailure.NeedsUpdate

class TestStaffOnlyFields {

  import TestingEnvironment._

  private def isActive(pc: ProgContext): Boolean =
    pc.getDataObjectField[SPProgram, Boolean](pc.sp.getNodeKey, _.isActive)

  private def setActive(pc: ProgContext, active: Boolean): Unit =
    pc.setDataObjectField[SPProgram](pc.sp.getNodeKey, _.setActive(if (active) SPProgram.Active.YES else SPProgram.Active.NO))

  private def getStaffContact(pc: ProgContext): String =
    pc.getDataObjectField[SPProgram, String](pc.sp.getNodeKey, _.getContactPerson)

  private def setStaffContact(pc: ProgContext, email: String): Unit =
    pc.setDataObjectField[SPProgram](pc.sp.getNodeKey, _.setContactPerson(email))

  @Test def testPiCannotUpdateActiveFlag() {
     withPiTestEnv { env =>
       import env._

       update(user)

       // Starts out active
       assertTrue(isActive(local))

       // Make it inactive
       setActive(local, active = false)
       assertFalse(isActive(local))

       local.setTitle("My New Program Title")

       update(user)

       // reset to the original value
       assertTrue(isActive(local))

       // still has your title though
       assertEquals("My New Program Title", local.getTitle())

       commit()

       assertEquals("My New Program Title", remote.getTitle())
       assertTrue(isActive(remote))
     }
  }

  @Test def testPiCannotCommitActiveFlagUpdate() {
     withPiTestEnv { env =>
       import env._

       update(user)

       // Starts out active
       assertTrue(isActive(local))

       // Make it inactive
       setActive(local, active = false)
       assertFalse(isActive(local))

       local.setTitle("My New Program Title")

       // can't skip the update and just commit this change
       cantCommit(NeedsUpdate)

       // reset to the original value so we can commit
       setActive(local, active = true)
       assertTrue(isActive(local))

       // still has your title
       assertEquals("My New Program Title", local.getTitle())

       // no update, but now it is okay to commit
       commit()

       assertEquals("My New Program Title", remote.getTitle())
       assertTrue(isActive(remote))
     }
  }


  @Test def testPiCannotUpdateActiveFlagInAConflict() {
     withPiTestEnv { env =>
       import env._

       update(user)

       setActive(remote, active = false)

       local.setTitle("My New Program Title")
       assertTrue(isActive(local))

       update(user)

       assertTrue(local.sp.hasConflicts)

       // By default the remote version is displayed, which will have been
       // set inactive.
       assertFalse(isActive(local))

       // Also though the conflict version should be inactive
       assertFalse(local.sp.getConflicts.dataObjectConflict.getValue.dataObject.asInstanceOf[SPProgram].isActive)

       local.sp.resolveConflicts()
       commit()
     }
  }

  @Test def testStaffCanUpdateActiveFlagInAConflict() {
     withStaffUserTestEnv("abc@gemini.edu") { env =>
       import env._

       setStaffContact(remote, "abc@gemini.edu")

       update(user)

       setActive(remote, active = false)

       local.setTitle("My New Program Title")
       assertTrue(isActive(local))

       update(user)

       assertTrue(local.sp.hasConflicts)

       // By default the remote version is displayed, which will have been
       // set inactive.
       assertFalse(isActive(local))

       // The conflict version now has the original value.
       assertTrue(local.sp.getConflicts.dataObjectConflict.getValue.dataObject.asInstanceOf[SPProgram].isActive)

       local.sp.resolveConflicts()
       commit()
     }
  }

  @Test def testPiCannotUpdateObsQaState() {
     withPiTestEnv { env =>
       import env._

       val key = remote.addObservation().getNodeKey

       update(user)

       local.setObsField(key, _.setOverrideQaState(true))
       local.setObsField(key, _.setOverriddenObsQaState(ObsQaState.PASS))
       local.setObsField(key, _.setTitle("okay to change this"))

       assertEquals(local.getObsField[Boolean](key, _.isOverrideQaState), true)
       assertEquals(local.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.PASS)

       update(user)
       commit()

       assertEquals(local.getObsField[Boolean](key, _.isOverrideQaState), false)
       assertEquals(local.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.UNDEFINED)
       assertEquals(local.getObsField[String](key, _.getTitle), "okay to change this")
     }
  }

  @Test def testPiCannotCommitObsQaState() {
     withPiTestEnv { env =>
       import env._

       val key = remote.addObservation().getNodeKey

       update(user)

       local.setObsField(key, _.setOverrideQaState(true))
       local.setObsField(key, _.setOverriddenObsQaState(ObsQaState.PASS))
       local.setObsField(key, _.setTitle("okay to change this"))

       assertEquals(local.getObsField[Boolean](key, _.isOverrideQaState), true)
       assertEquals(local.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.PASS)

       cantCommit(NeedsUpdate)

       // go back to what it should be
       local.setObsField(key, _.setOverrideQaState(false))
       local.setObsField(key, _.setOverriddenObsQaState(ObsQaState.UNDEFINED))

       // Now we can commit our title change without an update
       commit()

       assertEquals(local.getObsField[Boolean](key, _.isOverrideQaState), false)
       assertEquals(local.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.UNDEFINED)
       assertEquals(local.getObsField[String](key, _.getTitle), "okay to change this")
     }
  }

  @Test def testStaffCanUpdateObsQaState() {
    withStaffUserTestEnv("abc@gemini.edu") { env =>
      import env._

      val key = remote.addObservation().getNodeKey
      setStaffContact(remote, "abc@gemini.edu")

      update(user)

      local.setObsField(key, _.setOverrideQaState(true))
      local.setObsField(key, _.setOverriddenObsQaState(ObsQaState.PASS))
      local.setObsField(key, _.setTitle("okay to change this"))

      assertEquals(local.getObsField[Boolean](key, _.isOverrideQaState), true)
      assertEquals(local.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.PASS)

      update(user)
      commit()

      // assertEquals(local.getObsField[Boolean](key, _.isOverrideQaState), true)
      // assertEquals(local.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.PASS)
      assertEquals(local.getObsField[String](key, _.getTitle), "okay to change this")
    }
  }

  @Test def testPiCannotCreateANewObservationWithQaState() {
     withPiTestEnv { env =>
       import env._

       val key = local.addObservation().getNodeKey

       local.setObsField(key, _.setOverrideQaState(true))
       local.setObsField(key, _.setOverriddenObsQaState(ObsQaState.PASS))
       local.setObsField(key, _.setTitle("okay to change this"))

       update(user)
       commit()

       assertEquals(local.getObsField[Boolean](key, _.isOverrideQaState), false)
       assertEquals(local.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.UNDEFINED)
       assertEquals(local.getObsField[String](key, _.getTitle), "okay to change this")

     }
  }

  @Test def testPiCannotCommitANewObservationWithQaState() {
     withPiTestEnv { env =>
       import env._

       val key = local.addObservation().getNodeKey

       local.setObsField(key, _.setOverrideQaState(true))
       local.setObsField(key, _.setOverriddenObsQaState(ObsQaState.PASS))
       local.setObsField(key, _.setTitle("okay to change this"))

       cantCommit(NeedsUpdate)

       update(user)
       commit()

       assertEquals(local.getObsField[Boolean](key, _.isOverrideQaState), false)
       assertEquals(local.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.UNDEFINED)
       assertEquals(local.getObsField[String](key, _.getTitle), "okay to change this")

     }
  }

  @Test def testNeedsSuperUserToChangeEmails() {
     withStaffUserTestEnv("abc@gemini.edu") { env =>
       import env._

       setStaffContact(remote, "abc@gemini.edu")

       update(user)

       setStaffContact(local, "xyz@gemini.edu")

       update(user)

       assertEquals(getStaffContact(local), "abc@gemini.edu")

       commit()
     }
  }

  @Test def testSuperUserCanChangeEmails() {
     withStaffTestEnv { env =>
       import env._

       setStaffContact(remote, "abc@gemini.edu")

       update(user)

       setStaffContact(local, "xyz@gemini.edu")

       update(user)

       assertEquals(getStaffContact(local), "xyz@gemini.edu")

       commit()
     }
  }

  @Test def testPiCannotUpdatePinkNotes() {
     withPiTestEnv { env =>
       import env._

       val note = remote.odb.getFactory.createObsComponent(remote.sp, ProgramNote.SP_TYPE, null)
       val obj  = new ProgramNote()
       obj.setTitle("Secret Title")
       obj.setNote("Secret Text")
       note.setDataObject(obj)

       remote.sp.addObsComponent(note)

       update(user)

       local.setNoteText("hello?", note.getNodeKey)

       update(user)

       // nope, can't do that
       assertEquals("Secret Text", local.find(note.getNodeKey).getDataObject.asInstanceOf[SPNote].getNote)

       commit()
     }
  }
}
