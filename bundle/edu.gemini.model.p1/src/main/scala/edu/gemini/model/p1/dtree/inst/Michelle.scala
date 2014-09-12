package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Michelle {

  def apply() = new ModeNode

  import InstrumentMode._

  class ModeNode extends SingleSelectNode[Unit, InstrumentMode, Unit](()) with InstrumentModeNode {
    def instrumentName = "Michelle"

    def apply(m: InstrumentMode) = m match {
      case Imaging      => Left(new FilterNode)
      case Spectroscopy => Left(new FpuNode)
    }
    def unapply = {
      case _: MichelleBlueprintImaging      => Imaging
      case _: MichelleBlueprintSpectroscopy => Spectroscopy
    }
  }

  class FilterNode extends MultiSelectNode[Unit, MichelleFilter, List[MichelleFilter]](()) {
    val title       = "Filters"
    val description = "Select one or more filters."
    def choices     = MichelleFilter.values.toList

    def apply(fs: List[MichelleFilter]) = Left(new PolarimetryNode(fs))

     def unapply = {
       case b: MichelleBlueprintImaging => b.filters
     }
  }

  class PolarimetryNode(s: List[MichelleFilter]) extends SingleSelectNode[List[MichelleFilter], MichellePolarimetry, MichelleBlueprintImaging](s) {
    val title       = "Polarimetry"
    val description = "Will polarimetry mode be used?"
    def choices     = MichellePolarimetry.values.toList

    def apply(p: MichellePolarimetry) = Right(MichelleBlueprintImaging(s, p))

    override def default = Some(MichellePolarimetry.NO)
    
    def unapply = {
      case b: MichelleBlueprintImaging => b.polarimetry
    }
  }

  class FpuNode extends SingleSelectNode[Unit, MichelleFpu, MichelleFpu](()) {
    val title       = "Focal Plane Mask"
    val description = "Select the focal plane mask."
    def choices     = MichelleFpu.values.toList

    def apply(fpu: MichelleFpu) = Left(new DisperserNode(fpu))

    def unapply = {
      case b: MichelleBlueprintSpectroscopy => b.fpu
    }
  }

  class DisperserNode(s: MichelleFpu) extends SingleSelectNode[MichelleFpu, MichelleDisperser, MichelleBlueprintSpectroscopy](s) {
    val title       = "Disperser"
    val description = "Select the disperser."
    def choices     = MichelleDisperser.values.toList

    def apply(d: MichelleDisperser) = Right(MichelleBlueprintSpectroscopy(s, d))

    def unapply = {
      case b: MichelleBlueprintSpectroscopy => b.disperser
    }
  }
}