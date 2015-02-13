package edu.gemini.itc.baseline.util

import edu.gemini.itc.baseline._
import org.junit.{Ignore, Test}
import org.junit.Assert._

/**
 * Test cases that can be used to create a new baseline and to check exhaustively all provided configurations
 * against the current baseline.
 *
 * Execute {{{create()}}} in order to update the baseline. The file is written to the current output directory
 * (e.g. ../ocs/app/itc/idea/out/test/edu.gemini.itc-2015001.6.1/baseline.txt when working with IntelliJ) and
 * needs to be manually copied from there into the resources folder!
 *
 * Execute {{{checkAll()}}} in order to execute a test with all defined input values. This can be very time
 * consuming (~hour(s)) but allows for exhaustive testing and test coverage analysis.
 *
 * Since these tests are either meant to be executed manually or are very time consuming they are marked as Ignore.
 */
class BaselineTest {

  @Ignore
  @Test
  def create(): Unit = {
    val baseSeq = baselines()
    val baseMap = baseSeq.map(b => b.in -> b.out).toMap
    // --
    // make sure we don't run into the unlikely case that two baselines end up having identical keys
    // also checks if we have any problems with our makeshift hash values
    if (baseSeq.size != baseMap.size) throw new Exception("There are baselines with identical keys!")
    // --
    System.out.println(s"Writing new baseline with ${baseSeq.size} entries")
    Baseline.write(baseSeq)
  }

  @Ignore
  @Test
  def checkAll(): Unit =
    baselines().foreach { b => assertTrue(Baseline.checkAgainstBaseline(b)) }

  private def baselines(): Seq[Baseline] = {

    executeAll(BaselineAcqCam.Environments,    BaselineAcqCam.Observations,   BaselineAcqCam.executeRecipe)   ++
    executeAll(BaselineF2.Environments,        BaselineF2.Observations,       BaselineF2.executeRecipe)       ++
    executeAll(BaselineGmos.Environments,      BaselineGmos.Observations,     BaselineGmos.executeRecipe)     ++
    executeAll(BaselineGnirs.Environments,     BaselineGnirs.Observations,    BaselineGnirs.executeRecipe)    ++
    executeAll(BaselineGsaoi.Environments,     BaselineGsaoi.Observations,    BaselineGsaoi.executeRecipe)    ++
    executeAll(BaselineMichelle.Environments,  BaselineMichelle.Observations, BaselineMichelle.executeRecipe) ++
    executeAll(BaselineNifs.Environments,      BaselineNifs.Observations,     BaselineNifs.executeRecipe)     ++
    executeAll(BaselineNiri.Environments,      BaselineNiri.Observations,     BaselineNiri.executeRecipe)     ++
    executeAll(BaselineTRecs.Environments,     BaselineTRecs.Observations,    BaselineTRecs.executeRecipe)

  }

  private def executeAll[T <: Observation](envs: Seq[Environment], obs: Seq[T], f: (Environment, T) => Output): Seq[Baseline] =
    for {
      e <- envs
      o <- obs
    } yield Baseline.from(e, o, f(e, o))

}