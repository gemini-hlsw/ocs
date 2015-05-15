package edu.gemini.phase2.template.factory.api

import edu.gemini.spModel.template.{Phase1Group, SpBlueprint}

/**
 * Provides the API for a service that creates template groups.
 */
trait TemplateFactory {

  /**
   * Expands the given blueprint and its arguments into a template group and
   * a group of baseline calibrations.
   */
  def expand(blueprint: SpBlueprint, pig: Phase1Group, testing: Boolean): Either[String, BlueprintExpansion]
}
