package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{ISPFactory, ISPProgram}
import edu.gemini.pot.spdb.DBLocalDatabase
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import org.scalatest.prop.Checkers
import org.scalacheck.Prop

/**
 *
 */
class ProgramTest extends JUnitSuite with Checkers {

  private def checkDiffProperty(p: (ISPProgram, ISPProgram, ISPProgram, List[Diff]) => Boolean): Unit = {
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
  def testExample(): Unit = {
    checkDiffProperty { (start, local, remote, diffs) =>
      println("start:\n" + start)
      println("local:\n" + local)
      println("remote\n" + remote)
      true
    }
  }
}
