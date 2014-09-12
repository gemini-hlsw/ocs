package edu.gemini.phase2.template.factory.api

import edu.gemini.spModel.template.SpBlueprint
import edu.gemini.spModel.template.TemplateFolder.Phase1Group
import edu.gemini.spModel.target.SPTarget

/**
 * Provides the API for a service that creates template groups.
 */
trait TemplateFactory {

  /**
   * Expands the given blueprint and its arguments into a template group and
   * a group of baseline calibrations.
   */
  def expand(blueprint: SpBlueprint, pig: Phase1Group, tMap:Map[String, SPTarget]): Either[String, BlueprintExpansion]
}
