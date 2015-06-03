package edu.gemini.sp.vcs

import org.junit.Test
import org.junit.Assert._
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.obs.ObsQaState
import edu.gemini.spModel.obscomp.{SPNote, ProgramNote}
import edu.gemini.sp.vcs.OldVcsFailure.NeedsUpdate

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
     piSyncTest { env =>
       import env._

       update(user)

       // Starts out active
       assertTrue(isActive(cloned))

       // Make it inactive
       setActive(cloned, active = false)
       assertFalse(isActive(cloned))

       cloned.setTitle("My New Program Title")

       update(user)

       // reset to the original value
       assertTrue(isActive(cloned))

       // still has your title though
       assertEquals("My New Program Title", cloned.getTitle())

       commit()

       assertEquals("My New Program Title", central.getTitle())
       assertTrue(isActive(central))
     }
  }

  @Test def testPiCannotCommitActiveFlagUpdate() {
     piSyncTest { env =>
       import env._

       update(user)

       // Starts out active
       assertTrue(isActive(cloned))

       // Make it inactive
       setActive(cloned, active = false)
       assertFalse(isActive(cloned))

       cloned.setTitle("My New Program Title")

       // can't skip the update and just commit this change
       cantCommit(NeedsUpdate)

       // reset to the original value so we can commit
       setActive(cloned, active = true)
       assertTrue(isActive(cloned))

       // still has your title
       assertEquals("My New Program Title", cloned.getTitle())

       // no update, but now it is okay to commit
       commit()

       assertEquals("My New Program Title", central.getTitle())
       assertTrue(isActive(central))
     }
  }


  @Test def testPiCannotUpdateActiveFlagInAConflict() {
     piSyncTest { env =>
       import env._

       update(user)

       setActive(central, active = false)

       cloned.setTitle("My New Program Title")
       assertTrue(isActive(cloned))

       update(user)

       assertTrue(cloned.sp.hasConflicts)

       // By default the remote version is displayed, which will have been
       // set inactive.
       assertFalse(isActive(cloned))

       // Also though the conflict version should be inactive
       assertFalse(cloned.sp.getConflicts.dataObjectConflict.getValue.dataObject.asInstanceOf[SPProgram].isActive)

       cloned.sp.resolveConflicts()
       commit()
     }
  }

  @Test def testStaffCanUpdateActiveFlagInAConflict() {
     staffUserSyncTest("abc@gemini.edu") { env =>
       import env._

       setStaffContact(central, "abc@gemini.edu")

       update(user)

       setActive(central, active = false)

       cloned.setTitle("My New Program Title")
       assertTrue(isActive(cloned))

       update(user)

       assertTrue(cloned.sp.hasConflicts)

       // By default the remote version is displayed, which will have been
       // set inactive.
       assertFalse(isActive(cloned))

       // The conflict version now has the original value.
       assertTrue(cloned.sp.getConflicts.dataObjectConflict.getValue.dataObject.asInstanceOf[SPProgram].isActive)

       cloned.sp.resolveConflicts()
       commit()
     }
  }

  @Test def testPiCannotUpdateObsQaState() {
     piSyncTest { env =>
       import env._

       val key = central.addObservation().getNodeKey

       update(user)

       cloned.setObsField(key, _.setOverrideQaState(true))
       cloned.setObsField(key, _.setOverriddenObsQaState(ObsQaState.PASS))
       cloned.setObsField(key, _.setTitle("okay to change this"))

       assertEquals(cloned.getObsField[Boolean](key, _.isOverrideQaState), true)
       assertEquals(cloned.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.PASS)

       update(user)
       commit()

       assertEquals(cloned.getObsField[Boolean](key, _.isOverrideQaState), false)
       assertEquals(cloned.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.UNDEFINED)
       assertEquals(cloned.getObsField[String](key, _.getTitle), "okay to change this")
     }
  }

  @Test def testPiCannotCommitObsQaState() {
     piSyncTest { env =>
       import env._

       val key = central.addObservation().getNodeKey

       update(user)

       cloned.setObsField(key, _.setOverrideQaState(true))
       cloned.setObsField(key, _.setOverriddenObsQaState(ObsQaState.PASS))
       cloned.setObsField(key, _.setTitle("okay to change this"))

       assertEquals(cloned.getObsField[Boolean](key, _.isOverrideQaState), true)
       assertEquals(cloned.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.PASS)

       cantCommit(NeedsUpdate)

       // go back to what it should be
       cloned.setObsField(key, _.setOverrideQaState(false))
       cloned.setObsField(key, _.setOverriddenObsQaState(ObsQaState.UNDEFINED))

       // Now we can commit our title change without an update
       commit()

       assertEquals(cloned.getObsField[Boolean](key, _.isOverrideQaState), false)
       assertEquals(cloned.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.UNDEFINED)
       assertEquals(cloned.getObsField[String](key, _.getTitle), "okay to change this")
     }
  }

  @Test def testStaffCanUpdateObsQaState() {
    staffUserSyncTest("abc@gemini.edu") { env =>
      import env._

      val key = central.addObservation().getNodeKey
      setStaffContact(central, "abc@gemini.edu")

      update(user)

      cloned.setObsField(key, _.setOverrideQaState(true))
      cloned.setObsField(key, _.setOverriddenObsQaState(ObsQaState.PASS))
      cloned.setObsField(key, _.setTitle("okay to change this"))

      assertEquals(cloned.getObsField[Boolean](key, _.isOverrideQaState), true)
      assertEquals(cloned.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.PASS)

      update(user)
      commit()

      // assertEquals(local.getObsField[Boolean](key, _.isOverrideQaState), true)
      // assertEquals(local.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.PASS)
      assertEquals(cloned.getObsField[String](key, _.getTitle), "okay to change this")
    }
  }

  @Test def testPiCannotCreateANewObservationWithQaState() {
     piSyncTest { env =>
       import env._

       val key = cloned.addObservation().getNodeKey

       cloned.setObsField(key, _.setOverrideQaState(true))
       cloned.setObsField(key, _.setOverriddenObsQaState(ObsQaState.PASS))
       cloned.setObsField(key, _.setTitle("okay to change this"))

       update(user)
       commit()

       assertEquals(cloned.getObsField[Boolean](key, _.isOverrideQaState), false)
       assertEquals(cloned.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.UNDEFINED)
       assertEquals(cloned.getObsField[String](key, _.getTitle), "okay to change this")

     }
  }

  @Test def testPiCannotCommitANewObservationWithQaState() {
     piSyncTest { env =>
       import env._

       val key = cloned.addObservation().getNodeKey

       cloned.setObsField(key, _.setOverrideQaState(true))
       cloned.setObsField(key, _.setOverriddenObsQaState(ObsQaState.PASS))
       cloned.setObsField(key, _.setTitle("okay to change this"))

       cantCommit(NeedsUpdate)

       update(user)
       commit()

       assertEquals(cloned.getObsField[Boolean](key, _.isOverrideQaState), false)
       assertEquals(cloned.getObsField[ObsQaState](key, _.getOverriddenObsQaState), ObsQaState.UNDEFINED)
       assertEquals(cloned.getObsField[String](key, _.getTitle), "okay to change this")

     }
  }

  @Test def testNeedsSuperUserToChangeEmails() {
     staffUserSyncTest("abc@gemini.edu") { env =>
       import env._

       setStaffContact(central, "abc@gemini.edu")

       update(user)

       setStaffContact(cloned, "xyz@gemini.edu")

       update(user)

       assertEquals(getStaffContact(cloned), "abc@gemini.edu")

       commit()
     }
  }

  @Test def testSuperUserCanChangeEmails() {
     staffSyncTest { env =>
       import env._

       setStaffContact(central, "abc@gemini.edu")

       update(user)

       setStaffContact(cloned, "xyz@gemini.edu")

       update(user)

       assertEquals(getStaffContact(cloned), "xyz@gemini.edu")

       commit()
     }
  }

  @Test def testPiCannotUpdatePinkNotes() {
     piSyncTest { env =>
       import env._

       val note = central.odb.getFactory.createObsComponent(central.sp, ProgramNote.SP_TYPE, null)
       val obj  = new ProgramNote()
       obj.setTitle("Secret Title")
       obj.setNote("Secret Text")
       note.setDataObject(obj)

       central.sp.addObsComponent(note)

       update(user)

       cloned.setNoteText("hello?", note.getNodeKey)

       update(user)

       // nope, can't do that
       assertEquals("Secret Text", cloned.find(note.getNodeKey).getDataObject.asInstanceOf[SPNote].getNote)

       commit()
     }
  }
}
