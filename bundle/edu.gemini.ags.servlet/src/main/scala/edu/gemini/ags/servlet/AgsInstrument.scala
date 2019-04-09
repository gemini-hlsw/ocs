package edu.gemini.ags.servlet

import edu.gemini.pot.sp.Instrument
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.LyotWheel
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth
import edu.gemini.spModel.gemini.init.NodeInitializers
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.telescope.PosAngleConstraint

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
    case Flamingos2(w, cons) =>
      val i = new edu.gemini.spModel.gemini.flamingos2.Flamingos2
      i.setLyotWheel(w)
      i.setPosAngleConstraint(cons)
      i.right

    case GmosNorth(fpu, cons) =>
      val i = new edu.gemini.spModel.gemini.gmos.InstGmosNorth
      i.setFPUnit(fpu)
      i.setPosAngleConstraint(cons)
      i.right

    case GmosSouth(fpu, cons) =>
      val i = new edu.gemini.spModel.gemini.gmos.InstGmosSouth
      i.setFPUnit(fpu)
      i.setPosAngleConstraint(cons)
      i.right

    case Gnirs(cons) =>
      val i = new edu.gemini.spModel.gemini.gnirs.InstGNIRS
      i.setPosAngleConstraint(cons)
      i.right

    case Gsaoi(cons) =>
      val i = new edu.gemini.spModel.gemini.gsaoi.Gsaoi
      i.setPosAngleConstraint(cons)
      i.right

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

  final case class Flamingos2(
    lyotWheel: LyotWheel,
    cons: PosAngleConstraint
  ) extends AgsInstrument(Instrument.Flamingos2)

  final case class GmosNorth(
    fpu: FPUnitNorth,
    cons: PosAngleConstraint
  ) extends AgsInstrument(Instrument.GmosNorth)

  final case class GmosSouth(
    fpu: FPUnitSouth,
    cons: PosAngleConstraint
  ) extends AgsInstrument(Instrument.GmosSouth)

  final case class Gnirs(
    cons: PosAngleConstraint
  ) extends AgsInstrument(Instrument.Gnirs)

  final case class Gsaoi(
    cons: PosAngleConstraint
  ) extends AgsInstrument(Instrument.Gsaoi)

  final case class Other(
    id: Instrument
  ) extends AgsInstrument(id)

  // N.B. Other includes F2, GMOS, etc. with default parameter values
}