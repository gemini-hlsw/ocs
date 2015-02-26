package edu.gemini.sp.vcs

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.Conflict._
import edu.gemini.sp.vcs.VcsFailure._
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.rich.pot.sp._

import org.junit.Test
import org.junit.Assert._

import scala.collection.JavaConverters._


class TestMerge {

  import TestingEnvironment._

  @Test def dataObjectFromRemote(): Unit =
    piSyncTest { env =>
      import env._

      central.setTitle("New Title (Remote)")
      nodeVersionsEqual(key, fL = _.incr(central.lifespanId))

      update(user)
    }

  @Test def dataObjectFromLocal(): Unit =
    piSyncTest { env =>
      import env._

      cloned.setTitle("New Title (Local)")
      nodeVersionsEqual(key, fR = _.incr(cloned.lifespanId))

      update(user)
      nodeVersionsEqual(key, fR = _.incr(cloned.lifespanId))

      commit()
    }

  @Test def dataObjectConflict(): Unit =
    piSyncTest { env =>
      import env._

      central.setTitle("New Title (Remote)")
      nodeVersionsEqual(key, fL = _.incr(central.lifespanId))

      cloned.setTitle("New Title (Local)")
      nodeVersionsEqual(key, fR = _.incr(cloned.lifespanId), fL = _.incr(central.lifespanId))

      cantCommit(NeedsUpdate)
      update(user)

      assertTrue(cloned.sp.hasConflicts)
      assertEquals(0, cloned.sp.getConflicts.notes.size) // no tree conflicts
      nodeVersionsEqual(key, fR = _.incr(cloned.lifespanId))

      cantCommit(HasConflict)
      cloned.sp.resolveDataObjectConflict()

      commit()
    }

  @Test def noDataObjectConflictWhenChangesAreTheSame(): Unit =
    piSyncTest { env =>
      import env._

      central.setTitle("New Title")
      nodeVersionsEqual(key, fL = _.incr(central.lifespanId))

      cloned.setTitle("New Title")
      nodeVersionsEqual(key, fR = _.incr(cloned.lifespanId), fL = _.incr(central.lifespanId))

      cantCommit(NeedsUpdate)
      update(user)

      assertFalse(cloned.sp.hasConflicts)
      nodeVersionsEqual(key, fR = _.incr(cloned.lifespanId))

      commit()
    }

  /* Disabled for now, the QA log is not mergeable any longer....
  @Test def noDataObjectConflictWhenMergeable() {
    withAuthTestEnv(new StaffPrincipal("Gemini")) { env =>
      import env._

      val rObs = remote.addObservation()
      ObsLog.update(remote.odb, rObs.getObservationID, new ObsLog.UpdateOp {
        def apply(obs: ISPObservation, log: ObsLog) { /* empty */}
      })

      update(user)

      val lab1 = new DatasetLabel("GS-2013B-Q-1-1-1")
      val lab2 = new DatasetLabel("GS-2013B-Q-1-1-2")

      ObsLog.update(remote.odb, rObs.getObservationID, new ObsLog.UpdateOp {
        def apply(obs: ISPObservation, log: ObsLog) {
          log.qaLogDataObject.setComment(lab1, "remote comment")
        }
      })

      ObsLog.update(local.odb, rObs.getObservationID, new ObsLog.UpdateOp {
        def apply(obs: ISPObservation, log: ObsLog) {
          log.qaLogDataObject.setComment(lab2, "local comment")
        }
      })

      update(user)

      commit()
    }
  }
  */

  @Test def newNodeFromRemote(): Unit =
    piSyncTest { env =>
      import env._

      val note = central.addNote("Remote Note")
      nodeVersionsEqual(key, fL = _.incr(central.lifespanId))
      nodeVersionsEqual(note.getNodeKey, fL = _.incr(central.lifespanId))

      update(user)
    }

  @Test def newNodeFromLocal(): Unit =
    piSyncTest { env =>
      import env._

      val note = cloned.addNote("Local Note")
      nodeVersionsEqual(key, fR = _.incr(cloned.lifespanId))
      nodeVersionsEqual(note.getNodeKey, fR = _.incr(cloned.lifespanId))

      commit()
    }

  @Test def rearrangeNodes(): Unit =
    piSyncTest { env =>
      import env._

      val note0 = cloned.addNote("Local Note 0")
      val note1 = cloned.addNote("Local Note 1")
      val note2 = cloned.addNote("Local Note 2")

      commit()

      assertEquals(childKeys(central.sp), keys(note0, note1, note2))

      cloned.sp.setChildren(List(note2, note0, note1).asJava)

      commit()

      assertEquals(childKeys(central.sp), keys(note2, note0, note1))
    }


  @Test def newNodeNoConflict(): Unit =
    piSyncTest { env =>
      import env._

      val noteR = central.addNote("Remote Note")
      val noteL = cloned.addNote("Local Note")
      cantCommit(NeedsUpdate)
      update(user)

      // Keep both notes.
      val expected = keys(noteL, noteR)
      assertEquals(expected, childKeys(cloned.sp))
      assertFalse(cloned.sp.hasConflicts)

      commit()
      assertEquals(expected, childKeys(central.sp))
    }

  @Test def deleteNodeBeforeKnownToLocal(): Unit =
    piSyncTest { env =>
      import env._

      // Create, edit, delete a note before we ever get it in the local program
      val note = central.addNote("Note Text")
      central.setNoteText("Remote Edit", note.getNodeKey)
      central.delete(note.getNodeKey)

      update(user)
      versionsEqual()
    }

  @Test def deleteNodeBeforeKnownToRemote(): Unit =
    piSyncTest { env =>
      import env._

      // Create, edit, delete a note before we ever put it in the remote program
      val note = cloned.addNote("Note Text")
      cloned.setNoteText("Local Edit", note.getNodeKey)
      cloned.delete(note.getNodeKey)

      commit()
      versionsEqual()
    }

  @Test def deleteNodeFromBothAfterModifications(): Unit =
    piSyncTest { env =>
      import env._

      val note = central.addNote("Note Text")
      update(user)
      versionsEqual()

      // Edit both versions of the note.
      central.setNoteText("Remote Edit", note.getNodeKey)
      cloned.setNoteText("Local Edit", note.getNodeKey)

      // Delete both versions of the note.
      central.delete(note.getNodeKey)
      cloned.delete(note.getNodeKey)

      update(user)
      commit()
    }

  @Test def deletedNodeFromRemote(): Unit =
    piSyncTest { env =>
      import env._

      val noteR1 = central.addNote("Remote Note 1")
      val noteR2 = central.addNote("Remote Note 2")
      update(user)

      // We have them both now in both remote and local versions
      assertEquals(keys(noteR1, noteR2), childKeys(cloned.sp))
      versionsEqual()

      // Delete noteR1 in remote program
      central.delete(noteR1.getNodeKey)
      assertEquals(keys(noteR2), childKeys(central.sp))

      cantCommit(NeedsUpdate)
      update(user)

      // Gone in the local program as well.
      assertEquals(keys(noteR2), childKeys(cloned.sp))
    }

  @Test def deletedNodeFromLocal(): Unit =
    piSyncTest { env =>
      import env._

      val noteR1 = central.addNote("Remote Note 1")
      val noteR2 = central.addNote("Remote Note 2")
      update(user)

      // We have them both now in both remote and local versions
      assertEquals(keys(noteR1, noteR2), childKeys(cloned.sp))
      versionsEqual()

      // Delete noteR2 in local program
      cloned.delete(noteR2.getNodeKey)
      assertEquals(keys(noteR1, noteR2), childKeys(central.sp))
      assertEquals(keys(noteR1), childKeys(cloned.sp))

      commit()

      // Gone in the remote program as well.
      assertEquals(keys(noteR1), childKeys(central.sp))
    }

  @Test def deletedNodesNoConflict() {
    piSyncTest {
      env =>
        import env._

        val noteR1 = central.addNote("Remote Note 1")
        val noteR2 = central.addNote("Remote Note 2")
        update(user)

        // We have them both now
        assertEquals(keys(noteR1, noteR2), childKeys(cloned.sp))
        versionsEqual()

        // Delete noteR1 in remote program
        central.delete(noteR1.getNodeKey)
        assertEquals(keys(noteR2), childKeys(central.sp))

        // Delete noteR2 in local program
        cloned.delete(noteR2.getNodeKey)
        assertEquals(keys(noteR1), childKeys(cloned.sp))

        cantCommit(NeedsUpdate)
        update(user)

        // Both gone in the local program, remote still has noteR2
        assertEquals(Nil, childKeys(cloned.sp))
        assertEquals(keys(noteR2), childKeys(central.sp))

        // no conflict here
        assertFalse(cloned.sp.hasConflicts)

        // Commit to update remote so that it has no children
        commit()
        assertEquals(Nil, childKeys(central.sp))
        assertFalse(central.sp.hasConflicts)
    }
  }

  @Test def cannotRemoveModifiedLocalChild() {
    piSyncTest {
      env =>
        import env._

        def showState(title: String) {
          println(formatState(title, List(central, cloned)))
        }

        val note = central.addNote("Remote Note")
        showTree(central.sp)

        update(user)
        showState("PI Updates")

        versionsEqual()

        // Delete note in remote program
        central.delete(note.getNodeKey)

        showState("Note Deleted Remotely")

        // Modify note in local program
        cloned.setNoteText("Local Note", note.getNodeKey)
        cantCommit(NeedsUpdate)
        update(user)

        showState("PI Updates Again")

        // Now we have a copy of the note locally with a new key.
        val newNote = cloned.sp.getObsComponents.get(0)
        assertEquals(keys(newNote), childKeys(cloned.sp))
        assertEquals(Nil, childKeys(central.sp))

        assertTrue(cloned.sp.hasConflicts)
        val List(cn) = cloned.conflictNotes()
        assertEquals(new ReplacedRemoteDelete(newNote.getNodeKey), cn)

        cantCommit(HasConflict)
        cloned.sp.resolveConflict(cn)

        // Commit to update remote so that it has the child again
        commit()
        assertEquals(keys(newNote), childKeys(central.sp))
    }
  }

  @Test def cannotRemoveModifiedLocalChildWithConflict() {
    piSyncTest {
      env =>
        import env._

        val note = central.addNote("Remote Note")
        update(user)
        versionsEqual()

        // Edit the note in the remote program to increase its version number
        central.setNoteText("Updated Note Text", note.getNodeKey)
        // Now delete note in remote program
        central.delete(note.getNodeKey)

        // Modify note in local program
        cloned.setNoteText("Local Note", note.getNodeKey)
        cantCommit(NeedsUpdate)
        update(user)

        // Now we have a copy of the note locally with a new key.
        val newNote = cloned.sp.getObsComponents.get(0)
        assertEquals(keys(newNote), childKeys(cloned.sp))
        assertEquals(Nil, childKeys(central.sp))

        val List(cn) = cloned.conflictNotes()
        assertEquals(new ReplacedRemoteDelete(newNote.getNodeKey), cn)

        cantCommit(HasConflict)
        cloned.sp.resolveConflict(cn)

        // Commit to update remote so that it has the child again
        commit()
        assertEquals(keys(newNote), childKeys(central.sp))
    }
  }

  @Test def cannotRemoveModifiedRemoteChild() {
    piSyncTest {
      env =>
        import env._

        val note = central.addNote("Remote Note")
        update(user)
        versionsEqual()

        // Delete note in local program
        cloned.delete(note.getNodeKey)
        assertEquals(Nil, childKeys(cloned.sp))

        // Modify note in remote program
        central.setNoteText("Remote Note Modified", note.getNodeKey)

        cantCommit(NeedsUpdate)
        update(user)

        // Note comes back locally
        assertEquals(keys(note), childKeys(cloned.sp))

        val List(cn) = cloned.conflictNotes()
        assertEquals(new ResurrectedLocalDelete(note.getNodeKey), cn)

        cantCommit(HasConflict)
        cloned.sp.resolveConflict(cn)

        commit()
        assertEquals(keys(note), childKeys(central.sp))
    }
  }

  @Test def cannotRemoveNodeWithModifiedLocalChild() {
    piSyncTest {
      env =>
        import env._

        val obs = central.addObservation()
        val note = central.addNote("Remote Note", obs.getNodeKey)
        update(user)
        versionsEqual()

        // Delete obs in remote program
        central.delete(obs.getNodeKey)
        assertEquals(Nil, childKeys(central.sp))

        // Modify note in local program
        cloned.setNoteText("Local Note", note.getNodeKey)

        cantCommit(NeedsUpdate)
        update(user)

        // Now I have a copy of the observation
        val newObs = cloned.sp.getObservations.get(0)
        assertEquals(keys(newObs), childKeys(cloned.sp))
        val newNote = newObs.getObsComponents.get(0)
        assertTrue(childKeys(newObs).contains(newNote.getNodeKey))

        val List(cn) = cloned.conflictNotes()
        assertEquals(new ReplacedRemoteDelete(newObs.getNodeKey), cn)

        cantCommit(HasConflict)
        cloned.sp.resolveConflict(cn)

        // After commit, the observation comes back in the remote program too
        commit()
        assertEquals(keys(newObs), childKeys(central.sp))
        assertTrue(childKeys(central.find(newObs.getNodeKey)).contains(newNote.getNodeKey))
    }
  }

  @Test def cannotRemoveNodeWithModifiedRemoteChild() {
    piSyncTest {
      env =>
        import env._

        val obs = central.addObservation()
        val note = central.addNote("Remote Note", obs.getNodeKey)
        update(user)
        versionsEqual()

        // Delete obs in local program
        cloned.delete(obs.getNodeKey)
        assertEquals(Nil, childKeys(cloned.sp))

        // Modify note in remote program
        central.setNoteText("Remote Note Modified", note.getNodeKey)

        cantCommit(NeedsUpdate)
        update(user)

        // Observation comes back in the local program
        assertEquals(keys(obs), childKeys(cloned.sp))
        assertTrue(childKeys(cloned.find(obs.getNodeKey)).contains(note.getNodeKey))

        val List(cn) = cloned.conflictNotes()
        assertEquals(new ResurrectedLocalDelete(obs.getNodeKey), cn)

        cantCommit(HasConflict)
        cloned.sp.resolveConflict(cn)

        // After commit,the observation is still in the remote program
        commit()
        assertEquals(keys(obs), childKeys(central.sp))
        assertTrue(childKeys(central.find(obs.getNodeKey)).contains(note.getNodeKey))
    }
  }

  private def obsNumbersEqual(pc: ProgContext, pairs: (ISPObservation, Int)*) {
    val expected: List[(String, Int)] =
      (pairs map {
        case (obs, n) => pc.getTitle(obs.getNodeKey) -> n
      }).toList

    val actual: List[(String, Int)] =
      (pc.sp.getAllObservations.asScala map {
        obs => pc.getTitle(obs.getNodeKey) -> obs.getObservationNumber
      }).toList

    assertEquals(expected, actual)
  }

  private def mkNamedObs(pc: ProgContext, name: String, i: Int = -1): ISPObservation = {
    val obs = pc.odb.getFactory.createObservation(pc.sp, null)
    if (i >= 0) pc.sp.addObservation(i, obs) else pc.sp.addObservation(obs)
    pc.setTitle(name, obs.getNodeKey)
    obs
  }

  @Test def insert() {
    piSyncTest { env =>
      import env._


      val obs1 = mkNamedObs(central, "obs1")
      val obs2 = mkNamedObs(central, "obs2")
      update(user)
      versionsEqual()

      val obs3 = mkNamedObs(central, "obs3", 1)
      val obs4 = mkNamedObs(cloned, "obs4", 1)
      val obs5 = mkNamedObs(cloned, "obs5", 2)

      assertEquals(keys(obs1, obs3, obs2), childKeys(central.sp))
      assertEquals(keys(obs1, obs4, obs5, obs2), childKeys(cloned.sp))

      // In the remote program "obs3" is the 3rd observation whereas in the
      // local program "obs4" is the 3rd observation and "obs5" is the 4th.
      obsNumbersEqual(central, obs1 -> 1, obs3 -> 3, obs2 -> 2)
      obsNumbersEqual(cloned, obs1 -> 1, obs4 -> 3, obs5 -> 4, obs2 -> 2)

      update(user)
      assertEquals(keys(obs1, obs4, obs5, obs3, obs2), childKeys(cloned.sp))

      // After the update, there would have been two observations with number
      // 3 so they had to be renumbered. In particular, "obs3" from the
      // remote program stays as number 3 but in the local program we renumbered
      // "obs4" to become obs #4 and "obs5" to become obs #5
      obsNumbersEqual(cloned, obs1 -> 1, obs4 -> 4, obs5 -> 5, obs3 -> 3, obs2 -> 2)

      commit()
      assertEquals(keys(obs1, obs4, obs5, obs3, obs2), childKeys(central.sp))

      // After the commit in the local program, we have two new observations
      // which keep their numbers because they are unique in the remote
      // program.
      obsNumbersEqual(central, obs1 -> 1, obs4 -> 4, obs5 -> 5, obs3 -> 3, obs2 -> 2)
    }
  }

  @Test def dontRenumberWhenNotNecessary() {
    piSyncTest { env =>
      import env._

      update(user)

      val obs1 = cloned.addObservation()
      val obs2 = cloned.addObservation()
      obsNumbersEqual(cloned, obs1 -> 1, obs2 -> 2)

      // remove observation 1 but that shouldn't cause 2 to be renumbered
      cloned.sp.removeObservation(obs1)
      obsNumbersEqual(cloned, obs2 -> 2)

      update(user)
      commit()

      assertEquals(1, cloned.sp.getObservations.size())
      assertEquals(1, central.sp.getObservations.size())
      obsNumbersEqual(cloned, obs2 -> 2)
      obsNumbersEqual(central, obs2 -> 2)

      // next observation starts at 3
      val obs3R = central.addObservation()
      val obs3L = cloned.addObservation()

      obsNumbersEqual(central, obs2 -> 2, obs3R -> 3)
      obsNumbersEqual(cloned, obs2 -> 2, obs3L -> 3)

      update(user)

      // have to renumber here to avoid having two observation #3
      obsNumbersEqual(cloned, obs2 -> 2, obs3L -> 4, obs3R -> 3)

      commit()

      obsNumbersEqual(central, obs2 -> 2, obs3L -> 4, obs3R -> 3)
    }
  }

  @Test def reuseObservationNumbersWhenPossible() {
    piSyncTest { env =>
      import env._

      update(user)

      (1 to 9).foreach { _ =>
        val obs = central.addObservation()
        central.sp.removeObservation(obs)
      }
      val obs10 = mkNamedObs(central, "obs10")

      (1 to 8).foreach { _ =>
        val obs = cloned.addObservation()
        cloned.sp.removeObservation(obs)
      }

      // 9 must become 11 after update
      val obs9_11 = mkNamedObs(cloned, "obs9-11")

      // 10 and 11 "al agua"
      cloned.sp.removeObservation(cloned.addObservation())
      cloned.sp.removeObservation(cloned.addObservation())

      // 12, 13 can stay
      val obs12 = mkNamedObs(cloned, "obs12")
      val obs13 = mkNamedObs(cloned, "obs13")

      update(user)

      obsNumbersEqual(cloned, obs9_11 -> 11, obs12 -> 12, obs13 -> 13, obs10 -> 10)
      commit()
    }
  }

  @Test def bumpObservationNumbersWhenNecessary() {
    piSyncTest { env =>
      import env._

      update(user)

      (1 to 9).foreach { _ =>
        val obs = central.addObservation()
        central.sp.removeObservation(obs)
      }
      val obs10 = mkNamedObs(central, "obs10")

      (1 to 8).foreach { _ =>
        val obs = cloned.addObservation()
        cloned.sp.removeObservation(obs)
      }

      // 9 must become 11 after update
      val obs9_11 = mkNamedObs(cloned, "obs9-11")

      // 10 "al agua"
      cloned.sp.removeObservation(cloned.addObservation())

      // 11 bumped to 12, 12 bumped to 13
      val obs11_12 = mkNamedObs(cloned, "obs11-12")
      val obs12_13 = mkNamedObs(cloned, "obs12-13")

      update(user)

      obsNumbersEqual(cloned, obs9_11 -> 11, obs11_12 -> 12, obs12_13 -> 13, obs10 -> 10)
      commit()
    }
  }

  @Test def move() {
    piSyncTest {
      env =>
        import env._

        def mkGroup(pc: ProgContext, name: String): ISPGroup = {
          val grp = pc.addGroup()
          pc.setTitle(name, grp.getNodeKey)
          grp
        }

        def assertContainsChildren(pc: ProgContext, parent: ISPNode, child: ISPNode*) {
          val p = pc.find(parent.getNodeKey)
          val childKeys = child.map(_.getNodeKey).toSet
          assertTrue(p.children.map(_.getNodeKey).toSet == childKeys)
        }

        val grpOrigR = mkGroup(central, "orig")
        val note1 = central.addNote("1", grpOrigR.getNodeKey)
        val note2 = central.addNote("2", grpOrigR.getNodeKey)
        val note3 = central.addNote("3", grpOrigR.getNodeKey)
        update(user)
        versionsEqual()


        val grpR = mkGroup(central, "groupR")
        val grpL = mkGroup(cloned, "groupL")

        // move note3 to grpR in remote
        central.move(note3.getNodeKey, grpR.getNodeKey)

        // move notes 2 and 3 to grpL in local
        cloned.move(note3.getNodeKey, grpL.getNodeKey)
        cloned.move(note2.getNodeKey, grpL.getNodeKey)

        // update and see all three groups in the local program
        update(user)
        assertContainsChildren(cloned, cloned.sp, grpOrigR, grpR, grpL)

        // grpOrig should have note 1
        assertContainsChildren(cloned, grpOrigR, note1)

        // grpL should have note2 and a move conflict for note3
        assertContainsChildren(cloned, grpL, note2)
        val c = cloned.find(grpL.getNodeKey).getConflicts
        c.notes.toList.asScala.toList match {
          case List(mvConflict) =>
            assertEquals(new Conflict.Moved(note3.getNodeKey, grpR.getNodeKey), mvConflict)
          case _ => fail()
        }

        // grpR should have note3
        assertContainsChildren(cloned, grpR, note3)

        // acknowledge the conflict and commit
        cloned.find(grpL.getNodeKey).resolveConflicts()
        commit()
    }
  }

  @Test def conflictingMove() {
    piSyncTest {
      env =>
        import env._

        def mkGroup(pc: ProgContext, name: String): ISPGroup = {
          val grp = pc.addGroup()
          pc.setTitle(name, grp.getNodeKey)
          grp
        }

        val grp1R = mkGroup(central, "grp1")
        val grp2R = mkGroup(central, "grp2")
        val grp3R = mkGroup(central, "grp3")
        val obsR = central.addObservation(grp1R.getNodeKey)

        update(user)
        versionsEqual()

        // Move obs to grp2 remotely
        grp1R.children = Nil
        grp2R.children = List(obsR)

        // Move obs to grp3 locally
        val grp1L = cloned.find(grp1R.getNodeKey)
        val grp2L = cloned.find(grp2R.getNodeKey)
        val grp3L = cloned.find(grp3R.getNodeKey)
        val obsL = cloned.find(obsR.getNodeKey)

        grp1L.children = Nil
        grp3L.children = List(obsL)

        // Update should move the obs to grp2 locally to match the remote program
        update(user)

        def assertMovedToGroup2() {
          def format(pc: ProgContext, key: SPNodeKey): String =
            "%s - %s".format(pc.name, pc.getTitle(key))

          List((central, grp2R), (cloned, grp2L)) foreach {
            case (pc, grp) =>
              val key = grp.getNodeKey
              assertEquals(format(pc, key), keys(obsR), childKeys(pc.find(key)))
          }

          List((central, grp1R), (cloned, grp1L), (central, grp3R), (cloned, grp3L)) foreach {
            case (pc, grp) =>
              val key = grp.getNodeKey
              assertEquals(format(pc, key), Nil, childKeys(pc.find(key)))
          }
        }
        assertMovedToGroup2()

        // We expect grp3 to have a MOVED conflict.
        val g3 = cloned.find(grp3L.getNodeKey)
        assertTrue(g3.hasConflicts)
        val cn = g3.getConflicts.notes.get(0)
        assertEquals(new Moved(obsL.getNodeKey, grp2L.getNodeKey), cn)

        // Can't commit with a conflict.
        cantCommit(HasConflict)

        // Clear the conflict.
        g3.resolveConflict(cn)

        // Now we can commit.
        commit()
        assertMovedToGroup2()
    }
  }

  @Test def updateNodeAlreadyInConflict() {
    piSyncTest {
      env =>
        import env._

        val note = central.addNote("Remote Note")
        update(user)

        // Delete note in local program, modify note in remote program
        cloned.delete(note.getNodeKey)
        central.setNoteText("Remote Note Modified", note.getNodeKey)
        update(user)

        // Note comes back locally
        assertEquals(keys(note), childKeys(cloned.sp))

        val expect = new ResurrectedLocalDelete(note.getNodeKey)
        val List(actual0) = cloned.conflictNotes()
        assertEquals(expect, actual0)
        cantCommit(HasConflict)

        // Update again and we still have the conflict.
        update(user)
        val List(actual1) = cloned.conflictNotes()
        assertEquals(expect, actual1)
        cantCommit(HasConflict)

        // Resolve and commit.
        cloned.sp.resolveConflict(actual1)
        commit()
        assertEquals(keys(note), childKeys(central.sp))
    }
  }

  @Test def updateWithExistingDataObjectConflict() {
    piSyncTest {
      env =>
        import env._

        central.setTitle("New Title (Remote)")
        cloned.setTitle("New Title (Local)")
        cantCommit(NeedsUpdate)
        update(user)

        assertTrue(cloned.sp.hasConflicts)

        import DataObjectConflict.Perspective._

        def verifyDoc(conflictTitle: String, p: DataObjectConflict.Perspective, dObjTitle: String) {
          val actual = cloned.sp.getConflicts.dataObjectConflict.getValue
          assertEquals(conflictTitle, actual.dataObject.asInstanceOf[SPProgram].getTitle)
          assertEquals(p, actual.perspective)
          assertEquals(dObjTitle, cloned.sp.getDataObject.asInstanceOf[SPProgram].getTitle)
        }

        verifyDoc("New Title (Local)", LOCAL, "New Title (Remote)")
        cantCommit(HasConflict)

        // Change the remote title again and update.
        central.setTitle("Updated Remote Title")
        update(user)

        assertTrue(cloned.sp.hasConflicts)

        // Here we lose the "New Title (Local)".  It's been replaced with the
        // previous update from the remote database.
        verifyDoc("New Title (Remote)", LOCAL, "Updated Remote Title")

        // Now toggle the data object conflict.
        cloned.sp.swapDataObjectConflict()
        verifyDoc("Updated Remote Title", REMOTE, "New Title (Remote)")

        // Edit it remotely again and update.
        central.setTitle("Final Remote Title")
        update(user)
        verifyDoc("New Title (Remote)", LOCAL, "Final Remote Title")

        cloned.sp.resolveDataObjectConflict()
        commit()
    }
  }

  @Test def testMergeValidityIssue() {
    piSyncTest {
      env =>
        import env._

        // ! creates and adds and observation, modifying the remote program
        val key = central.addObservation().getNodeKey

        update(user)

        // Add a new instrument to both the remote and the local observations
        val remoteObs = central.sp.getObservations.get(0)
        remoteObs.addObsComponent(central.odb.getFactory.createObsComponent(central.sp, SPComponentType.INSTRUMENT_GMOS, null))
        val localObs = cloned.sp.getObservations.get(0)
        localObs.addObsComponent(cloned.odb.getFactory.createObsComponent(cloned.sp, SPComponentType.INSTRUMENT_NIRI, null))

        // Update, which should put one of these instruments in a conflict folder
        update(user)
        val cf = cloned.findObs(key).getConflictFolder
        assertNotNull(cf)

        val instComp = cf.children(0)
        instComp.getConflicts.notes.get(0) match {
          case _: ConstraintViolation => // okay
          case _ => fail()
        }
        assertTrue(instComp.asInstanceOf[ISPObsComponent].getType.broadType == SPComponentBroadType.INSTRUMENT)

        // remove the conflict generated by having two instruments
        cloned.findObs(key).removeConflictFolder()

        // TODO: spurious conflict in target component ... have to get rid of this
        cloned.sp.children(0).children.find {
            case oc: ISPObsComponent if oc.getType == SPComponentType.TELESCOPE_TARGETENV => true
            case _ => false
        }.foreach {
          _.resolveConflicts()
        }

        commit()
    }
  }

  @Test def newTemplateFolder() {
    piSyncTest {
      env =>
        import env._

        val tf = cloned.odb.getFactory.createTemplateFolder(cloned.sp, null)
        cloned.sp.setTemplateFolder(tf)
        commit()

        assertNotNull(cloned.sp.getTemplateFolder)

        update(user)

        assertNotNull(cloned.sp.getTemplateFolder)

        assertNotNull(central.sp.getTemplateFolder)
        assertEquals(cloned.sp.getTemplateFolder.getNodeKey, central.sp.getTemplateFolder.getNodeKey)
    }
  }
}