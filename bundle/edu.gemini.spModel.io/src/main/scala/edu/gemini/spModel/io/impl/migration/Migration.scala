package edu.gemini.spModel.io.impl.migration

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.pio.{ParamSet, Document}

/**
 * Base trait for all migrations.
 */
trait Migration {

  import PioSyntax._

  val PARAMSET_BASE                = "base"
  val PARAMSET_TARGET              = "spTarget"
  val PARAMSET_TEMPLATE_PARAMETERS = "Template Parameters"

  // Extract all the target paramsets, be they part of an observation or
  // template parameters.
  protected def allTargets(d: Document): List[ParamSet] = {
    val names = Set(PARAMSET_BASE, PARAMSET_TARGET)

    val templateTargets = for {
      cs  <- d.findContainers(SPComponentType.TEMPLATE_PARAMETERS)
      tps <- cs.allParamSets if tps.getName == PARAMSET_TEMPLATE_PARAMETERS
      ps  <- tps.allParamSets if names(ps.getName)
    } yield ps

    val obsTargets = for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps  <- env.allParamSets if names(ps.getName)
    } yield ps

    templateTargets ++ obsTargets
  }

}
