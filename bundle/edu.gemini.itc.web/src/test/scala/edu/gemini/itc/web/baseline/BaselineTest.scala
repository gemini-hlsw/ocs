package edu.gemini.itc.web.baseline

import edu.gemini.itc.baseline._
import edu.gemini.itc.baseline.util.Fixture
import edu.gemini.itc.shared.InstrumentDetails
import edu.gemini.itc.web.baseline.Baseline._
import org.junit.Assert._
import org.junit.{Ignore, Test}

/**
 * Test cases that can be used to create a new baseline and to check exhaustively all defined fixtures for all
 * instruments against the current baseline from the baseline.txt resource file.
 *
 * Execute {{{create()}}} in order to update the baseline. The file is written to the current output directory
 * (e.g. ../ocs/app/itc/idea/out/test/edu.gemini.itc.web-2015001.6.1/baseline.txt when working with IntelliJ) and
 * needs to be manually copied from there into the resources folder! Updating the baseline will be necessary after
 * any change that impacts the ITC calculation results and the HTML output.
 *
 * Execute {{{checkAll()}}} in order to execute a test with all defined input values. This can be very time
 * consuming but allows for exhaustive testing and test coverage analysis. A small, random subset of the defined
 * fixtures is executed as part of the default tests by the test spec [[BaselineAllSpec]]. See the code for
 * more details.
 *
 * Since these tests are very time consuming and only meant to be executed manually they are marked as {{{@Ignore}}}.
 *
 * When {{{checkAll()}}} fails, every fixture whose output differs from the baseline is printed to stderr.
 * To see what actually changed, set the system property {{{baseline.dump}}} to a directory; the output of every
 * fixture is then written there as {{{<key>.txt}}}. Run once on master and once on your branch, then diff the
 * two directories (e.g. {{{diff -r /tmp/baseline-master /tmp/baseline-branch}}}).  From sbt:
 * {{{
 *   set fork in Test := true
 *   set javaOptions in Test += "-Dbaseline.dump=/tmp/baseline"
 * }}}
 *
 */
class BaselineTest {

  /** A baseline together with the fixture and output that produced it, for diagnostics. */
  private case class Result(fixture: Fixture[_ <: InstrumentDetails], output: Output, baseline: Baseline)

  @Ignore
  @Test
  def create(): Unit = {
    val baseSeq = baselines().map(_.baseline)
    val baseMap = baseSeq.map(b => b.in -> b.out).toMap
    // --
    // make sure we don't run into the case where two baselines have identical keys
    // (having the hash functions be equal is highly unlikely, but this checks also
    // if our hash values are sound)
    require(baseSeq.size == baseMap.size, "There are baselines with identical keys!")
    // --
    System.out.println(s"Writing new baseline with ${baseSeq.size} entries")
    Baseline.write(baseSeq)
  }

  @Ignore
  @Test
  def checkAll(): Unit = {
    val results = baselines()

    // optionally write every fixture's output to disk so it can be diffed against another branch
    sys.props.get("baseline.dump").foreach(dir => dumpOutputs(results, dir))

    // collect all mismatches (instead of stopping at the first one) and report which fixtures they are
    val failures = results.filterNot(r => Baseline.checkAgainstBaseline(r.baseline))
    failures.foreach { r =>
      System.err.println(
        s"""
           |===== Baseline MISMATCH =====
           |fixture: ${r.fixture}
           |key:     ${r.baseline.in}
           |=============================""".stripMargin)
    }
    System.err.flush()

    assertTrue(s"${failures.size} of ${results.size} baselines differ from baseline.txt", failures.isEmpty)
  }

  private def baselines(): Seq[Result] =
    executeAll(BaselineAcqCam.Fixtures, executeAcqCamRecipe) ++
    executeAll(BaselineF2.Fixtures, executeF2Recipe) ++
    executeAll(BaselineGmos.Fixtures, executeGmosRecipe) ++
    executeAll(BaselineGnirs.Fixtures, executeGnirsRecipe) ++
    executeAll(BaselineGsaoi.Fixtures, executeGsaoiRecipe) ++
    executeAll(BaselineMichelle.Fixtures, executeMichelleRecipe) ++
    executeAll(BaselineNifs.Fixtures, executeNifsRecipe) ++
    executeAll(BaselineNiri.Fixtures, executeNiriRecipe) ++
    executeAll(BaselineTRecs.Fixtures, executeTrecsRecipe)

  private def executeAll[T <: InstrumentDetails](fs: Seq[Fixture[T]], recipe: (Fixture[T]) => Output): Seq[Result] = {
    require(fs.size > 10, "Not enough fixtures " + fs.size) // make sure there's a good number of fixtures
    fs.par.map { f =>
      val output =
        try recipe(f)
        catch {
          case e: Throwable =>
            // the fixtures run in parallel, so the stack trace alone doesn't say which one failed
            System.err.println(
              s"""
                 |===== Baseline recipe FAILED =====
                 |fixture: $f
                 |error:   $e
                 |==================================""".stripMargin)
            System.err.flush()
            throw e
        }
      Result(f, output, Baseline.from(f, output))
    }.seq
  }

  /** Write each fixture and its output to `<dir>/<key>.txt`; the key is derived from the fixture,
    * so the same configuration gets the same file name on every branch. */
  private def dumpOutputs(results: Seq[Result], dir: String): Unit = {
    val d = new java.io.File(dir)
    d.mkdirs()
    results.foreach { r =>
      val w = new java.io.PrintWriter(new java.io.File(d, s"${r.baseline.in}.txt"))
      try w.println(s"fixture: ${r.fixture}\n\n${r.output}") finally w.close()
    }
    System.out.println(s"Wrote ${results.size} baseline outputs to ${d.getAbsolutePath}")
  }

}
