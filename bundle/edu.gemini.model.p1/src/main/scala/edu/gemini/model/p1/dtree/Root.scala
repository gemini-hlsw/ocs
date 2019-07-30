package edu.gemini.model.p1.dtree

import edu.gemini.model.p1.immutable._
import Instrument._

class Root(sem:Semester) extends SingleSelectNode[Semester, Instrument, Any](sem) {

  var title = "Select Instrument"
  var description = s"The following instruments are available for semester ${sem.year}${sem.half}. See the Gemini website for information on instrument capabilities and configuration options."

  def choices = List(Alopeke, Flamingos2, GmosNorth, GmosSouth, Gnirs, Gpi, Graces, Gsaoi, Nifs, Niri, Visitor, Zorro)

  def apply(i:Instrument) = i match {
    case Alopeke    => Left(inst.Alopeke())
    case Dssi       => Left(inst.Dssi())
    case Flamingos2 => Left(inst.Flamingos2())
    case GmosSouth  => Left(inst.GmosSouth(sem))
    case GmosNorth  => Left(inst.GmosNorth())
    case Gnirs      => Left(inst.Gnirs())
    case Gpi        => Left(inst.Gpi())
    case Graces     => Left(inst.Graces())
    case Gsaoi      => Left(inst.Gsaoi())
    case Michelle   => Left(inst.Michelle())
    case Nici       => Left(inst.Nici())
    case Nifs       => Left(inst.Nifs())
    case Niri       => Left(inst.Niri())
    case Phoenix    => Left(inst.Phoenix())
    case Texes      => Left(inst.Texes())
    case Trecs      => Left(inst.Trecs())
    case Visitor    => Left(inst.Visitor())
    case Zorro      => Left(inst.Zorro())
  }

  def unapply = {
    case b:GeminiBlueprintBase => b.instrument
  }

}
