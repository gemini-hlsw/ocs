package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.{ObservationIterator, ISPFactory, SPNodeKey, ISPProgram}
import edu.gemini.pot.sp.version._
import edu.gemini.sp.vcs2.NodeDetail.Obs
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.rich.pot.sp._

import org.junit.Test
import org.scalatest.junit.JUnitSuite

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._


class ProgramDiffTest extends JUnitSuite {
  import MergePropertyTest.NamedProperty

  val props = List[NamedProperty[ProgramDiff]] (
    ("any node that differs must appear in diff",
      (start, local, remote, pd) => {
        val allKeys  = local.getVersions.keySet ++ remote.getVersions.keySet
        val diffKeys = allKeys.filter { key =>
          local.getVersions(key) =/= remote.getVersions(key)
        }

        (diffKeys &~ mpKeys(pd.plan)).isEmpty
      }
    ),

    ("all ancestors of nodes in the diff list must also appear",
      (start, local, remote, pd) => {
        val keys = presentKeys(pd.plan)
        remote.forall { n =>
          !keys.contains(n.key) || n.ancestors.forall(a => keys.contains(a.key))
        }
      }
    ),

    ("if a node in an observation appears in the diff list, the entire observation appears",
      (start, local, remote, pd) => {
        val modKeys    = modifiedKeys(pd.plan)
        val allObsKeys = new ObservationIterator(remote).asScala.map(o => o.fold(Set.empty[SPNodeKey])(_ + _.key))

        allObsKeys.forall { obsKeys =>
          val sd = obsKeys &~ modKeys
          sd.isEmpty || (sd == obsKeys) // all in or all out
        }
      }
    ),

    ("any node that is new in the local program must generate a Missing diff with empty NodeVersions",
      (start, local, remote, pd) => {
        val newLocalKeys     = local.getVersions.keySet &~ start.getVersions.keySet
        val emptyMissingKeys = pd.plan.delete.collect {
          case Missing(k, EmptyNodeVersions) => k
        }

        newLocalKeys == emptyMissingKeys
      }
    ),

    ("any node that is deleted in the remote program but present in the local program must generate a Missing diff with defined NodeVersions",
      (start, local, remote, pd) => {
        val remoteDeleted     = removedKeys(remote)
        val localPresent      = local.keySet
        val remoteDeletedOnly = remoteDeleted & localPresent

        val notEmptyMissingKeys = pd.plan.delete.collect {
          case Missing(k, nv) if nv =/= EmptyNodeVersions => k
        }

        (remoteDeletedOnly &~ notEmptyMissingKeys).isEmpty
      }
    ),

    ("any node that is deleted in the local program but present in the remote program must generate a Modified diff",
      (start, local, remote, pd) => {
        val localDeleted     = removedKeys(local)
        val remotePresent    = remote.keySet
        val localDeletedOnly = localDeleted & remotePresent

        val remoteModifiedDiffs = modifiedKeys(pd.plan)

        (localDeletedOnly & remoteModifiedDiffs) == localDeletedOnly
      }
    ),

    ("any node that is deleted in both sides, should only generate a Missing diff if NodeVersions differ",
      (start, local, remote, pd) => {
        val localDeleted  = removedKeys(local)
        val remoteDeleted = removedKeys(remote)
        val bothDeleted   = localDeleted & remoteDeleted

        val (differ, same) = bothDeleted.partition { key =>
          local.getVersions(key) =/= remote.getVersions(key)
        }

        val missingKeys = pd.plan.delete.map(_.key)

        (differ &~ missingKeys).isEmpty && (same & missingKeys).isEmpty
      }
    ),

    ("any differing observations should appear in the obs status list",
      (start, local, remote, pd) => {
        val obsKeys = pd.plan.update.sFoldRight(Set.empty[SPNodeKey]) { (mn, s) =>
          mn match {
            case Modified(k, _, _, Obs(_), _) => s + k
            case _                            => s
          }
        }

        val statusKeys = pd.obsStatus.unzip._1.toSet

        obsKeys == statusKeys
      }
    ),

    ("obs status list should correspond to remote program",
      (start, local, remote, pd) => {
        val remoteMap = (Map.empty[SPNodeKey, ObservationStatus]/:new ObservationIterator(remote).asScala) { (m,o) =>
          m + (o.key -> ObservationStatus.computeFor(o))
        }

        pd.obsStatus.forall { case (key, status) =>
          remoteMap.get(key).exists(_ == status)
        }
      }
    )
  )

  private def presentKeys(mp: MergePlan): Set[SPNodeKey] =
    mp.update.sFoldRight(Set.empty[SPNodeKey]) { (mn,s) => s + mn.key }

  private def modifiedKeys(mp: MergePlan): Set[SPNodeKey] =
    mp.update.sFoldRight(Set.empty[SPNodeKey]) { (mn,s) =>
      mn match {
        case _: Modified => s + mn.key
        case _           => s
      }
    }

  private def missingKeys(mp: MergePlan): Set[SPNodeKey] =
    mp.delete.map(_.key)

  private def mpKeys(mp: MergePlan): Set[SPNodeKey] = missingKeys(mp) ++ presentKeys(mp)

  @Test
  def testAllDiffProperties(): Unit = {
    def mkDiffs(f: ISPFactory, s: ISPProgram, l: ISPProgram, r: ISPProgram): ProgramDiff =
      ProgramDiff.compare(r, l.getVersions, removedKeys(l))

    new MergePropertyTest(mkDiffs).checkAllProperties(props)
  }
}
