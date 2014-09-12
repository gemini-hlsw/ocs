package edu.gemini.sp.vcs

import edu.gemini.spModel.obs.{ObsPhase2Status, ObservationStatus, SPObservation}

import org.junit.Test
import org.junit.Assert._

class TestOCSINF_32 {
  import TestingEnvironment._

  /*
  BUG description.  The problem here was the resurrection of the deleted
  observation in the PI's local database.  This was incorrectly treated as an
  attempt to create a new READY observation.  The solution was to stop allowing
  observations (or any node) to be revived after a deletion and successful
  commit.  Instead a new copy of the node is made with new keys.

  PIs can change observation status if stored at the right (wrong?) time:
  PI and Staff checkout and Open GN-2013A-Q-10
  Both are in sync with TEST observations at Phase 2.
  PI deletes TEST group and Syncs
  Staff sets TEST observations Ready and Syncs
  Staff gets conflict and keeps the test group and Syncs again
  PI Syncs and nothing happens (!)
  PI Syncs again and accepts changes in remote DB
  TEST group is now at Phase-2 (!)
  PI Syncs
  Staff Syncs and the TEST group goes back to Phase-2
  */

  @Test def groupObsStatusMix() {
    withPiTestEnv { env =>
      import env.{local => pi, _}

      useContext("staff") { staff =>

        // Make a group containing an observation
        val grp = remote.addGroup()
        val obs = remote.addObservation(grp.getNodeKey)

        //      println("Initial Program")
        //      showTree(remote.sp)

        def show(title: String) {
          // showState(title, List(remote, pi, staff))
        }

        // PI and Staff checkout and Open GN-2013A-Q-10
        // Both are in sync with TEST observations at Phase 2.
        pi.vcs.update(id, user)
        doAsStaff { user =>
          staff.vcs.update(id, user)
        }
        show("Initial State")

        // PI deletes TEST group and Syncs
        pi.delete(grp.getNodeKey)
        pi.vcs.commit(id)
        versionsEqual()
        show("PI deletes TEST group and Syncs")

        // Staff sets TEST observations Ready and Syncs
        val newObsKey = doAsStaff { user =>
          val staffObs = staff.findObs(obs.getNodeKey)
          val spObs = staffObs.getDataObject.asInstanceOf[SPObservation]
          spObs.setPhase2Status(ObsPhase2Status.PHASE_2_COMPLETE)
          staffObs.setDataObject(spObs)
          staff.vcs.update(id, user)

          // Staff gets conflict and keeps the test group
          assertTrue(staff.sp.hasConflicts)
          staff.sp.resolveConflicts()

          // The observation was successfully deleted so a copy was made with
          // new keys.
          val newObs = staff.sp.getGroups.get(0).getObservations.get(0)
          assertNotSame(newObs.getNodeKey, staffObs.getNodeKey)

          staff.vcs.commit(id)
          newObs.getNodeKey
        }
        show("Staff sets TEST observations Ready and Syncs")

        // PI Syncs and gets the new READY observation, has no pending commits
        println(pi.vcs.update(id, user))
        assertEquals(ObservationStatus.READY, ObservationStatus.computeFor(pi.findObs(newObsKey)))
        show("PI updates")
      }
    }
  }

}
