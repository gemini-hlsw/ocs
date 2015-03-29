package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.sp.vcs.diff.VcsFailure.Unmergeable
import org.specs2.matcher.MatchResult

import scalaz._
import Scalaz._

class ValidityCorrectionSpec extends MergeCorrectionSpec {
  val validityCorrection = new ValidityCorrection(lifespanId, Map.empty)

  private def test(start: Tree[MergeNode], expected: Tree[MergeNode], vc: ValidityCorrection = validityCorrection): MatchResult[Tree[MergeNode]] =
    vc.apply(plan(start)) match {
      case -\/(Unmergeable(msg)) => failure(msg)
      case \/-(mp)               => mp.update must correspondTo(expected)
    }

  "MergeValidityCorrection" should {
    "not modify an already valid program" in {
      val start = prog.node(obsTree(1))
      test(start, start)
    }

    "move validity constraint violations into a conflict folder" in {
      val p   = prog
      val tf1 = templateFolder
      val tf2 = templateFolder

      val start    = p.node(tf1.leaf, tf2.leaf)
      val expected = incr(incr(p)).node(
        incr(incr(conflictFolder)).node(tf2.leaf),
        tf1.leaf
      )

      test(start, expected)
    }

    "take into account unmodified node types" in {
      val odb = DBLocalDatabase.createTransient()
      try {
        // Create a template folder node, put it in the node map, and make a
        // corresponding Unmodified merge node.
        val key = new SPNodeKey()
        val pn  = odb.getFactory.createProgram(null, null)
        val tfn = odb.getFactory.createTemplateFolder(pn, key)
        val nodeMap = Map(key -> tfn)
        val un  = MergeNode.unmodified(tfn)

        val tf2 = templateFolder
        val p   = prog

        val start    = p.node(un.leaf, tf2.leaf)
        val expected = incr(incr(p)).node(
          incr(incr(conflictFolder)).node(tf2.leaf),
          un.leaf
        )

        test(start, expected, new ValidityCorrection(lifespanId, nodeMap))
      } finally {
        odb.getDBAdmin.shutdown()
      }
    }

    "handle multiple validity constraint violations" in {
      val p   = prog
      val tf1 = templateFolder
      val tf2 = templateFolder
      val tf3 = templateFolder

      val start    = p.node(tf1.leaf, tf2.leaf, tf3.leaf)
      val expected = incr(incr(incr(p))).node(
        incr(incr(incr(conflictFolder))).node(tf2.leaf, tf3.leaf),
        tf1.leaf
      )

      test(start, expected)
    }

    "expand an unmodified conflict folder if necessary" in {
      val odb = DBLocalDatabase.createTransient()
      try {
        // Create a conflict folder node, put it in the node map, and make a
        // corresponding Unmodified merge node.
        val key = new SPNodeKey()
        val pn  = odb.getFactory.createProgram(null, null)
        val cfn = odb.getFactory.createConflictFolder(pn, key)
        val nodeMap = Map(key -> cfn)
        val un  = MergeNode.unmodified(cfn)

        val p   = prog
        val tf1 = templateFolder
        val tf2 = templateFolder

        val start    = p.node(un.leaf, tf1.leaf, tf2.leaf)
        val expected = incr(p).node(
          incr(MergeNode.modified(cfn)).node(tf2.leaf),
          tf1.leaf
        )

        test(start, expected, new ValidityCorrection(lifespanId, nodeMap))
      } finally {
        odb.getDBAdmin.shutdown()
      }
    }
  }
}
