package edu.gemini.itc.baseline.util

import edu.gemini.itc.baseline._
import edu.gemini.itc.service.InstrumentDetails
import org.junit.Assert._
import org.junit.{Ignore, Test}

/**
 * Test cases that can be used to create a new baseline and to check exhaustively all defined fixtures for all
 * instruments against the current baseline from the baseline.txt resource file.
 *
 * Execute {{{create()}}} in order to update the baseline. The file is written to the current output directory
 * (e.g. ../ocs/app/itc/idea/out/test/edu.gemini.itc-2015001.6.1/baseline.txt when working with IntelliJ) and
 * needs to be manually copied from there into the resources folder!
 *
 * Execute {{{checkAll()}}} in order to execute a test with all defined input values. This can be very time
 * consuming but allows for exhaustive testing and test coverage analysis.
 *
 * Since these tests are very time consuming and only meant to be executed manually they are marked as {{{@Ignore}}}.
 */
class BaselineTest {

  @Ignore
  @Test
  def create(): Unit = {
    val baseSeq = baselines()
    val baseMap = baseSeq.map(b => b.in -> b.out).toMap
    // --
    // make sure we don't run into the case where two baselines have identical keys
    // (having the hash functions be equal is highly unlikely, but this checks also
    // if our hash values are sound)
    require (baseSeq.size == baseMap.size, "There are baselines with identical keys!")
    // --
    System.out.println(s"Writing new baseline with ${baseSeq.size} entries")
    Baseline.write(baseSeq)
  }

  @Ignore
  @Test
  def checkAll(): Unit =
    baselines().foreach { b => assertTrue(Baseline.checkAgainstBaseline(b))}

  private def baselines(): Seq[Baseline] =
    executeAll(BaselineAcqCam.Fixtures,    BaselineAcqCam.executeRecipe)        ++
    executeAll(BaselineF2.Fixtures,        BaselineF2.executeRecipe)            ++
    executeAll(BaselineGmos.Fixtures,      BaselineGmos.executeRecipe)          ++
    executeAll(BaselineGnirs.Fixtures,     BaselineGnirs.executeRecipe)         ++
    executeAll(BaselineGsaoi.Fixtures,     BaselineGsaoi.executeRecipe)         ++
    executeAll(BaselineMichelle.Fixtures,  BaselineMichelle.executeRecipe)      ++
    executeAll(BaselineNifs.Fixtures,      BaselineNifs.executeRecipe)          ++
    executeAll(BaselineNiri.Fixtures,      BaselineNiri.executeRecipe)          ++
    executeAll(BaselineTRecs.Fixtures,     BaselineTRecs.executeRecipe)

  private def executeAll[T <: InstrumentDetails](fs: Seq[Fixture[T]], recipe: (Fixture[T]) => Output): Seq[Baseline] = {
       require(fs.size > 10, "Not enough fixtures " + fs.size) // make sure there's a good number of fixtures
      fs.par.map(f => Baseline.from(f, recipe(f))).seq
  }

}