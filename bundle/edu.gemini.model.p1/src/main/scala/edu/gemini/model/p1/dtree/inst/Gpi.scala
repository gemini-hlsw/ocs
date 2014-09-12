package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Gpi {
  def apply() = new ObservingModeNode

  class ObservingModeNode extends SingleSelectNode[Unit, GpiObservingMode, GpiObservingMode](()) {
    val title = "Observing Mode"
    val description = "Select an observing mode for your configuration."

    def choices: List[GpiObservingMode] = GpiObservingMode.values.toList

    def apply(om: GpiObservingMode) = Left(new DisperserNode(om))

    override def default = Some(GpiObservingMode.forName("CORON_H_BAND"))

    def unapply = {
      case b: GpiBlueprint => b.observingMode
    }
  }

  class DisperserNode(om: GpiObservingMode) extends SingleSelectNode[GpiObservingMode, GpiDisperser, GpiBlueprint](om){
    def title = "Disperser"
    def description = "Select a disperser for your configuration."

    def apply(d: GpiDisperser) = Right(GpiBlueprint(om, d))

    override def default = Some(GpiDisperser.forName("PRISM"))

    def unapply = {
      case b: GpiBlueprint => b.disperser
    }

    def choices = GpiDisperser.values.toList
  }

}