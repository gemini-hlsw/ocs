package edu.gemini.sp.vcs.diff


import edu.gemini.sp.vcs.diff.ProgramLocation.Remote
import edu.gemini.spModel.template.{TemplateGroup, TemplateFolder}
import edu.gemini.spModel.util.VersionToken

import scalaz._
import Scalaz._

class TemplateNumberCorrectionSpec extends MergeCorrectionSpec {
  def vt1(segs: Int*): VersionToken = vt(segs: _*)(1)

  def vt(segs: Int*)(next: Int): VersionToken =
    VersionToken.apply(segs.toArray, next)

  def test(expected: List[VersionToken], merged: (VersionToken, Set[ProgramLocation])*): Boolean = {
    val tgList = merged.map { case (tok,_) => templateGroup(tok).leaf }

    val mergeTree = prog.node(Tree.node(templateFolder, tgList.toStream))

    val known = merged.unzip._2.zip(tgList.map(_.key)).collect {
      case (locs, key) if locs.contains(Remote) => key
    }.toSet

    def tgNumbers(t: Tree[MergeNode]): List[VersionToken] = {
      val tf = t.subForest.find(_.rootLabel match {
        case Modified(_, _, _: TemplateFolder, _, _) => true
        case _                                       => false
      })

      tf.toList.flatMap { _.subForest.toList.map { _.rootLabel } }.collect {
        case Modified(_, _, tg: TemplateGroup, _, _) => tg.getVersionToken
      }
    }

    val plan = MergePlan(mergeTree, Set.empty)
    val tnc  = new TemplateNumberingCorrection(lifespanId, Map.empty, known.contains)
    tnc(plan).map(mp => tgNumbers(mp.update)) shouldEqual \/-(expected)
  }

  "TemplateNumberingCorrection" should {
    "handle the no template group case without exception" in {
      test(Nil)
    }

    "not change remote only template group numbers" in {
      test(List(vt1(1), vt1(2), vt1(3)),
        (vt1(1), RemoteOnly),
        (vt1(2), RemoteOnly),
        (vt1(3), RemoteOnly)
      )
    }

    "not renumber new local-only groups if there are no remote groups" in {
      test(List(vt1(1), vt1(2)),
        (vt1(1), LocalOnly),
        (vt1(2), LocalOnly)
      )
    }

    "not renumber new local-only groups if they come after the last remote group" in {
      test(List(vt1(2), vt1(1), vt1(3)),
        (vt1(2), LocalOnly),
        (vt1(1), Both),
        (vt1(3), LocalOnly)
      )
    }

    "renumber a local group with the same number as a remote group" in {
      test(List(vt1(2), vt1(1)),
        (vt1(1), LocalOnly),
        (vt1(1), RemoteOnly)
      )
    }

    "renumber sub-groups with the same number as remote groups" in {
      test(List(vt1(1,2), vt1(1,1), vt(1)(3)),
        (vt1(1, 1), LocalOnly),
        (vt1(1, 1), RemoteOnly),
        (vt(1)(2),  Both)
      )
    }

    // Here 1, 1.1, and 1.2 clash with the remote group 1 so we renumber them
    // to 2, 2.1, and 2.2.
    "renumber all derived local groups with the same base number as a remote group" in {
      test(List(vt(2)(3), vt1(2, 1), vt1(2, 2), vt1(1)),
        (vt(1)(3),  LocalOnly),
        (vt1(1, 1), LocalOnly),
        (vt1(1, 2), LocalOnly),
        (vt1(1),    RemoteOnly)
      )
    }

    // VersionTokenUtilTest already tests the underlying renumbering algorithm.
    // See that test case for more involved examples.
  }
}
