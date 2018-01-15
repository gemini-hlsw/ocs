package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object ʻAlopeke {

  def apply() = new ModeNode

  class ModeNode extends SingleSelectNode[Unit, ʻAlopekeMode, ʻAlopekeBlueprint](()) {
    override val title = "Mode"
    override val description = "Please select mode, pixel scale, and field of view."
    override val choices: List[ʻAlopekeMode] = ʻAlopekeMode.values.toList
    override def apply(m: ʻAlopekeMode) = Right(ʻAlopekeBlueprint(m))

    override def unapply = {
      case b: ʻAlopekeBlueprint => b.mode
    }
  }
}
