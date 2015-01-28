package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{SPNodeKey, ISPProgram}
import edu.gemini.pot.sp.version._
import edu.gemini.spModel.rich.pot.sp._

import org.junit.Test
import org.scalatest.junit.JUnitSuite

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._


class ProgramDiffPropertyTest extends JUnitSuite {
  import MergePropertyTest.NamedProperty

  val props = List[NamedProperty[MergePlan]] (
    ("any node that differs must appear in diff",
      (start, local, remote, mp) => {
        val allKeys  = local.getVersions.keySet ++ remote.getVersions.keySet
        val diffKeys = allKeys.filter { key =>
          local.getVersions(key) =/= remote.getVersions(key)
        }

        (diffKeys &~ mpKeys(mp)).isEmpty
      }
    ),

    ("all ancestors of nodes in the diff list must also appear",
      (start, local, remote, mp) => {
        val keys = presentKeys(mp)
        remote.forall { n =>
          !keys.contains(n.key) || n.ancestors.forall(a => keys.contains(a.key))
        }
      }
    ),

    ("if a node in an observation appears in the diff list, the entire observation appears",
      (start, local, remote, mp) => {
        val modKeys    = modifiedKeys(mp)
        val allObsKeys = remote.getAllObservations.asScala.map(o => o.fold(Set.empty[SPNodeKey])(_ + _.key))

        allObsKeys.forall { obsKeys =>
          val sd = obsKeys &~ modKeys
          sd.isEmpty || (sd == obsKeys) // all in or all out
        }
      }
    ),

    ("any node that is new in the local program must generate a Missing diff with empty NodeVersions",
      (start, local, remote, mp) => {
        val newLocalKeys     = local.getVersions.keySet &~ start.getVersions.keySet
        val emptyMissingKeys = mp.delete.collect {
          case Missing(k, EmptyNodeVersions) => k
        }.toSet

        newLocalKeys == emptyMissingKeys
      }
    ),

    ("any node that is deleted in the remote program but present in the local program must generate a Missing diff with defined NodeVersions",
      (start, local, remote, mp) => {
        val remoteDeleted     = removedKeys(remote)
        val localPresent      = local.keySet
        val remoteDeletedOnly = remoteDeleted & localPresent

        val notEmptyMissingKeys = mp.delete.collect {
          case Missing(k, nv) if nv =/= EmptyNodeVersions => k
        }.toSet

        (remoteDeletedOnly &~ notEmptyMissingKeys).isEmpty
      }
    ),

    ("any node that is deleted in the local program but present in the remote program must generate a Modified diff",
      (start, local, remote, mp) => {
        val localDeleted     = removedKeys(local)
        val remotePresent    = remote.keySet
        val localDeletedOnly = localDeleted & remotePresent

        val remoteModifiedDiffs = modifiedKeys(mp)

        (localDeletedOnly & remoteModifiedDiffs) == localDeletedOnly
      }
    ),

    ("any node that is deleted in both sides, should only generate a Missing diff if NodeVersions differ",
      (start, local, remote, mp) => {
        val localDeleted  = removedKeys(local)
        val remoteDeleted = removedKeys(remote)
        val bothDeleted   = localDeleted & remoteDeleted

        val (differ, same) = bothDeleted.partition { key =>
          local.getVersions(key) =/= remote.getVersions(key)
        }

        val missingKeys = mp.delete.map(_.key).toSet

        (differ &~ missingKeys).isEmpty && (same & missingKeys).isEmpty
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
    def mkDiffs(s: ISPProgram, l: ISPProgram, r: ISPProgram): MergePlan =
      ProgramDiff.compare(r, l.getVersions, removedKeys(l))

    new MergePropertyTest(mkDiffs).checkAllProperties(props)
  }
}
