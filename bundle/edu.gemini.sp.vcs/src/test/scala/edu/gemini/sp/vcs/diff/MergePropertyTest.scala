package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{ISPFactory, ISPProgram}
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.spModel.rich.pot.sp._
import org.scalacheck.Prop
import org.scalatest.prop.Checkers

//
// Program property testing is a bit unorthodox in that there is one big
// "property" that combines a bunch of smaller ones.  Program generation is
// complicated by the fact that the ScienceProgram is mutable and must be
// created and manipulated in the context of a database and factory.
//
// Only about 3% of the total time is taken verifying the properties plus
// actually running the ProgramDiff for all the generated programs and edits.
// In other words, about 97% of the time is the overhead of the program and edit
// generation. If we separate the properties and generate programs and edits for
// each, the time required to run the tests will be multiplied by the number of
// properties (so instead of ~14 seconds it would take ~100 seconds).
//
// Because of the mutable science program, the generated programs and edits are
// actually functions which make the normal property failure output less useful.
// It will tell us that a property failed but the arguments it displays are
// only functions, as in:
//
//  Falsified after 0 successful property evaluations.
//  Location: (ProgramDiffPropertyTest.scala:154)
//  Occurred when passed generated values (
//    arg0 = <function1>
//  )
//
// For that reason, we explicitly write out the generated programs that trigger
// a failure.
//

/** Support for testing program merge.
  *
  * @param f combines two edited programs into a single result of type A
  */
class MergePropertyTest[A](f: (ISPProgram, ISPProgram) => A) extends Checkers {

  import MergePropertyTest._

  def checkOneProperty(p: MergeProperty[A]): Unit = {
    val odb  = DBLocalDatabase.createTransient()
    val fact = odb.getFactory

    import edu.gemini.pot.sp.ProgramGen._

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
        p(start, ed0, ed1, f(ed0, ed1))
      })
    } finally {
      odb.getDBAdmin.shutdown()
    }
  }

  def checkAllProperties(ps: List[NamedProperty[A]]): Unit =
    checkOneProperty { (start, local, remote, a) =>
      val failure = ps.find { case (_, p) => !p(start, local, remote, a) }

      failure.foreach { case (name, _) =>
        Console.err.println("*** Program property failure: " + name)
        val titles = List("Start", "Local", "Remote")
        val progs  = List(start,   local,   remote)
        titles.zip(progs).foreach { case (title, prog) =>
          Console.err.println(s"\n$title")
          Console.err.println(drawNodeTree(prog))
        }
      }

      failure.isEmpty
    }
}

object MergePropertyTest {
  /** A property that evaluates a starting program, a potentially  edited
    * "local" version, a potentially edited "remote" version, and a result of
    * combining the two edited versions.
    */
  type MergeProperty[A] = (ISPProgram, ISPProgram, ISPProgram, A) => Boolean

  /** A program property with a name / description. */
  type NamedProperty[A] = (String, MergeProperty[A])
}
