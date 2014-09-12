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

  @Test def dataObjectFromRemote() {
    withPiTestEnv {
      env =>
        import env._

        remote.setTitle("New Title (Remote)")
        nodeVersionsEqual(key, fL = _.incr(remote.lifespanId))

        update(user)
    }
  }

  @Test def dataObjectFromLocal() {
    withPiTestEnv {
      env =>
        import env._

        local.setTitle("New Title (Local)")
        nodeVersionsEqual(key, fR = _.incr(local.lifespanId))

        update(user)
        nodeVersionsEqual(key, fR = _.incr(local.lifespanId))

        commit()
    }
  }

  @Test def dataObjectConflict() {
    withPiTestEnv {
      env =>
        import env._

        remote.setTitle("New Title (Remote)")
        nodeVersionsEqual(key, fL = _.incr(remote.lifespanId))

        local.setTitle("New Title (Local)")
        nodeVersionsEqual(key, fR = _.incr(local.lifespanId), fL = _.incr(remote.lifespanId))

        cantCommit(NeedsUpdate)
        update(user)

        assertTrue(local.sp.hasConflicts)
        assertEquals(0, local.sp.getConflicts.notes.size) // no tree conflicts
        nodeVersionsEqual(key, fR = _.incr(local.lifespanId))

        cantCommit(HasConflict)
        local.sp.resolveDataObjectConflict()

        commit()
    }
  }

  @Test def noDataObjectConflictWhenChangesAreTheSame() {
    withPiTestEnv {
      env =>
        import env._

        remote.setTitle("New Title")
        nodeVersionsEqual(key, fL = _.incr(remote.lifespanId))

        local.setTitle("New Title")
        nodeVersionsEqual(key, fR = _.incr(local.lifespanId), fL = _.incr(remote.lifespanId))

        cantCommit(NeedsUpdate)
        update(user)

        assertFalse(local.sp.hasConflicts)
        nodeVersionsEqual(key, fR = _.incr(local.lifespanId))

        commit()
    }
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

  @Test def newNodeFromRemote() {
    withPiTestEnv {
      env =>
        import env._

        val note = remote.addNote("Remote Note")
        nodeVersionsEqual(key, fL = _.incr(remote.lifespanId))
        nodeVersionsEqual(note.getNodeKey, fL = _.incr(remote.lifespanId))

        update(user)
    }
  }

  @Test def newNodeFromLocal() {
    withPiTestEnv {
      env =>
        import env._

        val note = local.addNote("Local Note")
        nodeVersionsEqual(key, fR = _.incr(local.lifespanId))
        nodeVersionsEqual(note.getNodeKey, fR = _.incr(local.lifespanId))

        commit()
    }
  }

  @Test def rearrangeNodes() {
    withPiTestEnv {
      env =>
        import env._

        val note0 = local.addNote("Local Note 0")
        val note1 = local.addNote("Local Note 1")
        val note2 = local.addNote("Local Note 2")

        commit()

        assertEquals(childKeys(remote.sp), keys(note0, note1, note2))

        local.sp.setChildren(List(note2, note0, note1).asJava)

        commit()

        assertEquals(childKeys(remote.sp), keys(note2, note0, note1))
    }
  }


  @Test def newNodeNoConflict() {
    withPiTestEnv {
      env =>
        import env._

        val noteR = remote.addNote("Remote Note")
        val noteL = local.addNote("Local Note")
        cantCommit(NeedsUpdate)
        update(user)

        // Keep both notes.
        val expected = keys(noteL, noteR)
        assertEquals(expected, childKeys(local.sp))
        assertFalse(local.sp.hasConflicts)

        commit()
        assertEquals(expected, childKeys(remote.sp))
    }
  }

  @Test def deleteNodeBeforeKnownToLocal() {
    withPiTestEnv {
      env =>
        import env._

        // Create, edit, delete a note before we ever get it in the local program
        val note = remote.addNote("Note Text")
        remote.setNoteText("Remote Edit", note.getNodeKey)
        remote.delete(note.getNodeKey)

        update(user)
        versionsEqual()
    }
  }

  @Test def deleteNodeBeforeKnownToRemote() {
    withPiTestEnv {
      env =>
        import env._

        // Create, edit, delete a note before we ever put it in the remote program
        val note = local.addNote("Note Text")
        local.setNoteText("Local Edit", note.getNodeKey)
        local.delete(note.getNodeKey)

        commit()
        versionsEqual()
    }
  }

  @Test def deleteNodeFromBothAfterModifications() {
    withPiTestEnv {
      env =>
        import env._

        val note = remote.addNote("Note Text")
        update(user)
        versionsEqual()

        // Edit both versions of the note.
        remote.setNoteText("Remote Edit", note.getNodeKey)
        local.setNoteText("Local Edit", note.getNodeKey)

        // Delete both versions of the note.
        remote.delete(note.getNodeKey)
        local.delete(note.getNodeKey)

        update(user)
        commit()
    }
  }

  @Test def deletedNodeFromRemote() {
    withPiTestEnv {
      env =>
        import env._

        val noteR1 = remote.addNote("Remote Note 1")
        val noteR2 = remote.addNote("Remote Note 2")
        update(user)

        // We have them both now in both remote and local versions
        assertEquals(keys(noteR1, noteR2), childKeys(local.sp))
        versionsEqual()

        // Delete noteR1 in remote program
        remote.delete(noteR1.getNodeKey)
        assertEquals(keys(noteR2), childKeys(remote.sp))

        cantCommit(NeedsUpdate)
        update(user)

        // Gone in the local program as well.
        assertEquals(keys(noteR2), childKeys(local.sp))
    }
  }

  @Test def deletedNodeFromLocal() {
    withPiTestEnv {
      env =>
        import env._

        val noteR1 = remote.addNote("Remote Note 1")
        val noteR2 = remote.addNote("Remote Note 2")
        update(user)

        // We have them both now in both remote and local versions
        assertEquals(keys(noteR1, noteR2), childKeys(local.sp))
        versionsEqual()

        // Delete noteR2 in local program
        local.delete(noteR2.getNodeKey)
        assertEquals(keys(noteR1, noteR2), childKeys(remote.sp))
        assertEquals(keys(noteR1), childKeys(local.sp))

        commit()

        // Gone in the remote program as well.
        assertEquals(keys(noteR1), childKeys(remote.sp))
    }
  }

  @Test def deletedNodesNoConflict() {
    withPiTestEnv {
      env =>
        import env._

        val noteR1 = remote.addNote("Remote Note 1")
        val noteR2 = remote.addNote("Remote Note 2")
        update(user)

        // We have them both now
        assertEquals(keys(noteR1, noteR2), childKeys(local.sp))
        versionsEqual()

        // Delete noteR1 in remote program
        remote.delete(noteR1.getNodeKey)
        assertEquals(keys(noteR2), childKeys(remote.sp))

        // Delete noteR2 in local program
        local.delete(noteR2.getNodeKey)
        assertEquals(keys(noteR1), childKeys(local.sp))

        cantCommit(NeedsUpdate)
        update(user)

        // Both gone in the local program, remote still has noteR2
        assertEquals(Nil, childKeys(local.sp))
        assertEquals(keys(noteR2), childKeys(remote.sp))

        // no conflict here
        assertFalse(local.sp.hasConflicts)

        // Commit to update remote so that it has no children
        commit()
        assertEquals(Nil, childKeys(remote.sp))
        assertFalse(remote.sp.hasConflicts)
    }
  }

  @Test def cannotRemoveModifiedLocalChild() {
    withPiTestEnv {
      env =>
        import env._

        def showState(title: String) {
          println(formatState(title, List(remote, local)))
        }

        val note = remote.addNote("Remote Note")
        showTree(remote.sp)

        update(user)
        showState("PI Updates")

        versionsEqual()

        // Delete note in remote program
        remote.delete(note.getNodeKey)

        showState("Note Deleted Remotely")

        // Modify note in local program
        local.setNoteText("Local Note", note.getNodeKey)
        cantCommit(NeedsUpdate)
        update(user)

        showState("PI Updates Again")

        // Now we have a copy of the note locally with a new key.
        val newNote = local.sp.getObsComponents.get(0)
        assertEquals(keys(newNote), childKeys(local.sp))
        assertEquals(Nil, childKeys(remote.sp))

        assertTrue(local.sp.hasConflicts)
        val List(cn) = local.conflictNotes()
        assertEquals(new ReplacedRemoteDelete(newNote.getNodeKey), cn)

        cantCommit(HasConflict)
        local.sp.resolveConflict(cn)

        // Commit to update remote so that it has the child again
        commit()
        assertEquals(keys(newNote), childKeys(remote.sp))
    }
  }

  @Test def cannotRemoveModifiedLocalChildWithConflict() {
    withPiTestEnv {
      env =>
        import env._

        val note = remote.addNote("Remote Note")
        update(user)
        versionsEqual()

        // Edit the note in the remote program to increase its version number
        remote.setNoteText("Updated Note Text", note.getNodeKey)
        // Now delete note in remote program
        remote.delete(note.getNodeKey)

        // Modify note in local program
        local.setNoteText("Local Note", note.getNodeKey)
        cantCommit(NeedsUpdate)
        update(user)

        // Now we have a copy of the note locally with a new key.
        val newNote = local.sp.getObsComponents.get(0)
        assertEquals(keys(newNote), childKeys(local.sp))
        assertEquals(Nil, childKeys(remote.sp))

        val List(cn) = local.conflictNotes()
        assertEquals(new ReplacedRemoteDelete(newNote.getNodeKey), cn)

        cantCommit(HasConflict)
        local.sp.resolveConflict(cn)

        // Commit to update remote so that it has the child again
        commit()
        assertEquals(keys(newNote), childKeys(remote.sp))
    }
  }

  @Test def cannotRemoveModifiedRemoteChild() {
    withPiTestEnv {
      env =>
        import env._

        val note = remote.addNote("Remote Note")
        update(user)
        versionsEqual()

        // Delete note in local program
        local.delete(note.getNodeKey)
        assertEquals(Nil, childKeys(local.sp))

        // Modify note in remote program
        remote.setNoteText("Remote Note Modified", note.getNodeKey)

        cantCommit(NeedsUpdate)
        update(user)

        // Note comes back locally
        assertEquals(keys(note), childKeys(local.sp))

        val List(cn) = local.conflictNotes()
        assertEquals(new ResurrectedLocalDelete(note.getNodeKey), cn)

        cantCommit(HasConflict)
        local.sp.resolveConflict(cn)

        commit()
        assertEquals(keys(note), childKeys(remote.sp))
    }
  }

  @Test def cannotRemoveNodeWithModifiedLocalChild() {
    withPiTestEnv {
      env =>
        import env._

        val obs = remote.addObservation()
        val note = remote.addNote("Remote Note", obs.getNodeKey)
        update(user)
        versionsEqual()

        // Delete obs in remote program
        remote.delete(obs.getNodeKey)
        assertEquals(Nil, childKeys(remote.sp))

        // Modify note in local program
        local.setNoteText("Local Note", note.getNodeKey)

        cantCommit(NeedsUpdate)
        update(user)

        // Now I have a copy of the observation
        val newObs = local.sp.getObservations.get(0)
        assertEquals(keys(newObs), childKeys(local.sp))
        val newNote = newObs.getObsComponents.get(0)
        assertTrue(childKeys(newObs).contains(newNote.getNodeKey))

        val List(cn) = local.conflictNotes()
        assertEquals(new ReplacedRemoteDelete(newObs.getNodeKey), cn)

        cantCommit(HasConflict)
        local.sp.resolveConflict(cn)

        // After commit, the observation comes back in the remote program too
        commit()
        assertEquals(keys(newObs), childKeys(remote.sp))
        assertTrue(childKeys(remote.find(newObs.getNodeKey)).contains(newNote.getNodeKey))
    }
  }

  @Test def cannotRemoveNodeWithModifiedRemoteChild() {
    withPiTestEnv {
      env =>
        import env._

        val obs = remote.addObservation()
        val note = remote.addNote("Remote Note", obs.getNodeKey)
        update(user)
        versionsEqual()

        // Delete obs in local program
        local.delete(obs.getNodeKey)
        assertEquals(Nil, childKeys(local.sp))

        // Modify note in remote program
        remote.setNoteText("Remote Note Modified", note.getNodeKey)

        cantCommit(NeedsUpdate)
        update(user)

        // Observation comes back in the local program
        assertEquals(keys(obs), childKeys(local.sp))
        assertTrue(childKeys(local.find(obs.getNodeKey)).contains(note.getNodeKey))

        val List(cn) = local.conflictNotes()
        assertEquals(new ResurrectedLocalDelete(obs.getNodeKey), cn)

        cantCommit(HasConflict)
        local.sp.resolveConflict(cn)

        // After commit,the observation is still in the remote program
        commit()
        assertEquals(keys(obs), childKeys(remote.sp))
        assertTrue(childKeys(remote.find(obs.getNodeKey)).contains(note.getNodeKey))
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
    withPiTestEnv { env =>
      import env._


      val obs1 = mkNamedObs(remote, "obs1")
      val obs2 = mkNamedObs(remote, "obs2")
      update(user)
      versionsEqual()

      val obs3 = mkNamedObs(remote, "obs3", 1)
      val obs4 = mkNamedObs(local, "obs4", 1)
      val obs5 = mkNamedObs(local, "obs5", 2)

      assertEquals(keys(obs1, obs3, obs2), childKeys(remote.sp))
      assertEquals(keys(obs1, obs4, obs5, obs2), childKeys(local.sp))

      // In the remote program "obs3" is the 3rd observation whereas in the
      // local program "obs4" is the 3rd observation and "obs5" is the 4th.
      obsNumbersEqual(remote, obs1 -> 1, obs3 -> 3, obs2 -> 2)
      obsNumbersEqual(local, obs1 -> 1, obs4 -> 3, obs5 -> 4, obs2 -> 2)

      update(user)
      assertEquals(keys(obs1, obs4, obs5, obs3, obs2), childKeys(local.sp))

      // After the update, there would have been two observations with number
      // 3 so they had to be renumbered. In particular, "obs3" from the
      // remote program stays as number 3 but in the local program we renumbered
      // "obs4" to become obs #4 and "obs5" to become obs #5
      obsNumbersEqual(local, obs1 -> 1, obs4 -> 4, obs5 -> 5, obs3 -> 3, obs2 -> 2)

      commit()
      assertEquals(keys(obs1, obs4, obs5, obs3, obs2), childKeys(remote.sp))

      // After the commit in the local program, we have two new observations
      // which keep their numbers because they are unique in the remote
      // program.
      obsNumbersEqual(remote, obs1 -> 1, obs4 -> 4, obs5 -> 5, obs3 -> 3, obs2 -> 2)
    }
  }

  @Test def dontRenumberWhenNotNecessary() {
    withPiTestEnv { env =>
      import env._

      update(user)

      val obs1 = local.addObservation()
      val obs2 = local.addObservation()
      obsNumbersEqual(local, obs1 -> 1, obs2 -> 2)

      // remove observation 1 but that shouldn't cause 2 to be renumbered
      local.sp.removeObservation(obs1)
      obsNumbersEqual(local, obs2 -> 2)

      update(user)
      commit()

      assertEquals(1, local.sp.getObservations.size())
      assertEquals(1, remote.sp.getObservations.size())
      obsNumbersEqual(local, obs2 -> 2)
      obsNumbersEqual(remote, obs2 -> 2)

      // next observation starts at 3
      val obs3R = remote.addObservation()
      val obs3L = local.addObservation()

      obsNumbersEqual(remote, obs2 -> 2, obs3R -> 3)
      obsNumbersEqual(local, obs2 -> 2, obs3L -> 3)

      update(user)

      // have to renumber here to avoid having two observation #3
      obsNumbersEqual(local, obs2 -> 2, obs3L -> 4, obs3R -> 3)

      commit()

      obsNumbersEqual(remote, obs2 -> 2, obs3L -> 4, obs3R -> 3)
    }
  }

  @Test def reuseObservationNumbersWhenPossible() {
    withPiTestEnv { env =>
      import env._

      update(user)

      (1 to 9).foreach { _ =>
        val obs = remote.addObservation()
        remote.sp.removeObservation(obs)
      }
      val obs10 = mkNamedObs(remote, "obs10")

      (1 to 8).foreach { _ =>
        val obs = local.addObservation()
        local.sp.removeObservation(obs)
      }

      // 9 must become 11 after update
      val obs9_11 = mkNamedObs(local, "obs9-11")

      // 10 and 11 "al agua"
      local.sp.removeObservation(local.addObservation())
      local.sp.removeObservation(local.addObservation())

      // 12, 13 can stay
      val obs12 = mkNamedObs(local, "obs12")
      val obs13 = mkNamedObs(local, "obs13")

      update(user)

      obsNumbersEqual(local, obs9_11 -> 11, obs12 -> 12, obs13 -> 13, obs10 -> 10)
      commit()
    }
  }

  @Test def bumpObservationNumbersWhenNecessary() {
    withPiTestEnv { env =>
      import env._

      update(user)

      (1 to 9).foreach { _ =>
        val obs = remote.addObservation()
        remote.sp.removeObservation(obs)
      }
      val obs10 = mkNamedObs(remote, "obs10")

      (1 to 8).foreach { _ =>
        val obs = local.addObservation()
        local.sp.removeObservation(obs)
      }

      // 9 must become 11 after update
      val obs9_11 = mkNamedObs(local, "obs9-11")

      // 10 "al agua"
      local.sp.removeObservation(local.addObservation())

      // 11 bumped to 12, 12 bumped to 13
      val obs11_12 = mkNamedObs(local, "obs11-12")
      val obs12_13 = mkNamedObs(local, "obs12-13")

      update(user)

      obsNumbersEqual(local, obs9_11 -> 11, obs11_12 -> 12, obs12_13 -> 13, obs10 -> 10)
      commit()
    }
  }

  @Test def move() {
    withPiTestEnv {
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

        val grpOrigR = mkGroup(remote, "orig")
        val note1 = remote.addNote("1", grpOrigR.getNodeKey)
        val note2 = remote.addNote("2", grpOrigR.getNodeKey)
        val note3 = remote.addNote("3", grpOrigR.getNodeKey)
        update(user)
        versionsEqual()


        val grpR = mkGroup(remote, "groupR")
        val grpL = mkGroup(local, "groupL")

        // move note3 to grpR in remote
        remote.move(note3.getNodeKey, grpR.getNodeKey)

        // move notes 2 and 3 to grpL in local
        local.move(note3.getNodeKey, grpL.getNodeKey)
        local.move(note2.getNodeKey, grpL.getNodeKey)

        // update and see all three groups in the local program
        update(user)
        assertContainsChildren(local, local.sp, grpOrigR, grpR, grpL)

        // grpOrig should have note 1
        assertContainsChildren(local, grpOrigR, note1)

        // grpL should have note2 and a move conflict for note3
        assertContainsChildren(local, grpL, note2)
        val c = local.find(grpL.getNodeKey).getConflicts
        c.notes.toList.asScala.toList match {
          case List(mvConflict) =>
            assertEquals(new Conflict.Moved(note3.getNodeKey, grpR.getNodeKey), mvConflict)
          case _ => fail()
        }

        // grpR should have note3
        assertContainsChildren(local, grpR, note3)

        // acknowledge the conflict and commit
        local.find(grpL.getNodeKey).resolveConflicts()
        commit()
    }
  }

  @Test def conflictingMove() {
    withPiTestEnv {
      env =>
        import env._

        def mkGroup(pc: ProgContext, name: String): ISPGroup = {
          val grp = pc.addGroup()
          pc.setTitle(name, grp.getNodeKey)
          grp
        }

        val grp1R = mkGroup(remote, "grp1")
        val grp2R = mkGroup(remote, "grp2")
        val grp3R = mkGroup(remote, "grp3")
        val obsR = remote.addObservation(grp1R.getNodeKey)

        update(user)
        versionsEqual()

        // Move obs to grp2 remotely
        grp1R.children = Nil
        grp2R.children = List(obsR)

        // Move obs to grp3 locally
        val grp1L = local.find(grp1R.getNodeKey)
        val grp2L = local.find(grp2R.getNodeKey)
        val grp3L = local.find(grp3R.getNodeKey)
        val obsL = local.find(obsR.getNodeKey)

        grp1L.children = Nil
        grp3L.children = List(obsL)

        // Update should move the obs to grp2 locally to match the remote program
        update(user)

        def assertMovedToGroup2() {
          def format(pc: ProgContext, key: SPNodeKey): String =
            "%s - %s".format(pc.name, pc.getTitle(key))

          List((remote, grp2R), (local, grp2L)) foreach {
            case (pc, grp) =>
              val key = grp.getNodeKey
              assertEquals(format(pc, key), keys(obsR), childKeys(pc.find(key)))
          }

          List((remote, grp1R), (local, grp1L), (remote, grp3R), (local, grp3L)) foreach {
            case (pc, grp) =>
              val key = grp.getNodeKey
              assertEquals(format(pc, key), Nil, childKeys(pc.find(key)))
          }
        }
        assertMovedToGroup2()

        // We expect grp3 to have a MOVED conflict.
        val g3 = local.find(grp3L.getNodeKey)
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
    withPiTestEnv {
      env =>
        import env._

        val note = remote.addNote("Remote Note")
        update(user)

        // Delete note in local program, modify note in remote program
        local.delete(note.getNodeKey)
        remote.setNoteText("Remote Note Modified", note.getNodeKey)
        update(user)

        // Note comes back locally
        assertEquals(keys(note), childKeys(local.sp))

        val expect = new ResurrectedLocalDelete(note.getNodeKey)
        val List(actual0) = local.conflictNotes()
        assertEquals(expect, actual0)
        cantCommit(HasConflict)

        // Update again and we still have the conflict.
        update(user)
        val List(actual1) = local.conflictNotes()
        assertEquals(expect, actual1)
        cantCommit(HasConflict)

        // Resolve and commit.
        local.sp.resolveConflict(actual1)
        commit()
        assertEquals(keys(note), childKeys(remote.sp))
    }
  }

  @Test def updateWithExistingDataObjectConflict() {
    withPiTestEnv {
      env =>
        import env._

        remote.setTitle("New Title (Remote)")
        local.setTitle("New Title (Local)")
        cantCommit(NeedsUpdate)
        update(user)

        assertTrue(local.sp.hasConflicts)

        import DataObjectConflict.Perspective._

        def verifyDoc(conflictTitle: String, p: DataObjectConflict.Perspective, dObjTitle: String) {
          val actual = local.sp.getConflicts.dataObjectConflict.getValue
          assertEquals(conflictTitle, actual.dataObject.asInstanceOf[SPProgram].getTitle)
          assertEquals(p, actual.perspective)
          assertEquals(dObjTitle, local.sp.getDataObject.asInstanceOf[SPProgram].getTitle)
        }

        verifyDoc("New Title (Local)", LOCAL, "New Title (Remote)")
        cantCommit(HasConflict)

        // Change the remote title again and update.
        remote.setTitle("Updated Remote Title")
        update(user)

        assertTrue(local.sp.hasConflicts)

        // Here we lose the "New Title (Local)".  It's been replaced with the
        // previous update from the remote database.
        verifyDoc("New Title (Remote)", LOCAL, "Updated Remote Title")

        // Now toggle the data object conflict.
        local.sp.swapDataObjectConflict()
        verifyDoc("Updated Remote Title", REMOTE, "New Title (Remote)")

        // Edit it remotely again and update.
        remote.setTitle("Final Remote Title")
        update(user)
        verifyDoc("New Title (Remote)", LOCAL, "Final Remote Title")

        local.sp.resolveDataObjectConflict()
        commit()
    }
  }

  @Test def testMergeValidityIssue() {
    withPiTestEnv {
      env =>
        import env._

        // ! creates and adds and observation, modifying the remote program
        val key = remote.addObservation().getNodeKey

        update(user)

        // Add a new instrument to both the remote and the local observations
        val remoteObs = remote.sp.getObservations.get(0)
        remoteObs.addObsComponent(remote.odb.getFactory.createObsComponent(remote.sp, SPComponentType.INSTRUMENT_GMOS, null))
        val localObs = local.sp.getObservations.get(0)
        localObs.addObsComponent(local.odb.getFactory.createObsComponent(local.sp, SPComponentType.INSTRUMENT_NIRI, null))

        // Update, which should put one of these instruments in a conflict folder
        update(user)
        val cf = local.findObs(key).getConflictFolder
        assertNotNull(cf)

        val instComp = cf.children(0)
        instComp.getConflicts.notes.get(0) match {
          case _: ConstraintViolation => // okay
          case _ => fail()
        }
        assertTrue(instComp.asInstanceOf[ISPObsComponent].getType.broadType == SPComponentBroadType.INSTRUMENT)

        // remove the conflict generated by having two instruments
        local.findObs(key).removeConflictFolder()

        // TODO: spurious conflict in target component ... have to get rid of this
        local.sp.children(0).children find {
          _ match {
            case oc: ISPObsComponent if oc.getType == SPComponentType.TELESCOPE_TARGETENV => true
            case _ => false
          }
        } foreach {
          _.resolveConflicts()
        }

        commit()
    }
  }

  @Test def newTemplateFolder() {
    withPiTestEnv {
      env =>
        import env._

        val tf = local.odb.getFactory.createTemplateFolder(local.sp, null)
        local.sp.setTemplateFolder(tf)
        commit()

        assertNotNull(local.sp.getTemplateFolder)

        update(user)

        assertNotNull(local.sp.getTemplateFolder)

        assertNotNull(remote.sp.getTemplateFolder)
        assertEquals(local.sp.getTemplateFolder.getNodeKey, remote.sp.getTemplateFolder.getNodeKey)
    }
  }
}