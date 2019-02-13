package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Zorro {

  def apply() = new ModeNode

  class ModeNode extends SingleSelectNode[Unit, ZorroMode, ZorroBlueprint](()) {
    override val title = "Mode"
    override val description = "Please select mode, pixel scale, and field of view."
    override val choices: List[ZorroMode] = ZorroMode.values.toList
    override def apply(m: ZorroMode) = Right(ZorroBlueprint(m))

    override def unapply = {
      case b: ZorroBlueprint => b.mode
    }
  }
}
