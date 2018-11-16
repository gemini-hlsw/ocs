package edu.gemini.phase2.template.factory.impl.visitor

import edu.gemini.spModel.gemini.visitor.blueprint.SpVisitorBlueprint

case class Visitor(blueprint: SpVisitorBlueprint) extends VisitorBase {

  // Some groupings
  private var sci = Seq.empty[Int]

  //  INCLUDE {1} IN target-specific Scheduling Group
  include(1) in TargetGroup
    sci = Seq(1)

  // SET Name from Phase-I
  forObs(sci: _*)(setName fromPI)
  forObs(sci: _*)(setWavelength fromPI)
}
