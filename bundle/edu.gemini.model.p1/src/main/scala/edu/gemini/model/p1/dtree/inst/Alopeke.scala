package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Alopeke {

  def apply() = new ModeNode

  class ModeNode extends SingleSelectNode[Unit, AlopekeMode, AlopekeBlueprint](()) {
    override val title = "Mode"
    override val description = "Please select mode, pixel scale, and field of view."
    override val choices: List[AlopekeMode] = AlopekeMode.values.toList
    override def apply(m: AlopekeMode) = Right(AlopekeBlueprint(m))

    override def unapply = {
      case b: AlopekeBlueprint => b.mode
    }
  }
}
