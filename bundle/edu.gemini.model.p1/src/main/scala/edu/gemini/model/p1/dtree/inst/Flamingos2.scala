package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Flamingos2 {

  object F2Mode extends Enumeration {
    val Imaging  = Value("Imaging")
    val Longslit = Value("Long-slit Spectroscopy")
    val Mos      = Value("Multi-object Spectroscopy")
    type F2Mode = Value
  }

  import F2Mode._

  def apply() = new ModeNode

  import Flamingos2Filter._
  import Flamingos2Disperser._
  private def filtersFor(d: Option[Flamingos2Disperser]): List[Flamingos2Filter] = d match {
    case Some(R1200HK) => List(JH, HK)
    case Some(R1200JH) => List(JH)
    case Some(R3000)   => List(J_LOW, J, H, K_LONG, K_SHORT, K_BLUE, K_RED)
    case _             => List(Y, J_LOW, J, H, K_LONG, K_SHORT, K_BLUE, K_RED)
  }

  private def defaultFilterFor(d: Flamingos2Disperser): Option[Flamingos2Filter] = d match {
    case R1200HK => Some(HK)
    case R1200JH => Some(JH)
    case _       => None
  }

  class ModeNode extends SingleSelectNode[Unit, F2Mode, Unit](()) {
    val title       = "Select Instrument Mode"
    val description = "Flamingos2 can be used for both imaging and spectroscopy."
    def choices     = F2Mode.values.toList

    def apply(m: F2Mode) = m match {
      case Imaging   => Left(new ImagingFilterNode)
      case Longslit  => Left(new FpuNode)
      case Mos       => Left(new PreImagingNode)
    }

    def unapply = {
      case _: Flamingos2BlueprintImaging  => Imaging
      case _: Flamingos2BlueprintLongslit => Longslit
      case _: Flamingos2BlueprintMos      => Mos
    }
  }

  class ImagingFilterNode extends MultiSelectNode[Unit, Flamingos2Filter, Flamingos2BlueprintImaging](()) {
    val title       = "Imaging Filters"
    val description = "Select at least one filter for your imaging configuration.  More than one may be selected."
    def choices     = filtersFor(None)

    def apply(fs: List[Flamingos2Filter]) = Right(Flamingos2BlueprintImaging(fs))

    def unapply = {
      case b: Flamingos2BlueprintImaging => b.filters
    }
  }

  // A function that takes a disperser and a filter and makes a spectroscopy
  // blueprint.
  type BlueprintGen = ((Flamingos2Disperser, List[Flamingos2Filter]) => Flamingos2BlueprintSpectroscopyBase)

  class FpuNode extends SingleSelectNode[Unit, Flamingos2Fpu, BlueprintGen](()) {
    val title       = "Long-slit FPUnit"
    val description = "Select a focal plane unit."
    def choices     = Flamingos2Fpu.values.toList

    def apply(fpu: Flamingos2Fpu) = Left(new DisperserNode(Flamingos2BlueprintLongslit(_,_,fpu)))

    def unapply = {
      case b: Flamingos2BlueprintLongslit => b.fpu
    }
  }

  object PreImaging extends Enumeration {
    val Yes, No = Value
    type PreImaging = Value
    implicit val toBool = (pi:PreImaging) => pi == Yes
  }

  import PreImaging._

  class PreImagingNode extends SingleSelectNode[Unit, PreImaging, BlueprintGen](()) {
    val title       = "Pre-Imaging"
    val description = "Is pre-imaging required for this configuration?"
    def choices     = PreImaging.values.toList

    def apply(pi: PreImaging) = Left(new DisperserNode(Flamingos2BlueprintMos(_,_,PreImaging.toBool(pi))))

    def unapply = {
      case b: Flamingos2BlueprintMos => if (b.preImaging) Yes else No
    }
  }

  class DisperserNode(s: BlueprintGen) extends SingleSelectNode[BlueprintGen, Flamingos2Disperser, Flamingos2BlueprintSpectroscopyBase](s) {
    val title       = "Disperser"
    val description = "Select a disperser."
    def choices     = Flamingos2Disperser.values.toList

    def apply(d: Flamingos2Disperser) = {
      val filters   = filtersFor(Some(d))
      val blueprint = s(d, filters)
      if (filters.size == 1)
        Right(blueprint)
      else
        Left(new SpectroscopyFilterNode(blueprint))
    }

    def unapply = {
      case b: Flamingos2BlueprintSpectroscopyBase => b.disperser
    }
  }

  class SpectroscopyFilterNode(s: Flamingos2BlueprintSpectroscopyBase) extends MultiSelectNode[Flamingos2BlueprintSpectroscopyBase, Flamingos2Filter, Flamingos2BlueprintSpectroscopyBase](s) {
    val title       = "Filter"
    val description = "Select at least one filter for your spectroscopy configuration.  More than one filter may be selected."
    def choices     = filtersFor(Some(s.disperser))

    def apply(fs: List[Flamingos2Filter]) = Right(s.withFilters(fs))

    override def default = defaultFilterFor(s.disperser).map(List(_))

    def unapply = {
      case b: Flamingos2BlueprintSpectroscopyBase => b.filters
    }
  }
}