package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

import edu.gemini.model.p1.dtree.inst.GmosCommon._

object GmosSouth {

  import InstrumentMode._

  // Constructor for initial node
  def apply(s: Semester) = new SelectMode

  class SelectMode extends SingleSelectNode[Unit, InstrumentMode, Unit](()) with InstrumentModeNode {
    def instrumentName = "GMOS"

    def apply(m: InstrumentMode) = m match {
      case Imaging      => Left(new SelectImagingFilters)
      case Spectroscopy => Left(new SelectSpecMode)
    }

    def unapply = {
      case _: GmosSBlueprintIfu        => Spectroscopy
      case _: GmosSBlueprintIfuNs      => Spectroscopy
      case _: GmosSBlueprintImaging    => Imaging
      case _: GmosSBlueprintLongslit   => Spectroscopy
      case _: GmosSBlueprintLongslitNs => Spectroscopy
      case _: GmosSBlueprintMos        => Spectroscopy
    }
  }

  // Imaging Terminal Node
  class SelectImagingFilters extends MultiSelectNode[Unit, GmosSFilter, GmosSBlueprintImaging](()) {
    val title       = "Imaging Filters"
    val description = "Select at least one filter for your imaging configuration. More than one filter may be selected."
    def choices = GmosSFilter.IMAGING
    def apply(fs: List[GmosSFilter]) = Right(GmosSBlueprintImaging(fs))

    def unapply = {
      case b: GmosSBlueprintImaging => b.filters
    }
  }

  // SPECTROSCOPY NODES ----------

  object SpecMode extends Enumeration {
    val Longslit   = Value("Longslit")
    val LongslitNS = Value("Longslit Nod & Shuffle")
    val Mos        = Value("MOS")
    val MosNS      = Value("MOS Nod & Shuffle")
    val Ifu        = Value("IFU")
    val IfuNS      = Value("IFU Nod & Shuffle")
    type SpecMode = Value
  }
  import SpecMode._

  class SelectSpecMode extends SingleSelectNode[Unit, SpecMode, NodAndShuffle](()) {
    val title       = "Select Spectroscopy Mode"
    val description = "Select a spectroscopy mode. See the Gemini website for more information on supported modes."
    def choices     = SpecMode.values.toList

    def apply(mode: SpecMode) = mode match {
        case Mos        => Left(new SelectMosPreImaging(NsNo))
        case MosNS      => Left(new SelectMosPreImaging(NsYes))
        case Longslit   => Left(new SelectLongslitFpu(NsNo))
        case LongslitNS => Left(new SelectLongslitFpuNs(NsYes))
        case Ifu        => Left(new SelectIfuFpu(NsNo))
        case IfuNS      => Left(new SelectIfuFpuNs(NsYes))
      }

    def unapply = {
      case b: GmosSBlueprintIfu                    => Ifu
      case b: GmosSBlueprintIfuNs                  => IfuNS
      case b: GmosSBlueprintLongslit               => Longslit
      case b: GmosSBlueprintLongslitNs             => LongslitNS
      case b: GmosSBlueprintMos if b.nodAndShuffle => MosNS
      case b: GmosSBlueprintMos                    => Mos
    }
  }

  type BlueprintGen = ((GmosSDisperser, GmosSFilter) => GmosSBlueprintSpectrosopyBase)

  class SelectMosPreImaging(ns: NodAndShuffle)
    extends SingleSelectNode[NodAndShuffle, PreImaging, PreImaging](ns)
    with PreImagingNode {

    def apply(pi: PreImaging) =
      Left(new SelectMOSLongslitFpu(ns, pi))

    def unapply = {
      case b: GmosSBlueprintMos if b.preImaging => PreImagingYes
      case _: GmosSBlueprintMos                 => PreImagingNo
    }
  }

  class SelectMOSLongslitFpu(ignore: NodAndShuffle, pi: PreImaging)
    extends SingleSelectNode[PreImaging, GmosSMOSFpu, BlueprintGen](pi) {
    val title       = "Longslit MOS FPU"
    val description = "Select a focal plane unit."
    def choices     = GmosSMOSFpu.values.toList

    def apply(f: GmosSMOSFpu) =
      Left(new SelectDisperser(GmosSBlueprintMos(_, _, f, ignore.toBoolean, pi.toBoolean)))

    def unapply = {
      case b: GmosSBlueprintMos => b.fpu
    }
  }

  class SelectLongslitFpu(ignore: NodAndShuffle)
    extends SingleSelectNode[NodAndShuffle, GmosSFpu, BlueprintGen](ignore) {
    val title       = "Longslit FPU"
    val description = "Select a focal plane unit."
    def choices     = GmosSFpu.values.toList

    def apply(f: GmosSFpu) =
      Left(new SelectDisperser(GmosSBlueprintLongslit(_, _, f)))

    def unapply = {
      case b: GmosSBlueprintLongslit => b.fpu
    }
  }

  class SelectLongslitFpuNs(ignore: NodAndShuffle)
    extends SingleSelectNode[NodAndShuffle, GmosSFpuNs, BlueprintGen](ignore) {
    val title       = "Longslit FPU (Nod & Shuffle)"
    val description = "Select a focal plane unit."
    def choices     = GmosSFpuNs.values.toList

    def apply(f: GmosSFpuNs) =
      Left(new SelectDisperser(GmosSBlueprintLongslitNs(_, _, f)))

    def unapply = {
      case b: GmosSBlueprintLongslitNs => b.fpu
    }
  }

  class SelectIfuFpu(ignore: NodAndShuffle)
    extends SingleSelectNode[NodAndShuffle, GmosSFpuIfu, BlueprintGen](ignore) {
    val title       = "IFU FPU"
    val description = "Select a focal plane unit."
    def choices     = GmosSFpuIfu.values.toList

    def apply(f: GmosSFpuIfu) =
      Left(new SelectDisperser(GmosSBlueprintIfu(_, _, f)))

    def unapply = {
      case b: GmosSBlueprintIfu => b.fpu
    }
  }

  class SelectIfuFpuNs(ignore: NodAndShuffle)
    extends SingleSelectNode[NodAndShuffle, GmosSFpuIfuNs, BlueprintGen](ignore) {
    val title       = "IFU FPU (Nod & Shuffle)"
    val description = "Select a focal plane unit."
    def choices     = GmosSFpuIfuNs.values.toList

    def apply(f: GmosSFpuIfuNs) =
      Left(new SelectDisperser(GmosSBlueprintIfuNs(_, _, f)))

    def unapply = {
      case b: GmosSBlueprintIfuNs => b.fpu
    }
  }

  class SelectDisperser(gen: BlueprintGen)
    extends SingleSelectNode[BlueprintGen, GmosSDisperser, (BlueprintGen, GmosSDisperser)](gen) {
    val title       = "Select Disperser"
    val description = "Please select a disperser for your spectroscopic configuration."
    def choices     = GmosSDisperser.values.toList.filterNot(_ == GmosSDisperser.B600)
    def apply(d: GmosSDisperser) = Left(new SelectFilter((gen, d)))

    def unapply = {
      case b: GmosSBlueprintSpectrosopyBase => b.disperser
    }
  }

  class SelectFilter(state: (BlueprintGen, GmosSDisperser))
    extends SingleSelectNode[(BlueprintGen, GmosSDisperser), GmosSFilter, GmosSBlueprintSpectrosopyBase](state) {
    val title       = "Select Filter"
    val description = "Select a single filter to use with disperser %s.".format(state._2.value)
    def choices = GmosSFilter.values.toList

    override def default = Some(GmosSFilter.None)
    
    def apply(f: GmosSFilter) = {
      val (gen, disperser) = state
      Right(gen(disperser, f))
    }

    def unapply = {
      case b: GmosSBlueprintSpectrosopyBase => b.filter
    }
  }
}



