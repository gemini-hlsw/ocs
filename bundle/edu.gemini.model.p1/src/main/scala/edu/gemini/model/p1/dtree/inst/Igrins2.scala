package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Igrins2 {

  def apply() = new NoddingOption

  class NoddingOption extends SingleSelectNode[Unit, Igrins2NoddingOption, Igrins2Blueprint](()) {
    override val title = "Nodding"
    override val description = "Select a nodding option"
    override val choices: List[Igrins2NoddingOption] = Igrins2NoddingOption.values.toList
    override def apply(m: Igrins2NoddingOption) = Right(Igrins2Blueprint(m))

    override def unapply = {
      case b: Igrins2Blueprint => b.nodding
    }
  }
}
