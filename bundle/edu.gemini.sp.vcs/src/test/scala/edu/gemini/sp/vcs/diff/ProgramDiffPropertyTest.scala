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

  val props = List[NamedProperty[List[Diff]]] (
    ("any node that differs must appear in diff list",
      (start, local, remote, diffList) => {
        val allKeys      = local.getVersions.keySet ++ remote.getVersions.keySet
        val diffListKeys = diffList.map(_.key).toSet
        val diffKeys     = allKeys.filter { key =>
          local.getVersions(key) =/= remote.getVersions(key)
        }

        (diffKeys &~ diffListKeys).isEmpty
      }
    ),

    ("all ancestors of nodes in the diff list must also appear",
      (start, local, remote, diffList) => {
        val keys = presentKeys(diffList)
        local.forall { n =>
          !keys.contains(n.key) || n.ancestors.forall(a => keys.contains(a.key))
        }
      }
    ),

    ("if a node in an observation appears in the diff list, the entire observation appears",
      (start, local, remote, diffList) => {
        val diffListKeys = presentKeys(diffList)
        val allObsKeys   = local.getAllObservations.asScala.map(o => o.fold(Set.empty[SPNodeKey])(_ + _.key))

        allObsKeys.forall { obsKeys =>
          val sd = obsKeys &~ diffListKeys
          sd.isEmpty || (sd == obsKeys) // all in or all out
        }
      }
    ),

    ("any node that is new in the remote program must generate a Missing diff with empty NodeVersions",
      (start, local, remote, diffList) => {
        val newRemoteKeys    = remote.getVersions.keySet &~ start.getVersions.keySet
        val emptyMissingKeys = diffList.collect {
          case Diff.Missing(k, EmptyNodeVersions) => k
        }.toSet

        newRemoteKeys == emptyMissingKeys
      }
    ),

    ("any node that is deleted in the local program but present in the remote program must generate a Missing diff with defined NodeVersions",
      (start, local, remote, diffList) => {
        val localDeleted     = removedKeys(local)
        val remotePresent    = remote.keySet
        val localDeletedOnly = localDeleted & remotePresent

        val notEmptyMissingKeys = diffList.collect {
          case Diff.Missing(k, nv) if nv =/= EmptyNodeVersions => k
        }.toSet

        (localDeletedOnly &~ notEmptyMissingKeys).isEmpty
      }
    ),

    ("any node that is deleted in the remote program but present in the local program must generate a Present diff",
      (start, local, remote, diffList) => {
        val remoteDeleted     = removedKeys(remote)
        val localPresent      = local.keySet
        val remoteDeletedOnly = remoteDeleted & localPresent

        val localPresentDiffs = presentKeys(diffList)

        (remoteDeletedOnly & localPresentDiffs) == remoteDeletedOnly
      }
    ),

    ("any node that is deleted in both sides, should only generate a Missing diff if NodeVersions differ",
      (start, local, remote, diffList) => {
        val localDeleted  = removedKeys(local)
        val remoteDeleted = removedKeys(remote)
        val bothDeleted   = localDeleted & remoteDeleted

        val (differ, same) = bothDeleted.partition { key =>
          local.getVersions(key) =/= remote.getVersions(key)
        }

        val missingKeys = diffList.collect { case Diff.Missing(k, _) => k }.toSet

        (differ &~ missingKeys).isEmpty && (same & missingKeys).isEmpty
      }
    )
  )

  private def presentKeys(diffList: List[Diff]): Set[SPNodeKey] =
    diffList.collect { case Diff.Present(k, _, _, _, _) =>  k }.toSet

  @Test
  def testAllDiffProperties(): Unit = {
    def mkDiffList(s: ISPProgram, ed0: ISPProgram, ed1: ISPProgram): List[Diff] =
      ProgramDiff.compare(ed0, ed1.getVersions, removedKeys(ed1))

    new MergePropertyTest(mkDiffList).checkAllProperties(props)
  }
}
