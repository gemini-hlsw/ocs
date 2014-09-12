package edu.gemini.phase2.template.factory.impl.texes

import edu.gemini.spModel.gemini.texes.blueprint.SpTexesBlueprint

case class Texes(blueprint: SpTexesBlueprint) extends TexesBase {

  // Some groupings
  private var sci = Seq.empty[Int]

  // INCLUDE top-level program note "General TEXES Notes"
  addNote("General TEXES Notes") in TopLevel

  // INCLUDE {1}, {2}, and {3} IN target-specific Scheduling Group
  //     SCI={1},{2},{3}
  include(1, 2, 3) in TargetGroup
    sci = Seq(1, 2, 3)

  // # Disperser
  // SET DISPERSER from Phase-I
  forObs(sci: _*)(setDisperser fromPI)
}
