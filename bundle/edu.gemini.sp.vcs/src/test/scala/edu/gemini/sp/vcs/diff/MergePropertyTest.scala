package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version.{LifespanId, NodeVersions}
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.spModel.rich.pot.sp._
import org.scalacheck.Prop
import org.scalatest.prop.Checkers

import scalaz._

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
  * @param f combines a starting program and two edited versions into a single
  *          result of type A
  */
class MergePropertyTest[A](f: (ISPProgram, ISPProgram, ISPProgram) => A) extends Checkers {

  import MergePropertyTest._


  def checkAllProperties(ps: List[NamedProperty[A]]): Unit =
    exec { (start, local, remote, a) =>
      val failure = ps.find { case (_, p) => !p(start, local, remote, a) }

      failure.foreach { case (name, _) =>
        Console.err.println("*** Program property failure: " + name)
        showProgs(start, local, remote)
      }

      failure.isEmpty
    }

  private def exec(p: MergeProperty[A]): Unit = {
    val odb  = DBLocalDatabase.createTransient()
    val fact = odb.getFactory

    import edu.gemini.pot.sp.ProgramGen._

    // Generate a starting program and two independently edited copies of it.
    val genProgs = for {
      fStart <- genProg
      fEd0   <- genEditedProg
      fEd1   <- genEditedProg
    } yield {(f: ISPFactory) => {
      val start  = fStart(f)
      val local  = fEd0(f, start)
      val remote = fEd1(f, start)
      (start, local, remote)
    }}

    try {
      check(Prop.forAll(genProgs) { fun =>
        val (start, local, remote) = fun(fact)
        p(start, local, remote, f(start, local, remote))
      })
    } finally {
      odb.getDBAdmin.shutdown()
    }
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

  def showProgs(start: ISPProgram, local: ISPProgram, remote: ISPProgram): Unit = {
    val showNodeWithVersions = {
      val lifespanMap = Map(
        start.getLifespanId -> "start",
        local.getLifespanId -> "local",
        remote.getLifespanId -> "remote").withDefault(k => s"??? ($k)")

      def showNodeVersionsEntry(kv: (LifespanId, Integer)): String =
        s"${lifespanMap(kv._1)} -> ${kv._2}"

      def showNodeVersions(nv: NodeVersions): String =
        nv.clocks.map(showNodeVersionsEntry).toList.sorted.mkString("[", ", ", "]")

      Show.shows[ISPNode] { n: ISPNode => s"${ShowNode.shows(n)} ${showNodeVersions(n.getVersion)}"}
    }

    val titles = List("Start", "Local", "Remote")
    val progs  = List(start,   local,   remote)
    titles.zip(progs).foreach { case (title, prog) =>
      Console.err.println(s"\n$title")
      Console.err.println(drawNodeTree(prog)(showNodeWithVersions))
    }
  }
}
