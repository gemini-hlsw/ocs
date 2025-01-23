package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Igrins2 {

  def apply() = new NoddingOption

  class NoddingOption extends SingleSelectNode[Unit, Igrins2NoddingOption, Igrins2NoddingOption](()) {
    override val title = "Nodding"
    override val description = "Select a nodding option"
    override val choices: List[Igrins2NoddingOption] = Igrins2NoddingOption.values.toList
    override def apply(m: Igrins2NoddingOption) = Left(new TelluricStarsOption(m))

    override def unapply = {
      case b: Igrins2Blueprint => b.nodding
    }
  }

  // The enumeration cannot include numbers thus we use a String as interemdiate here
  class TelluricStarsOption(n: Igrins2NoddingOption) extends SingleSelectNode[Igrins2NoddingOption, String, Igrins2Blueprint](n) {
    override val title = "Telluric calibration stars"
    override val description = "Select the the number of telluric stars per observation. Each star uses 0.25 hr of partner time. Any change from the default must be justified in the technical description section."
    override val choices: List[String] = Igrins2TelluricStars.values.toList.map(Igrins2TelluricStars.show)
    override def apply(m: String) = {
      Right(Igrins2Blueprint(n, Igrins2TelluricStars.unsafeFromString(m)))
    }

    override def unapply = {
      case b: Igrins2Blueprint => Igrins2TelluricStars.show(b.telluricStars)
    }
  }
}
