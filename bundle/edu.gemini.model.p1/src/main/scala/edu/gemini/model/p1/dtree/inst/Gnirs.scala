package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Gnirs {

  import InstrumentMode._

  def apply() = new AdaptiveOpticsNode

  class AdaptiveOpticsNode extends SingleSelectNode[Unit, Altair, Altair](()) with AltairNode {
    override val allGuideStarTypes = false
    def apply(a:Altair) = Left(new PixelScaleNode(a))
    def unapply = {
      case b:GnirsBlueprintBase => b.altair
    }
  }

  class PixelScaleNode(a:Altair) extends SingleSelectNode[Altair, GnirsPixelScale, (Altair, GnirsPixelScale)](a) {
    val title = "Pixel Scale"
    val description = "Please select pixel scale."
    val choices = GnirsPixelScale.values.toList
    def apply(ps:GnirsPixelScale) = Left(new ModeNode((a, ps)))
    
    override def default = a match {
      case AltairNone => Some(GnirsPixelScale.PS_015)
      case _          => Some(GnirsPixelScale.PS_005)
    }
    
    def unapply = {
      case b:GnirsBlueprintBase => b.pixelScale
    }
  }

  class ModeNode(s:(Altair, GnirsPixelScale)) extends SingleSelectNode[(Altair, GnirsPixelScale), InstrumentMode, (Altair, GnirsPixelScale)](s) with InstrumentModeNode {
    def instrumentName = "GNIRS"

    def apply(m: InstrumentMode) = m match {
      case Imaging      => Left(new ImagingNode(s))
      case Spectroscopy => Left(new DisperserNode(s))
    }
    def unapply = {
      case _:GnirsBlueprintImaging      => Imaging
      case _:GnirsBlueprintSpectroscopy => Spectroscopy
    }
  }

  class ImagingNode(s:(Altair, GnirsPixelScale)) extends SingleSelectNode[(Altair, GnirsPixelScale), GnirsFilter, GnirsBlueprintImaging](s) {
    val title = "Filters"
    val description = "Please select an imaging filter."
    val choices = GnirsFilter.values.toList
    def apply(f:GnirsFilter) = Right(GnirsBlueprintImaging(s._1, s._2, f))
    def unapply = {
      case b:GnirsBlueprintImaging => b.filter
    }
  }

  class DisperserNode(s:(Altair, GnirsPixelScale)) extends SingleSelectNode[(Altair, GnirsPixelScale), GnirsDisperser, (Altair, GnirsPixelScale, GnirsDisperser)](s) {
    val title = "Disperser"
    val description = "Please select a disperser."
    def choices = s._2 match {
      case GnirsPixelScale.PS_005 => GnirsDisperser.values.toList
      case GnirsPixelScale.PS_015 => GnirsDisperser.values.filterNot(_ == GnirsDisperser.D_10).toList
    }
    def apply(d:GnirsDisperser) = Left(new CrossDisperserNode((s._1, s._2, d)))
    def unapply = {
      case b:GnirsBlueprintSpectroscopy => b.disperser
    }
  }

  class CrossDisperserNode(s:(Altair, GnirsPixelScale, GnirsDisperser))
    extends SingleSelectNode[(Altair, GnirsPixelScale, GnirsDisperser), GnirsCrossDisperser, (Altair, GnirsPixelScale, GnirsDisperser, GnirsCrossDisperser)](s) {
    val title = "Cross Disperser"
    val description = "Please select a cross disperser."
    def choices = s._2 match {
      case GnirsPixelScale.PS_005 => GnirsCrossDisperser.values.toList
      case GnirsPixelScale.PS_015 => GnirsCrossDisperser.values.filterNot(_ == GnirsCrossDisperser.LXD).toList
    }
    def apply(d:GnirsCrossDisperser) = Left(new FpuNode((s._1, s._2, s._3, d)))
    def unapply = {
      case b:GnirsBlueprintSpectroscopy => b.crossDisperser
    }
  }

  class FpuNode(s:(Altair, GnirsPixelScale, GnirsDisperser, GnirsCrossDisperser))
    extends SingleSelectNode[(Altair, GnirsPixelScale, GnirsDisperser, GnirsCrossDisperser), GnirsFpu, (Altair, GnirsPixelScale, GnirsDisperser, GnirsCrossDisperser, GnirsFpu)](s) {
    val title = "Focal Plane Unit"
    val description = "Please select a focal plane unit."
    // REL-4149 Allow LR_IFU only for pixel scale 0.15 and No crossdisperser
    // REL-4149 was reverted but let's keep the code until LR-IFU is available
    // val choices = GnirsFpu.values.filterNot(f => (!(s._2 == GnirsPixelScale.PS_015 && s._4 == GnirsCrossDisperser.No) && f == GnirsFpu.LR_IFU) || f == GnirsFpu.HR_IFU).toList
    val choices = GnirsFpu.values.filterNot(f => f == GnirsFpu.LR_IFU || f == GnirsFpu.HR_IFU).toList 
    def apply(f:GnirsFpu) = Left(new CentralWavelengthNode((s._1, s._2, s._3, s._4, f)))
    def unapply = {
      case b:GnirsBlueprintSpectroscopy => b.fpu
    }
  }

  class CentralWavelengthNode(s:(Altair, GnirsPixelScale, GnirsDisperser, GnirsCrossDisperser, GnirsFpu))
    extends SingleSelectNode[(Altair, GnirsPixelScale, GnirsDisperser, GnirsCrossDisperser, GnirsFpu), GnirsCentralWavelength, GnirsBlueprintSpectroscopy](s) {
    val title = "Central Wavelength"
    val description = "Please select a central wavelength range."
    val choices = GnirsCentralWavelength.values .toList
    def apply(c:GnirsCentralWavelength) = Right(GnirsBlueprintSpectroscopy(s._1, s._2, s._3, s._4, s._5, c))
    def unapply = {
      case b:GnirsBlueprintSpectroscopy => b.centralWavelength
    }
  }

}
