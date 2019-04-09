package edu.gemini.ags.servlet

import edu.gemini.pot.sp.Instrument
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.LyotWheel
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth
import edu.gemini.spModel.gemini.init.NodeInitializers
import edu.gemini.spModel.obscomp.SPInstObsComp

import scalaz._
import Scalaz._

/**
 * Specifies the instrument to use in an AGS request including only the details
 * relevant to AGS.
 */
sealed abstract class AgsInstrument(id: Instrument) extends Product with Serializable {

  import AgsInstrument._

  /**
   * AGS requires a configured SPInstObsComp so we create one from the provided
   * information.  Unspecified instrument parameters are irrelevant for the AGS
   * calculation.
   */
  def instObsComp: \/[String, SPInstObsComp] = this match {
    case Flamingos2(w) =>
      val f2 = new edu.gemini.spModel.gemini.flamingos2.Flamingos2
      f2.setLyotWheel(w)
      f2.right

    case GmosNorth(fpu) =>
      val gn = new edu.gemini.spModel.gemini.gmos.InstGmosNorth
      gn.setFPUnit(fpu)
      gn.right

    case GmosSouth(fpu) =>
      val gs = new edu.gemini.spModel.gemini.gmos.InstGmosSouth
      gs.setFPUnit(fpu)
      gs.right

    case Other(i) =>
      (Option(NodeInitializers.instance.obsComp.get(i.componentType)) \/> s"No node initializer for instrument $i")
        .map(_.createDataObject)
        .flatMap {
          case inst: SPInstObsComp => inst.right
          case _                   => s"Internal error, not an instrument type $i".left
        }

  }

}

object AgsInstrument {

  final case class Flamingos2(lyotWheel: LyotWheel) extends AgsInstrument(Instrument.Flamingos2)
  final case class GmosNorth(fpu: FPUnitNorth)      extends AgsInstrument(Instrument.GmosNorth)
  final case class GmosSouth(fpu: FPUnitSouth)      extends AgsInstrument(Instrument.GmosSouth)
  final case class Other(id: Instrument)            extends AgsInstrument(id)

  // N.B. Other includes F2 and GMOS, with default parameter values
}