package edu.gemini.model.p1.dtree

import edu.gemini.model.p1.immutable._
import Instrument._

class Root(sem:Semester) extends SingleSelectNode[Semester, Instrument, Any](sem) {

  var title = "Select Instrument"
  var description = s"The following instruments are available for semester ${sem.year}${sem.half}. See the Gemini website for information on instrument capabilities and configuration options."

  def choices = List(Flamingos2, Dssi, GmosNorth, GmosSouth, Gnirs, Gpi, Graces, Gsaoi, Nifs, Niri, Phoenix, Texes, Visitor)

  def apply(i:Instrument) = i match {
    case Flamingos2 => Left(inst.Flamingos2())
    case GmosSouth  => Left(inst.GmosSouth(sem))
    case GmosNorth  => Left(inst.GmosNorth())
    case Gnirs      => Left(inst.Gnirs())
    case Gsaoi      => Left(inst.Gsaoi())
    case Graces     => Left(inst.Graces())
    case Gpi        => Left(inst.Gpi())
    case Michelle   => Left(inst.Michelle())
    case Nici       => Left(inst.Nici())
    case Nifs       => Left(inst.Nifs())
    case Niri       => Left(inst.Niri())
    case Phoenix    => Left(inst.Phoenix())
    case Dssi       => Left(inst.Dssi())
    case Texes      => Left(inst.Texes())
    case Trecs      => Left(inst.Trecs())
    case Visitor    => Left(inst.Visitor())
  }

  def unapply = {
    case b:GeminiBlueprintBase => b.instrument
  }

}
