package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{SPNodeKey, ISPFactory, ISPProgram}
import edu.gemini.pot.sp.version._
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.spModel.rich.pot.sp._

import org.junit.Test

import org.scalatest.junit.JUnitSuite
import org.scalatest.prop.Checkers
import org.scalacheck.Prop

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

class ProgramTest extends JUnitSuite with Checkers {

  type DiffProperty = (ISPProgram, ISPProgram, ISPProgram, List[Diff]) => Boolean

  val props = List[(String, DiffProperty)] (
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

  private def checkDiffProperty(p: DiffProperty): Unit = {
    val odb  = DBLocalDatabase.createTransient()
    val fact = odb.getFactory

    import ProgramGen._

    // Generate a starting program and two independently edited copies of it.
    val genProgs = for {
      fStart <- genProg
      fEd0   <- genEditedProg
      fEd1   <- genEditedProg
    } yield {(f: ISPFactory) => {
      val start = fStart(f)
      val ed0   = fEd0(f, start)
      val ed1   = fEd1(f, start)
      (start, ed0, ed1)
    }}

    try {
      check(Prop.forAll(genProgs) { fun =>
        val (start, ed0, ed1) = fun(fact)
        val diffs = ProgramDiff.compare(ed0, ed1.getVersions, removedKeys(ed1))
        p(start, ed0, ed1, diffs)
      })
    } finally {
      odb.getDBAdmin.shutdown()
    }
  }

  @Test
  def testAllDiffProperties(): Unit = {
    checkDiffProperty { (start, local, remote, diffs) =>
      val failure = props.find { case (_, p) => !p(start, local, remote, diffs) }

      failure.foreach { case (desc, _) =>
        println("*** Diff property failure: " + desc)

        val titles = List("Start", "Local", "Remote")
        val progs  = List(start,   local,   remote)
        titles.zip(progs).foreach { case (title, prog) =>
          println(s"\n$title")
          println(prog)
        }
      }

      failure.isEmpty
    }
  }
}
