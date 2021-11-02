package edu.gemini.phase2.template.factory.impl.visitor

import edu.gemini.model.p1.immutable.Instrument.Igrins
import edu.gemini.spModel.gemini.visitor.blueprint.SpVisitorBlueprint

case class Visitor(blueprint: SpVisitorBlueprint) extends VisitorBase {

  private val (sci, cal) = blueprint.visitorConfig.instrument match {
    case Igrins => (Seq(2), Seq(3))
    case _      => (Seq(1), Nil)
  }

  private val all = sci ++ cal

  include(all: _*) in TargetGroup

  forObs(sci: _*)(setName fromPI)
  forObs(all: _*)(setVisitorConfig fromPI)
  forObs(all: _*)(setWavelength fromPI)
  forObs(sci: _*)(setPosAngle fromPI)
}
