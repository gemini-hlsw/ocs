package edu.gemini.phase2.template.factory.impl.graces

import edu.gemini.phase2.template.factory.impl._
import edu.gemini.shared.skyobject.Magnitude.Band
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint
import edu.gemini.pot.sp.{SPComponentType, ISPGroup}
import edu.gemini.spModel.gemini.visitor.VisitorInstrument
import edu.gemini.spModel.target.SPTarget
import edu.gemini.shared.util.immutable.ScalaConverters._

import SpGracesBlueprint.ReadMode._
import SpGracesBlueprint.FiberMode._

case class Graces(blueprint: SpGracesBlueprint, exampleTarget: Option[SPTarget]) extends GroupInitializer[SpGracesBlueprint] with TemplateDsl2[VisitorInstrument] {
  val program = "GRACES PHASE I/II MAPPING BPS"

  def instCompType: SPComponentType =
    VisitorInstrument.SP_TYPE

  def seqConfigCompType: SPComponentType =
    sys.error("No sequence component type for visitor instrument GRACES")

  // This seems to be necessary, sorry.
  var db:Option[TemplateDb] = None
  override def initialize(db:TemplateDb):Maybe[ISPGroup] =
    try {
      this.db = Some(db)
      super.initialize(db)
    } finally {
      this.db = None
    }

  // R = Phase-I target R-band magnitude
  val rMag: Option[Double] =
    for {
      t <- exampleTarget
      m <- t.getTarget.getMagnitude(Band.R).asScalaOpt
    } yield m.getBrightness

  // IF          R < 6.5 INCLUDE {1}
  // ELIF 6.5 <= R < 10  INCLUDE {2}
  // ELIF 10  <= R < 21  INCLUDE {3}
  // ELIF 21  <= R       INCLUDE {4}
  // ELSE INCLUDE {1},{2},{3},{4} # No magnitude given so include all
  val acq = rMag.map { r =>
         if (r < 6.5) List(1)
    else if (r < 10)  List(2)
    else if (r < 21)  List(3)
    else              List(4)
  } .getOrElse(List(1, 2, 3, 4))

  // IF   FIBER-MODE == 1 AND READ-MODE == Fast   INCLUDE {5}
  // ELIF FIBER-MODE == 1 AND READ-MODE == Normal INCLUDE {6}
  // ELIF FIBER-MODE == 1 AND READ-MODE == Slow   INCLUDE {7}
  // ELIF FIBER-MODE == 2 AND READ-MODE == Fast   INCLUDE {8}
  // ELIF FIBER-MODE == 2 AND READ-MODE == Normal INCLUDE {9}
  // ELIF FIBER-MODE == 2 AND READ-MODE == Slow   INCLUDE {10}
  val sci = (blueprint.getFiberMode, blueprint.getReadMode) match {
    case (ONE_FIBER, FAST)   => 5
    case (ONE_FIBER, NORMAL) => 6
    case (ONE_FIBER, SLOW)   => 7
    case (TWO_FIBER, FAST)   => 8
    case (TWO_FIBER, NORMAL) => 9
    case (TWO_FIBER, SLOW)   => 10
  }

  include(sci :: acq : _*) in TargetGroup

}
