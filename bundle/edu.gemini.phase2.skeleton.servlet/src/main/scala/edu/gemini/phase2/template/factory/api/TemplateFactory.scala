package edu.gemini.phase2.template.factory.api

import edu.gemini.spModel.template.{Phase1Group, SpBlueprint}
import edu.gemini.spModel.core.SPProgramID

/**
 * Provides the API for a service that creates template groups.
 */
trait TemplateFactory {

  /**
   * Expands the given blueprint and its arguments into a template group and a group of baseline
   * calibrations. If `preserveLibraryIds` is false (the typical case) then library ids will be
   * erased from the generated program; if true they will be preserved, which can be helpful for
   * testing.
   */
  def expand(blueprint: SpBlueprint, pig: Phase1Group, preserveLibraryIds: Boolean, pid: SPProgramID): Either[String, BlueprintExpansion]

}
