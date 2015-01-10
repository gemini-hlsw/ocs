package edu.gemini.itc.baseline.util

import edu.gemini.itc.baseline._
import org.junit.{Ignore, Test}

/**
 * Test cases that can be used to create a new baseline and to check exhaustively all provided configurations
 * against the current baseline. By default these tests are time consuming and we therefore only want to
 * execute them "on demand".
 */
class BaselineTest {

  @Ignore
  @Test
  def create(): Unit = {
    val baseSeq = baselines()
    val baseMap = baseSeq.map(b => b.in -> b.out).toMap
    // --
    // make sure we don't run into the unlikely case that two baselines end up having identical keys
    if (baseSeq.size != baseMap.size) throw new Exception("There are baselines with identical keys!")
    // --
    Baseline.write(baseSeq)
  }
  
  @Ignore
  @Test
  def checkAll(): Unit = {
    baselines().foreach(Baseline.checkAgainstBaseline)
  }

  private def baselines(): Seq[Baseline] = {
    val envs = environments()

    executeAll(envs, BaselineAcqCamSpec.Observations,   BaselineAcqCamSpec.executeRecipe) ++
    executeAll(envs, BaselineF2Spec.Observations,       BaselineF2Spec.executeRecipe) ++
    executeAll(envs, BaselineGmosSpec.Observations,     BaselineGmosSpec.executeRecipe) ++
    executeAll(envs, BaselineGnirsSpec.Observations,    BaselineGnirsSpec.executeRecipe) ++
    executeAll(envs, BaselineGsaoiSpec.Observations, BaselineGsaoiSpec.executeRecipe) ++
    executeAll(envs, BaselineMichelleSpec.Observations, BaselineMichelleSpec.executeRecipe) ++
    executeAll(envs, BaselineNiciSpec.Observations,     BaselineNiciSpec.executeRecipe) ++
    executeAll(envs, BaselineNifsSpec.Observations,     BaselineNifsSpec.executeRecipe) ++
    executeAll(envs, BaselineNiriSpec.Observations,     BaselineNiriSpec.executeRecipe) ++
    // NOTE: TRecs needs special conditions!
    executeAll(envs.filter(BaselineTRecsSpec.isValidForTRecs), BaselineTRecsSpec.Observations,    BaselineTRecsSpec.executeRecipe)

  }

  private def executeAll[T <: Observation](envs: Seq[Environment], obs: Seq[T], f: (Environment, T) => Output): Seq[Baseline] =
    for {
      e <- envs
      o <- obs
    } yield Baseline.from(e, o, f(e, o))

  private def environments() =
    for {
      src <- Environment.Sources
      ocp <- Environment.ObservingConditions
      tp  <- Environment.TelescopeConfigurations
      pdp <- Environment.PlottingParameters
    } yield Environment(src, ocp, tp, pdp)

}