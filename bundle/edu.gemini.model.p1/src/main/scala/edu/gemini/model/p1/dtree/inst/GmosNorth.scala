package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

import edu.gemini.model.p1.dtree.inst.GmosCommon._
import edu.gemini.model.p1.dtree.inst.InstrumentMode._

object GmosNorth {

  import InstrumentMode._

   def apply() = new AdaptiveOpticsNode

  class AdaptiveOpticsNode extends SingleSelectNode[Unit, Altair, Altair](()) with AltairNode {
    override val allGuideStarTypes = true
    def apply(a: Altair) = Left(new SelectMode(a))
    def unapply = {
      case b: GmosNBlueprintBase => b.altair
    }
  }

  class SelectMode(altair: Altair) extends SingleSelectNode[Altair, InstrumentMode, (Altair, InstrumentMode)](altair) with InstrumentModeNode {
    def instrumentName = "GMOS"

    def apply(m: InstrumentMode) = m match {
      case Imaging      => Left(new SelectImagingFilters(altair))
      case Spectroscopy => Left(new SelectSpecMode(altair))
    }

    def unapply = {
      case _: GmosNBlueprintIfu        => Spectroscopy
      case _: GmosNBlueprintImaging    => Imaging
      case _: GmosNBlueprintLongslit   => Spectroscopy
      case _: GmosNBlueprintLongslitNs => Spectroscopy
      case _: GmosNBlueprintMos        => Spectroscopy
    }
  }

  // Imaging Terminal Node
  class SelectImagingFilters(altair: Altair) extends MultiSelectNode[(Altair, InstrumentMode), GmosNFilter, GmosNBlueprintImaging]((altair, InstrumentMode.Imaging)) {
    val title = "Imaging Filters"
    val description = "Select at least one filter for your imaging configuration. More than one filter may be selected."
    def choices = if (altair == AltairNone) GmosNFilter.IMAGING else GmosNFilter.ALTAIR_IMAGING
    def apply(fs: List[GmosNFilter]) = Right(GmosNBlueprintImaging(altair, fs))

    def unapply = {
      case b: GmosNBlueprintImaging => b.filters
    }
  }

  // SPECTROSCOPY NODES ----------

  object SpecMode extends Enumeration {
    val Longslit   = Value("Longslit")
    val LongslitNS = Value("Longslit Nod & Shuffle")
    val Mos        = Value("MOS")
    val MosNS      = Value("MOS Nod & Shuffle")
    val Ifu        = Value("IFU")
    type SpecMode = Value
  }
  import SpecMode._

  class SelectSpecMode(altair: Altair) extends SingleSelectNode[(Altair, InstrumentMode), SpecMode, (Altair, NodAndShuffle)]((altair, InstrumentMode.Spectroscopy)) {
    val title       = "Select Spectroscopy Mode"
    val description = "Select a spectroscopy mode. See the Gemini website for more information on supported modes."
    def choices     = SpecMode.values.toList

    def apply(mode: SpecMode) = mode match {
        case Mos        => Left(new SelectMosPreImaging(altair, NsNo))
        case MosNS      => Left(new SelectMosPreImaging(altair, NsYes))
        case Longslit   => Left(new SelectLongslitFpu(altair, NsNo))
        case LongslitNS => Left(new SelectLongslitFpuNs(altair, NsYes))
        case Ifu        => Left(new SelectIfuFpu(altair, NsNo))
      }

    def unapply = {
      case b: GmosNBlueprintIfu                    => Ifu
      case b: GmosNBlueprintLongslit               => Longslit
      case b: GmosNBlueprintLongslitNs             => LongslitNS
      case b: GmosNBlueprintMos if b.nodAndShuffle => MosNS
      case b: GmosNBlueprintMos                    => Mos
    }
  }

  type BlueprintGen = ((Altair, GmosNDisperser, GmosNFilter) => GmosNBlueprintSpectrosopyBase)

  class SelectMosPreImaging(altair: Altair, ns: NodAndShuffle)
    extends SingleSelectNode[(Altair, NodAndShuffle), PreImaging, PreImaging]((altair, ns))
    with PreImagingNode {

    def apply(pi: PreImaging) =
      Left(new SelectMOSLongslitFpu(altair, ns, pi))

    def unapply = {
      case b: GmosNBlueprintMos if b.preImaging => PreImagingYes
      case _: GmosNBlueprintMos                 => PreImagingNo
    }
  }

  class SelectMOSLongslitFpu(altair: Altair, ignore: NodAndShuffle, pi: PreImaging)
    extends SingleSelectNode[PreImaging, GmosNMOSFpu, BlueprintGen](pi) {
    val title       = "Longslit MOS FPU"
    val description = "Select a focal plane unit."
    def choices     = GmosNMOSFpu.values.toList

    def apply(f: GmosNMOSFpu) =
      Left(new SelectDisperser(altair, GmosNBlueprintMos(_, _, _, f, ignore.toBoolean, pi.toBoolean)))

    def unapply = {
      case b: GmosNBlueprintMos => b.fpu
    }
  }

  class SelectLongslitFpu(altair: Altair, ignore: NodAndShuffle)
    extends SingleSelectNode[(Altair, NodAndShuffle), GmosNFpu, BlueprintGen]((altair, ignore)) {
    val title       = "Longslit FPU"
    val description = "Select a focal plane unit."
    def choices     = GmosNFpu.values.toList

    def apply(f: GmosNFpu) =
      Left(new SelectDisperser(altair, GmosNBlueprintLongslit(_, _, _, f)))

    def unapply = {
      case b: GmosNBlueprintLongslit => b.fpu
    }
  }

  class SelectLongslitFpuNs(altair: Altair, ignore: NodAndShuffle)
    extends SingleSelectNode[(Altair, NodAndShuffle), GmosNFpuNs, BlueprintGen]((altair, ignore)) {
    val title       = "Longslit FPU (Nod & Shuffle)"
    val description = "Select a focal plane unit."
    def choices     = GmosNFpuNs.values.toList

    def apply(f: GmosNFpuNs) =
      Left(new SelectDisperser(altair, GmosNBlueprintLongslitNs(_, _, _, f)))

    def unapply = {
      case b: GmosNBlueprintLongslitNs => b.fpu
    }
  }

  class SelectIfuFpu(altair: Altair, ignore: NodAndShuffle)
    extends SingleSelectNode[(Altair, NodAndShuffle), GmosNFpuIfu, BlueprintGen]((altair, ignore)) {
    val title       = "IFU FPU"
    val description = "Select a focal plane unit."
    def choices     = GmosNFpuIfu.values.toList

    def apply(f: GmosNFpuIfu) =
      Left(new SelectDisperser(altair, GmosNBlueprintIfu(_, _, _, f)))

    def unapply = {
      case b: GmosNBlueprintIfu => b.fpu
    }
  }

  class SelectDisperser(altair: Altair, gen: BlueprintGen)
    extends SingleSelectNode[BlueprintGen, GmosNDisperser, (Altair, BlueprintGen, GmosNDisperser)](gen) {
    val title       = "Select Disperser"
    val description = "Please select a disperser for your spectroscopic configuration."
    def choices     = GmosNDisperser.values.toList
    def apply(d: GmosNDisperser) = Left(new SelectFilter((altair, gen, d)))

    def unapply = {
      case b: GmosNBlueprintSpectrosopyBase => b.disperser
    }
  }

  class SelectFilter(state: (Altair, BlueprintGen, GmosNDisperser))
    extends SingleSelectNode[(Altair, BlueprintGen, GmosNDisperser), GmosNFilter, GmosNBlueprintSpectrosopyBase](state) {
    val title       = "Select Filter"
    val description = "Select a single filter to use with disperser %s.".format(state._3.value)
    def choices = if (state._1 == AltairNone) GmosNFilter.values.toList else GmosNFilter.ALTAIR_SPECTROSCOPY

    def apply(f: GmosNFilter) = {
      val (altair, gen, disperser) = state
      Right(gen(altair, disperser, f))
    }

    override def default = if (state._1 == AltairNone) Some(GmosNFilter.None) else None

    def unapply = {
      case b: GmosNBlueprintSpectrosopyBase => b.filter
    }
  }
}


