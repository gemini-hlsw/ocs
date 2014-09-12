package edu.gemini.phase2.template.factory.impl.graces

import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint
import edu.gemini.pot.sp.ISPGroup

case class Graces(blueprint: SpGracesBlueprint) extends GroupInitializer[SpGracesBlueprint] {
  val program = "GRACES PHASE I/II MAPPING BPS"
  val targetGroup = Seq(1, 2)
  val baselineFolder = Seq.empty
  val notes = Seq("N FIBERS")

  def initialize(group:ISPGroup, db:TemplateDb): Maybe[Unit] = Right(())
}
