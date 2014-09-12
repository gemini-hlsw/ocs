package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Nici {
  object NiciMode extends Enumeration {
    val Standard      = Value("Standard (Non-Coronagraphic)")
    val Coronagraphic = Value("Coronagraphic")
    type NiciMode = Value
  }

  import NiciMode._

  def apply() = new ModeNode

  // A function that takes a dichroic, a list of red filters, a list of blue
  // filters and returns a NICI blueprint.
  type BlueprintGen = ((NiciDichroic, List[NiciRedFilter], List[NiciBlueFilter]) => NiciBlueprintBase)

  class ModeNode extends SingleSelectNode[Unit, NiciMode, Unit](()) {
    val title       = "Select Instrument Mode"
    val description = "NICI can be used in either standard and coronagraphic mode."
    def choices     = NiciMode.values.toList

    def apply(m: NiciMode) = m match {
      case Standard      => Left(new StandardDichroicNode)
      case Coronagraphic => Left(new FpmNode)
    }

    def unapply = {
      case _: NiciBlueprintStandard      => Standard
      case _: NiciBlueprintCoronagraphic => Coronagraphic
    }
  }

  class FpmNode extends SingleSelectNode[Unit, NiciFpm, BlueprintGen](()) {
    val title       = "Focal Plane Mask"
    val description = "Select the focal plane mask for your coronagraphic configuration."
    def choices     = NiciFpm.values.toList

    def apply(fpm: NiciFpm) = Left(new DichroicNode(NiciBlueprintCoronagraphic(fpm, _, _, _)))

    def unapply = {
      case b: NiciBlueprintCoronagraphic => b.fpm
    }
  }

//  type RedInput  = (BlueprintGen, NiciDichroic)
  type NiciData = (BlueprintGen, NiciDichroic, List[NiciRedFilter])

  import NiciDichroic.{OPEN, MIRROR}

  // ModeNode needs to output a Node with Unit input so this is just a little
  // class to adapt new DichroicNode(NiciBlueprintStandard(_,_,_)) to that.
  class StandardDichroicNode extends SingleSelectNode[Unit, NiciDichroic, NiciData](()) {
    val delegate    = new DichroicNode(NiciBlueprintStandard(_,_,_))
    val title       = delegate.title
    val description = delegate.description
    def choices     = delegate.choices
    def apply(d: NiciDichroic) = delegate(d)
    def unapply     = delegate.unapply
  }

//  When selecting MIRROR, it should not ask for red channel filter.
//  When selecting OPEN, it should not ask for blue channel filter.

  class DichroicNode(gen: BlueprintGen) extends SingleSelectNode[BlueprintGen, NiciDichroic, NiciData](gen) {
    val title       = "Dichroic"
    val description = "Select the dichroic."
    def choices     = NiciDichroic.values.toList

    def apply(d: NiciDichroic) = d match {
      case MIRROR => Left(new MultiBlueNode((gen, d, Nil)))
      case OPEN   => Left(new MultiRedNode((gen, d, Nil)))
      case _      => Left(new SingleRedNode((gen, d, Nil)))
    }

    def unapply = {
      case b: NiciBlueprintBase => b.dichroic
    }
  }


  class MultiRedNode(s: NiciData) extends MultiSelectNode[NiciData, NiciRedFilter, NiciBlueprintBase](s) {
    val title       = "Red Channel Filter"
    val description = "Choose one or more red channel filters."
    def choices     = NiciRedFilter.values.toList

    def apply(fs: List[NiciRedFilter]) = {
      val (gen, dichroic, _) = s
      Right(gen(dichroic, fs, Nil))
    }

    def unapply = {
      case b: NiciBlueprintBase => b.redFilters
    }
  }

  class SingleRedNode(s: NiciData) extends SingleSelectNode[NiciData, NiciRedFilter, NiciData](s) {
    val title       = "Red Channel Filter"
    val description = "Choose a red channel filter."
    def choices     = NiciRedFilter.values.toList

    def apply(f: NiciRedFilter) = {
      val (gen, dichroic, _) = s
      Left(new SingleBlueNode((gen, dichroic, List(f))))
    }

    def unapply = {
      case b: NiciBlueprintBase => b.redFilters.head
    }
  }

  class MultiBlueNode(s: NiciData) extends MultiSelectNode[NiciData, NiciBlueFilter, NiciBlueprintBase](s) {
    val title       = "Blue Channel Filter"
    val description = "Choose one or more blue channel filters."
    def choices     = NiciBlueFilter.values.toList

    def apply(fs: List[NiciBlueFilter]) = {
      val (gen, dichroic, reds) = s
      Right(gen(dichroic, reds, fs))
    }

    def unapply = {
      case b: NiciBlueprintBase => b.blueFilters
    }
  }

  class SingleBlueNode(s: NiciData) extends SingleSelectNode[NiciData, NiciBlueFilter, NiciBlueprintBase](s) {
    val title       = "Blue Channel Filter"
    val description = "Choose a blue channel filter."
    def choices     = NiciBlueFilter.values.toList

    def apply(f: NiciBlueFilter) = {
      val (gen, dichroic, reds) = s
      Right(gen(dichroic, reds, List(f)))
    }

    def unapply = {
      case b: NiciBlueprintBase => b.blueFilters.head
    }
  }
}