package edu.gemini.sp.vcs

import edu.gemini.pot.sp.Conflict.{DeletePermissionFail, CreatePermissionFail, UpdatePermissionFail}
import edu.gemini.pot.sp.{SPComponentType, ISPObservation, ISPSeqComponent}
import edu.gemini.sp.vcs.VcsFailure._
import edu.gemini.spModel.config2.DefaultConfig
import edu.gemini.spModel.dataset._
import edu.gemini.spModel.obs.{ObservationStatus, SPObservation, ObsPhase2Status}
import edu.gemini.spModel.obslog.ObsLog
import edu.gemini.spModel.obslog.ObsLog.UpdateOp
import edu.gemini.spModel.obsrecord.ObsExecStatus
import edu.gemini.spModel.obsrecord.ObsExecStatus._
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.seqcomp.SeqRepeatObserve
import edu.gemini.util.security.permission.ObsMergePermission

import ObsPhase2Status._

import org.junit.Test
import org.junit.Assert._

import scala.collection.JavaConverters._
import edu.gemini.spModel.event.SlewEvent
import edu.gemini.spModel.gemini.flamingos2.Flamingos2


class TestMergeSecurity {

  import TestingEnvironment._

  @Test def testPiUpdatePhase2() {
    piSyncTest { env =>
      import env._

      val obsKey = central.addObservation().getNodeKey
      update(user)

      assertEquals(Nil, MergeSecurity.violations(central.odb, central.sp, cloned.sp, user))

      // local edit of the observation
      cloned.setTitle("x", obsKey)

      // editing a phase 2 observation so this should be okay
      assertEquals(Nil, MergeSecurity.violations(central.odb, central.sp, cloned.sp, user))

      update(user)
      commit()
    }
  }

  @Test def testPiReceiveUpdates() {
    piSyncTest { env =>
      import env._

      // Create the observation remotely and update to have a local copy of
      // the same thing.
      val obsKey = central.addObservation().getNodeKey
      update(user)

      assertEquals(Nil, MergeSecurity.violations(central.odb, central.sp, cloned.sp, user))

      // Now simulate having a staff member make the observation READY.
      central.setPhase2Status(obsKey, ObsPhase2Status.PHASE_2_COMPLETE)
      central.setTitle("x", obsKey)

      // Pi should still be able to update an get the changes even though he
      // cannot edit the observation.
      update(user)

      assertFalse(cloned.sp.hasConflicts)
      assertFalse(cloned.findObs(obsKey).hasConflicts)
      assertEquals(ObsPhase2Status.PHASE_2_COMPLETE, cloned.getPhase2Status(obsKey))

      commit()
    }
  }

  @Test def testPiCreate() {
    piSyncTest { env =>
      import env._

      update(user)

      // PI makes a new observation set to READY
      val obsKey = cloned.addObservation().getNodeKey
      cloned.setPhase2Status(obsKey, PHASE_2_COMPLETE)

      update(user)

      // reset to PHASE2
      assertEquals(PI_TO_COMPLETE, cloned.getPhase2Status(obsKey))

      // with a conflict note
      val obs = cloned.findObs(obsKey)
      val List(conflict) = obs.getConflicts.notes.toList.asScala.toList
      assertEquals(new CreatePermissionFail(obsKey), conflict)

      obs.resolveConflict(conflict)
      commit()
    }
  }

  @Test def testPiUpdateReady() {
    piSyncTest { env =>
      import env._

      val obsKey = central.addObservation().getNodeKey
      update(user)

      // remote status changes to READY
      central.setPhase2Status(obsKey, PHASE_2_COMPLETE)

      // an update now would be okay because the PI hasn't edited the obs
      assertEquals(Nil, MergeSecurity.violations(central.odb, central.sp, cloned.sp, user))

      // local edit of the observation
      cloned.setTitle("x", obsKey)

      // now we can't merge the observations
      val perms = MergeSecurity.violations(central.odb, central.sp, cloned.sp, user)
      val expect = ObsMergePermission(Some(central.findObs(obsKey)), Some(cloned.findObs(obsKey)))
      assertEquals(List(expect), perms)

      // Update will make a copy of the existing observation with the new title
      // in a conflicts folder
      update(user)

      val newObsList = cloned.sp.getAllObservations.asScala.toList
      assertEquals(1, newObsList.size)

      val conflictFolder = cloned.sp.getConflictFolder
      assertEquals(1, conflictFolder.children.size)
      val newObs = conflictFolder.children(0)
      assertTrue(conflictFolder.hasConflicts)
      assertTrue(newObs.hasConflicts)

      val List(conflict) = newObs.getConflicts.notes.toList.asScala.toList
      assertEquals(new UpdatePermissionFail(obsKey), conflict)
      newObs.resolveConflict(conflict)

      assertEquals("x", cloned.getTitle(newObs.getNodeKey))

      // Have to remove the conflict folder itself
      cantCommit(HasConflict)

      conflictFolder.children = Nil
      cloned.sp.removeConflictFolder()

      commit()
    }
  }

  @Test def testPiSetToForReview() {
    piSyncTest { env =>
      import env._

      val obsKey = central.addObservation().getNodeKey
      update(user)

      // edit observation and set the status to NGO_TO_REVIEW
      cloned.setTitle("x", obsKey)
      cloned.setPhase2Status(obsKey, NGO_TO_REVIEW)

      // you can sync this change
      update(user)
      commit()

      // you even edit this since nobody else has changed it
      cloned.setTitle("y", obsKey)
      update(user)
      commit()
    }
  }

  @Test def testPiSetToForReviewNgoEditPiThenCantSync() {
    piSyncTest { env =>
      import env._

      val obsKey = central.addObservation().getNodeKey
      update(user)

      // edit observation and set the status to NGO_TO_REVIEW
      cloned.setTitle("x", obsKey)
      cloned.setPhase2Status(obsKey, NGO_TO_REVIEW)

      // you can sync this change
      update(user)
      commit()

      // Now an NGO can get this version.
      useContext("ngo") { ngo =>

        doAsNgo { user =>
          ngo.vcs.update(id, user)
          ngo.setTitle("ngo", obsKey)
          ngo.vcs.commit(id)
        }

        // Now you can't commit changes without a conflict since you haven't
        // seen the changes from the ngo
        cloned.setTitle("y", obsKey)
        update(user)
        cantCommit(HasConflict)

        assertNotNull(cloned.sp.getConflictFolder)
        cloned.sp.setConflictFolder(null)
        assertEquals("ngo", cloned.getObsField(obsKey, _.getTitle))
        commit()
      }
    }
  }


  private def makeScienceObs(pc: ProgContext): ISPSeqComponent =
    pc.odb.getFactory.createSeqComponent(pc.sp, SeqRepeatObserve.SP_TYPE, null)

  // Make sure that if the PI moves a child node of the observation to
  // elsewhere in the program and then encounters a permissions exception,
  // the node is moved back to the observation and a copy of the child is
  // left in place where it was.
  @Test def testPiUpdateReadyMovedSubNode() {
    piSyncTest { env =>
      import env._

      val obsKey1 = central.addObservation().getNodeKey
      val obsKey2 = central.addObservation().getNodeKey

      val sc1 = central.findObs(obsKey1).getSeqComponent

      val scienceObs1 = makeScienceObs(central)
      sc1.addSeqComponent(scienceObs1)

      update(user)

      // Change remote status to PHASE_2_COMPLETE in obs1
      central.setPhase2Status(obsKey1, PHASE_2_COMPLETE)

      // Move the sequence to obs2 in the local program
      val localObs2 = cloned.findObs(obsKey2)
      localObs2.removeSeqComponent()

      val localSeqRoot1 = cloned.find(sc1.getNodeKey).asInstanceOf[ISPSeqComponent]
      val localObs1 = cloned.findObs(obsKey1)
      localObs1.removeSeqComponent()

      localObs2.setSeqComponent(localSeqRoot1)

      // Now update again.  We will have (a) edited obs1 which will have become
      // PHASE_2_COMPLETE in the remote version (b) moved the sequence root to another
      // observation.

      update(user)

      // Now we should have 3 observations.  obs1 will have been restored
      // as PHASE_2_COMPLETE and will have the sequence node with its observe

      assertEquals(PHASE_2_COMPLETE, cloned.getPhase2Status(obsKey1))
      val o1 = cloned.findObs(obsKey1)
      assertEquals(sc1.getNodeKey, o1.getSeqComponent.getNodeKey)
      assertEquals(scienceObs1.getNodeKey, o1.getSeqComponent.children(0).getNodeKey)

      // obs2 will still have a sequence root and its observe, but with a new
      // key.

      assertEquals(PI_TO_COMPLETE, cloned.getPhase2Status(obsKey2))
      val o2 = cloned.findObs(obsKey2)
      assertNotSame(sc1.getNodeKey, o2.getSeqComponent.getNodeKey)
      assertNotSame(scienceObs1.getNodeKey, o2.getSeqComponent.children(0).getNodeKey)


      // obs3 will have been created as a copy of obs1 as it was before the
      // update (with no sequence component) but reset to PHASE2
      val obsKey3 = cloned.sp.getConflictFolder.children(0).getNodeKey
      assertEquals(PI_TO_COMPLETE, cloned.getPhase2Status(obsKey3))
      val o3 = cloned.findObs(obsKey3)
      assertNull(o3.getSeqComponent)

      // obs3 will be marked with a conflict note
      val List(note) = o3.getConflicts.notes.toList.asScala.toList
      assertEquals(new UpdatePermissionFail(obsKey1), note)
      o3.resolveConflict(note)

      cantCommit(HasConflict)
      cloned.sp.removeConflictFolder()
      commit()
    }
  }

  @Test def testPiDeleteFirstObs() {
    piSyncTest { env =>
      import env._

      val obsKey1 = central.addObservation().getNodeKey
      val obsKey2 = central.addObservation().getNodeKey

      update(user)

      // Make the 1st obs ready remotely
      central.setPhase2Status(obsKey1, PHASE_2_COMPLETE)

      // Delete the 1st obs locally
      cloned.sp.children = cloned.sp.children.tail

      update(user)

      // 1st obs comes back in PHASE_2_COMPLETE status
      assertEquals(PHASE_2_COMPLETE, cloned.getPhase2Status(obsKey1))
      assertEquals(PI_TO_COMPLETE, cloned.getPhase2Status(obsKey2))

      // in order 1, 2
      assertEquals(List(obsKey1, obsKey2), cloned.sp.children.map(_.getNodeKey))

      // 1st obs has a conflict note
      val List(conflict) = cloned.findObs(obsKey1).getConflicts.notes.toList.asScala.toList
      assertEquals(new DeletePermissionFail(obsKey1), conflict)

      cloned.findObs(obsKey1).resolveConflict(conflict)

      commit()
    }
  }

  @Test def testPiDeleteSecondObs() {
    piSyncTest { env =>
      import env._

      val obsKey1 = central.addObservation().getNodeKey
      val obsKey2 = central.addObservation().getNodeKey
      val obsKey3 = central.addObservation().getNodeKey

      update(user)

      // Make the 2st obs ready remotely
      central.setPhase2Status(obsKey2, PHASE_2_COMPLETE)

      // Delete the 2nd obs locally
      cloned.sp.children = cloned.sp.children.filterNot(_.getNodeKey == obsKey2)

      update(user)

      // 2st obs comes back in PHASE_2_COMPLETE status
      assertEquals(PI_TO_COMPLETE, cloned.getPhase2Status(obsKey1))
      assertEquals(PHASE_2_COMPLETE, cloned.getPhase2Status(obsKey2))
      assertEquals(PI_TO_COMPLETE, cloned.getPhase2Status(obsKey3))

      // 2st obs has a conflict note
      val List(conflict) = cloned.findObs(obsKey2).getConflicts.notes.toList.asScala.toList
      assertEquals(new DeletePermissionFail(obsKey2), conflict)

      // in order 1, 2, 3
      assertEquals(List(obsKey1, obsKey2, obsKey3), cloned.sp.children.map(_.getNodeKey))

      cloned.findObs(obsKey2).resolveConflict(conflict)

      commit()
    }
  }

  @Test def testPiDeleteLastObs() {
    piSyncTest { env =>
      import env._

      val obsKey1 = central.addObservation().getNodeKey
      val obsKey2 = central.addObservation().getNodeKey
      val obsKey3 = central.addObservation().getNodeKey

      update(user)

      // Make the 3rd obs ready remotely
      central.setPhase2Status(obsKey3, PHASE_2_COMPLETE)

      // Delete the 2nd obs locally (this should work)
      cloned.sp.children = cloned.sp.children.filterNot(_.getNodeKey == obsKey2)

      // Delete the 3rd obs locally (this one will come back)
      cloned.sp.children = cloned.sp.children.filterNot(_.getNodeKey == obsKey3)

      update(user)

      // 3rd obs comes back in PHASE_2_COMPLETE status
      assertEquals(PI_TO_COMPLETE, cloned.getPhase2Status(obsKey1))
      assertEquals(PHASE_2_COMPLETE, cloned.getPhase2Status(obsKey3))

      // 3rd obs has a conflict note
      val List(conflict) = cloned.findObs(obsKey3).getConflicts.notes.toList.asScala.toList
      assertEquals(new DeletePermissionFail(obsKey3), conflict)

      // in order 1, 3
      assertEquals(List(obsKey1, obsKey3), cloned.sp.children.map(_.getNodeKey))

      cloned.findObs(obsKey3).resolveConflict(conflict)

      commit()
    }
  }

  @Test def testPiDeleteWithDeletedParent() {
    piSyncTest { env =>
      import env._

      val grp = central.addGroup()
      val obsKey = central.addObservation(grp.getNodeKey).getNodeKey

      update(user)

      // Make the obs ready remotely
      central.setPhase2Status(obsKey, PHASE_2_COMPLETE)

      // Delete the whole group locally (which includes its child, the obs)
      cloned.sp.children = Nil

      update(user)

      // Obs comes back in PHASE_2_COMPLETE status
      assertEquals(PHASE_2_COMPLETE, cloned.getPhase2Status(obsKey))

      // Obs is marked with a conflict note
      val List(conflict) = cloned.findObs(obsKey).getConflicts.notes.toList.asScala.toList
      assertEquals(new DeletePermissionFail(obsKey), conflict)
      cloned.findObs(obsKey).resolveConflict(conflict)

      // Group comes back.
      assertNotNull(cloned.find(grp.getNodeKey))
      assertTrue(cloned.findObs(obsKey).getParent == cloned.find(grp.getNodeKey))

      // Group added to program
      assertEquals(List(cloned.find(grp.getNodeKey)), cloned.sp.children)

      commit()
    }
  }


  private def wrap(s: ObsExecStatus): edu.gemini.shared.util.immutable.Some[ObsExecStatus] =
    new edu.gemini.shared.util.immutable.Some(s)

  @Test def notEvenStaffCanEditAnObsThatBecameObserved() {
    staffSyncTest { env =>
      import env._

      // Create an observation remotely and update to get it locally.
      val rObs = central.addObservation()
      val key = rObs.getNodeKey
      update(user)
      versionsEqual()

      // Set the status to Observed remotely.
      central.setPhase2Status(key, PHASE_2_COMPLETE)
      central.setExecStatusOverride(key, wrap(OBSERVED))

      // Edit the observation locally.
      cloned.setObsField(key, _.setTitle("Local Title"))

      update(user)

      // Expect an update conflict because we changed an observed
      // observation.
      cloned.sp.getConflictFolder.getChildren.asScala.toList match {
        case List(o) =>
          assertEquals(PI_TO_COMPLETE, cloned.getPhase2Status(o.getNodeKey))
          assertNotSame(o.getNodeKey, key)
          assertEquals("Local Title", cloned.getObsField(o.getNodeKey, _.getTitle))
          o.getConflicts.notes.toList.asScala.toList match {
            case List(cn: UpdatePermissionFail) => assertEquals(cn.getNodeKey, key)
            case _ => fail()
          }
        case _ => fail()
      }

      // Remove the conflicts
      cloned.sp.setConflictFolder(null)

      commit()
    }
  }

  @Test def staffCanChangeExplicitObservedStatusToOngoing() {
    staffSyncTest { env =>
      import env._

      // Create an observation remotely and update to get it locally.
      val rObs = central.addObservation()
      val key = rObs.getNodeKey
      // Set the status to Observed remotely.
      central.setPhase2Status(key, PHASE_2_COMPLETE)
      central.setExecStatusOverride(key, wrap(OBSERVED))
      update(user)
      versionsEqual()

      // Reset the status to ongoing
      cloned.setExecStatusOverride(key, wrap(ONGOING))

      update(user)
      commit()
    }
  }


  @Test def staffCanSetTheStatusAndEditIfNotModifiedInTheDatabase() {
    staffSyncTest { env =>
      import env._

      // Create an observation remotely and update to get it locally.
      val rObs = central.addObservation()
      val key = rObs.getNodeKey
      update(user)
      versionsEqual()

      // Set the status to Observed local.
      cloned.setPhase2Status(key, PHASE_2_COMPLETE)
      cloned.setExecStatusOverride(key, wrap(OBSERVED))
      cloned.setObsField(key, _.setTitle("Local Title"))

      update(user)
      commit()
    }
  }


  @Test def youCannotSetObservedForAnEditedObservation() {
    staffSyncTest { env =>
      import env._

      // Create an observation remotely and update to get it locally.
      val rObs = central.addObservation()
      val key = rObs.getNodeKey
      update(user)
      versionsEqual()

      // Edit the observation remotely.
      central.setObsField(key, _.setTitle("Remote Title"))

      // Set the status to Observed locally.
      cloned.setPhase2Status(key, PHASE_2_COMPLETE)
      cloned.setExecStatusOverride(key, wrap(OBSERVED))
      cloned.setObsField(key, _.setTitle("Local Title"))

      update(user)

      // Expect an update conflict because we tried to make an edited
      // observation observed.
      cloned.sp.getConflictFolder.getChildren.asScala.toList match {
        case List(o) =>
          assertEquals(PI_TO_COMPLETE, cloned.getPhase2Status(o.getNodeKey))
          assertNotSame(o.getNodeKey, key)
          assertEquals("Local Title", cloned.getObsField(o.getNodeKey, _.getTitle))
          o.getConflicts.notes.toList.asScala.toList match {
            case List(cn: UpdatePermissionFail) => assertEquals(cn.getNodeKey, key)
            case _ => fail()
          }
        case _ => fail()
      }

      // Remove the conflicts
      cloned.sp.setConflictFolder(null)

      commit()
    }
  }

  @Test def youCanChangeStatusIfAllThatHasChangedIsTheObsLog() {
    staffSyncTest { env =>
      import env._

      // Create an observation remotely.
      val rObs = central.addObservation()
      val key = rObs.getNodeKey
      val oid = rObs.getObservationID
      val label1 = new DatasetLabel(oid, 1)
      val label2 = new DatasetLabel(oid, 2)

      // Set the status to Ongoing remotely.
      central.setPhase2Status(key, PHASE_2_COMPLETE)
      central.setExecStatusOverride(key, wrap(ONGOING))

      // Add the obslog
      ObsLog.update(central.odb, oid, new UpdateOp {
        def apply(obs: ISPObservation, log: ObsLog) {
          val dataset = new Dataset(label1, "filename1", 0)
          val record = new DatasetExecRecord(dataset)
          log.getExecRecord.putDatasetExecRecord(record, new DefaultConfig())
          val qa = new DatasetQaRecord(label1, DatasetQaState.USABLE, "comment1")
          log.qaLogDataObject.set(qa)
        }
      })

      // Get the observation in the local program.
      update(user)
      versionsEqual()

      // Edit the observation locally.
      cloned.setObsField(key, _.setTitle("Local Title"))

      // Add a dataset remotely.
      ObsLog.update(central.odb, oid, new UpdateOp {
        def apply(obs: ISPObservation, log: ObsLog) {
          val dataset = new Dataset(label2, "filename2", 0)
          val record = new DatasetExecRecord(dataset)
          log.getExecRecord.putDatasetExecRecord(record, new DefaultConfig())
          val qa = new DatasetQaRecord(label2, DatasetQaState.CHECK, "comment2")
          log.qaLogDataObject.set(qa)
        }
      })

      // Shouldn't cause any conflicts so commit should succeed.
      update(user)
      commit()

      // Local observation is now observed with the new title and the
      // updates to the log.
      val obs = cloned.findObs(key)
      val dob = obs.getDataObject.asInstanceOf[SPObservation]
      assertEquals("Local Title", dob.getTitle)
      assertEquals(PHASE_2_COMPLETE, dob.getPhase2Status)
      assertEquals(wrap(ObsExecStatus.ONGOING), dob.getExecStatusOverride)

      val log = ObsLog.getIfExists(obs)
      assertNotNull(log)
      assertEquals("comment1", log.getQaRecord.comment(label1))
      assertEquals("comment2", log.getQaRecord.comment(label2))
      assertEquals(DatasetQaState.USABLE, log.getQaRecord.qaState(label1))
      assertEquals(DatasetQaState.CHECK, log.getQaRecord.qaState(label2))
      assertEquals("filename2", log.getExecRecord.getDatasetExecRecord(label2).dataset.getDhsFilename)
    }
  }

  // OCSINF-363
  @Test def observerCanTweakReadyObservationExecutingInOdb() {
    staffSyncTest { env =>
      import env._

      // Create an observation remotely.
      val rObs = central.addObservation()
      val key = rObs.getNodeKey
      val oid = rObs.getObservationID

      // Add an instrument.
      val f2 = central.odb.getFactory.createObsComponent(central.sp, SPComponentType.INSTRUMENT_FLAMINGOS2, null)
      rObs.addObsComponent(f2)

      // Add a couple of observes
      val sc = central.odb.getFactory.createSeqComponent(central.sp, SPComponentType.OBSERVER_OBSERVE, null)
      val ob = sc.getDataObject.asInstanceOf[SeqRepeatObserve]
      ob.setStepCount(2)
      sc.setDataObject(ob)
      rObs.getSeqComponent.addSeqComponent(sc)

      // Set the status to Ready.
      central.setPhase2Status(key, PHASE_2_COMPLETE)

      // Get the observation in the local program.
      update(user)
      versionsEqual()

      // Now add the obslog remotely.  This should make it Ongoing remotely.
      ObsLog.update(central.odb, oid, new UpdateOp {
        def apply(obs: ISPObservation, log: ObsLog) {
          log.getExecRecord.addEvent(new SlewEvent(System.currentTimeMillis, oid), null)
        }
      })
      assertEquals(ObservationStatus.ONGOING, ObservationStatus.computeFor(rObs))

      // But still Ready locally.
      assertEquals(ObservationStatus.READY, ObservationStatus.computeFor(cloned.findObs(key)))

      // Nevertheless you can edit this observation if you don't touch the
      // observation node itself.
      val localF2 = cloned.find(f2.getNodeKey)
      val f2Obj = localF2.getDataObject.asInstanceOf[Flamingos2]
      f2Obj.setPosAngle(123.0)
      localF2.setDataObject(f2Obj)

      update(user)
      commit()
    }
  }

  @Test def observerCanTweakTheObservationNodeItselfIfReadyLocallyButExecutingInOdb() {
    staffSyncTest { env =>
      import env._

      // Create an observation remotely.
      val rObs = central.addObservation()
      val key = rObs.getNodeKey
      val oid = rObs.getObservationID

      // Add an instrument.
      val f2 = central.odb.getFactory.createObsComponent(central.sp, SPComponentType.INSTRUMENT_FLAMINGOS2, null)
      rObs.addObsComponent(f2)

      // Add a couple of observes
      val sc = central.odb.getFactory.createSeqComponent(central.sp, SPComponentType.OBSERVER_OBSERVE, null)
      val ob = sc.getDataObject.asInstanceOf[SeqRepeatObserve]
      ob.setStepCount(2)
      sc.setDataObject(ob)
      rObs.getSeqComponent.addSeqComponent(sc)

      // Set the status to Ready.
      central.setPhase2Status(key, PHASE_2_COMPLETE)

      // Get the observation in the local program.
      update(user)
      versionsEqual()

      // Now add the obslog remotely.  This should make it Ongoing remotely.
      ObsLog.update(central.odb, oid, new UpdateOp {
        def apply(obs: ISPObservation, log: ObsLog) {
          log.getExecRecord.addEvent(new SlewEvent(System.currentTimeMillis, oid), null)
        }
      })
      assertEquals(ObservationStatus.ONGOING, ObservationStatus.computeFor(rObs))

      // But still Ready locally.
      assertEquals(ObservationStatus.READY, ObservationStatus.computeFor(cloned.findObs(key)))

      // Until REL-1590, you couldn't touch the observation node itself but
      // now you can ...
      cloned.setObsField(key, _.setPriority(SPObservation.Priority.HIGH))

      update(user)
      commit()
    }
  }

  @Test def piCantEditTheObsLogRegardlessOfObsStatus() {
    piSyncTest { env =>
      import env._

      // Create an observation remotely.
      val rObs = central.addObservation()
      val oid = rObs.getObservationID
      val label = new DatasetLabel(oid, 1)

      // Add the obslog
      ObsLog.update(central.odb, oid, new UpdateOp {
        def apply(obs: ISPObservation, log: ObsLog) {
          val dataset = new Dataset(label, "filename1", 0)
          val record = new DatasetExecRecord(dataset)
          log.getExecRecord.putDatasetExecRecord(record, new DefaultConfig())
          val qa = new DatasetQaRecord(label, DatasetQaState.USABLE, "comment")
          log.qaLogDataObject.set(qa)
        }
      })

      // Get the observation in the local program.
      update(user)
      versionsEqual()

      // Edit a comment
      ObsLog.update(cloned.odb, oid, new UpdateOp {
        def apply(obs: ISPObservation, log: ObsLog) {
          val upd = log.qaLogDataObject.get(label).withComment("comment 2")
          log.qaLogDataObject.set(upd)
        }
      })

      update(user)
      cantCommit(HasConflict)

      cloned.sp.setConflictFolder(null)
      commit()
    }
  }

  // The backend should always allow the user to make an edit to an observation
  // status that they can switch to (PI in "For Review" for example), assuming
  // that they have the newest version of the observation.  This includes
  // deleting nodes, as is the case in this test case.
  @Test def piSetPhase2DeleteThenSetBackToForReview() {
    piSyncTest { env =>
      import env._

      // Create an observation remotely and add an observe.
      val rObs = central.addObservation()
      val key = rObs.getNodeKey
      val rSeq = central.odb.getFactory.createSeqComponent(central.sp, SPComponentType.ITERATOR_OFFSET, null)
      rObs.getSeqComponent.addSeqComponent(rSeq)

      // Update to get the same thing locally.
      update(user)
      versionsEqual()

      // Switch to "For Review" and commit.
      cloned.setPhase2Status(key, NGO_TO_REVIEW)
      commit()
      versionsEqual()

      // Switch back to "Phase 2", delete the iterator and commit.
      cloned.setPhase2Status(key, PI_TO_COMPLETE)
      val lObs = cloned.findObs(key)
      lObs.getSeqComponent.children = Nil
      assertEquals(0, cloned.findObs(key).getSeqComponent.children.size)
      cloned.setPhase2Status(key, NGO_TO_REVIEW)
      commit()
      versionsEqual()

      assertEquals(0, central.findObs(key).getSeqComponent.children.size)
    }
  }

  @Test def ocsinf260() {
    piSyncTest { env =>
      import env._

      // Create an observation remotely and add an offset iterator with an observe.
      val rObs = central.addObservation()
      val key = rObs.getNodeKey
      val rOff = central.odb.getFactory.createSeqComponent(central.sp, SPComponentType.ITERATOR_OFFSET, null)
      rObs.getSeqComponent.addSeqComponent(rOff)
      val rObserve = central.odb.getFactory.createSeqComponent(central.sp, SPComponentType.OBSERVER_OBSERVE, null)
      rOff.addSeqComponent(rObserve)

      // Update to get the same thing locally.
      update(user)
      versionsEqual()

      // Switch to "For Review" and commit.
      cloned.setPhase2Status(key, NGO_TO_REVIEW)
      commit()
      versionsEqual()

      // Switch back to "Phase 2", delete the offset iterator, but move the
      // observe directly under the sequence component.
      cloned.setPhase2Status(key, PI_TO_COMPLETE)
      val lObs = cloned.findObs(key)
      val lOff = lObs.getSeqComponent.children(0).asInstanceOf[ISPSeqComponent]
      val lObserve = lOff.children(0).asInstanceOf[ISPSeqComponent]
      lOff.children = Nil
      lObs.getSeqComponent.children = List(lObserve)

      assertEquals(1, cloned.findObs(key).getSeqComponent.children.size)
      cloned.setPhase2Status(key, NGO_TO_REVIEW)
      commit()
      versionsEqual()

      assertEquals(1, central.findObs(key).getSeqComponent.children.size)
    }
  }
}
