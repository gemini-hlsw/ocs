package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Trecs {
  import InstrumentMode._

  def apply() = new ModeNode

  class ModeNode extends SingleSelectNode[Unit, InstrumentMode, Unit](()) with InstrumentModeNode {
    def instrumentName = "T-ReCS"

    def apply(m: InstrumentMode) = m match {
      case Imaging      => Left(new FilterNode)
      case Spectroscopy => Left(new DisperserNode)
    }

    def unapply = {
      case _: TrecsBlueprintImaging      => Imaging
      case _: TrecsBlueprintSpectroscopy => Spectroscopy
    }
  }

  class FilterNode extends MultiSelectNode[Unit, TrecsFilter, TrecsBlueprintImaging](()) {
    val title       = "Filters"
    val description = "Select one or more filters."
    def choices     = TrecsFilter.values.toList

    def apply(fs: List[TrecsFilter]) = Right(TrecsBlueprintImaging(fs))

    def unapply = {
      case b: TrecsBlueprintImaging => b.filters
    }
  }

  class DisperserNode extends SingleSelectNode[Unit, TrecsDisperser, TrecsDisperser](()) {
    val title       = "Disperser"
    val description = "Select the disperser to use.  Note, the High Res Grating is only used in special cases and will require approval by the contact scientist."
    def choices     = TrecsDisperser.values.toList

    def apply(ds: TrecsDisperser) = Left(new FpuNode(ds))

    def unapply = {
      case b: TrecsBlueprintSpectroscopy => b.disperser
    }
  }

  class FpuNode(s: TrecsDisperser) extends SingleSelectNode[TrecsDisperser, TrecsFpu, TrecsBlueprintSpectroscopy](s) {
    val title       = "Focal Plane Unit"
    val description = "Select the focal plane mask."
    def choices     = TrecsFpu.values.toList

    def apply(fpu: TrecsFpu) = Right(TrecsBlueprintSpectroscopy(s, fpu))

    def unapply = {
      case b: TrecsBlueprintSpectroscopy => b.fpu
    }
  }
}