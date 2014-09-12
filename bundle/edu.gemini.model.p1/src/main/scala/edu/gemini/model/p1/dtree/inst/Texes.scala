package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Texes {

  def apply() = new DisperserNode

  class DisperserNode extends SingleSelectNode[Unit, TexesDisperser, TexesBlueprint](()) {
    val title       = "Disperser"
    val description = "Select the disperser to use."
    def choices     = TexesDisperser.values.toList

    def apply(ds: TexesDisperser) = Right(new TexesBlueprint(ds))

    def unapply = {
      case b: TexesBlueprint => b.disperser
    }
  }

}