package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{SPObservationID, SPNodeKey}
import edu.gemini.pot.sp.version.EmptyNodeVersions
import edu.gemini.sp.vcs.diff.ProgramLocation.{Remote, Local}
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.event.SlewEvent
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.obslog.ObsExecLog
import org.specs2.mutable._

import scalaz._
import Scalaz._

class ObsNumberCorrectionSpec extends Specification {
  val LocalOnly: Set[ProgramLocation]  = Set(Local)
  val RemoteOnly: Set[ProgramLocation] = Set(Remote)
  val Both: Set[ProgramLocation]       = Set(Local, Remote)


  import NodeDetail._

  def mergeNode(dob: ISPDataObject, obsNum: Option[Int]): MergeNode = Modified(
    new SPNodeKey(),
    EmptyNodeVersions,
    dob,
    obsNum.fold(Empty: NodeDetail) { Obs.apply }
  )

  def nonObs(dob: ISPDataObject): MergeNode = mergeNode(dob, None)

  def obs(num: Int): MergeNode = mergeNode(new SPObservation, Some(num))

  def doTest(expected: List[Int], merged: (Int, Set[ProgramLocation])*): Boolean = {
    val obsList = merged.map { case (i,_) => obs(i).leaf }

    val mergeTree =
      Tree.node(nonObs(new SPProgram), obsList.toStream)

    val known = merged.unzip._2.zip(obsList.map(_.key)).map { case (locs, key) =>
      locs.map(loc => (loc, key))
    }.toSet.flatten

    def obsNumbers(t: Tree[MergeNode]): List[Int] =
      t.subForest.map(_.rootLabel match {
        case Modified(_, _, _, Obs(i)) => i
        case _                         => -1
      }).toList

    val plan = MergePlan(mergeTree, Set.empty)
    val onc  = new ObsNumberCorrection(Function.untupled(known.contains))

    onc(plan).map(mp => obsNumbers(mp.update)) shouldEqual \/-(expected)
  }

  "ObsRenumber" should {

    "handle the no-observation case without exception" in {
      doTest(Nil)
    }

    "not change remote only observation numbers" in {
      doTest(List(3, 2, 1),
        (3, RemoteOnly),
        (2, RemoteOnly),
        (1, RemoteOnly)
      )
    }

    "not renumber observations known to both sides" in {
      doTest(List(1, 2),
        (1, Both),
        (2, Both)
      )
    }

    "not renumber new local-only observations if there are no remote observations" in {
      doTest(List(1, 2),
        (1, LocalOnly),
        (2, LocalOnly)
      )
    }

    "not renumber new local-only observations if they come after the last remote observation" in {
      doTest(List(2, 1, 3),
        (2, LocalOnly),
        (1, Both),
        (3, LocalOnly)
      )
    }

    "renumber a local observation with the same number as a remote observation" in {
      doTest(List(2, 1),
        (1, LocalOnly),
        (1, RemoteOnly)
      )
    }

    "renumber new local observations with numbers that come before remote observations" in {
      doTest(List(5, 2, 6, 4),
        (1, LocalOnly),
        (2, RemoteOnly),
        (3, LocalOnly),
        (4, RemoteOnly)
      )
    }

    "respect the original local-only numbering sort when renumbering" in {
      doTest(List(6, 2, 5, 4),
        (3, LocalOnly),
        (2, RemoteOnly),
        (1, LocalOnly),
        (4, RemoteOnly)
      )
    }

    "respect the original local-only numbering if there is no need to renumber" in {
      doTest(List(1, 5, 6),
        (1, RemoteOnly),
        (5, LocalOnly),
        (6, LocalOnly)
      )
    }

    "renumber local-only observations sequentially if we must renumber" in {
      doTest(List(1, 2, 3, 4),
        (1, RemoteOnly),
        (1, LocalOnly),
        (5, LocalOnly),
        (6, LocalOnly)
      )
    }

    "reject a renumbering if the observation is executed" in {
      val log = new ObsExecLog() <| (_.getRecord.addEvent(new SlewEvent(1, new SPObservationID("GS-2015A-Q-1-1")), null))

      val localObs  = obs(1).node(nonObs(log).leaf)
      val remoteObs = obs(1).leaf

      val mergeTree = nonObs(new SPProgram).node(localObs, remoteObs)
      val plan      = MergePlan(mergeTree, Set.empty)

      val known = Set[(ProgramLocation, SPNodeKey)](
        (Local,  localObs.key),
        (Remote, remoteObs.key)
      )
      val onc  = new ObsNumberCorrection(Function.untupled(known.contains))

      onc(plan) shouldEqual ObsNumberCorrection.unmergeable(List(1)).left[MergePlan]
    }

  }
}
