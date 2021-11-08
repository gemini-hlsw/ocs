package edu.gemini.phase2.template.factory.impl.graces

import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint
import edu.gemini.pot.sp.{SPComponentType, ISPGroup}
import edu.gemini.spModel.gemini.visitor.VisitorInstrument
import edu.gemini.spModel.target.SPTarget

import SpGracesBlueprint.ReadMode._
import SpGracesBlueprint.FiberMode._
import edu.gemini.spModel.core.SPProgramID

case class Graces(blueprint: SpGracesBlueprint, exampleTarget: Option[SPTarget]) extends GroupInitializer[SpGracesBlueprint] with TemplateDsl2[VisitorInstrument] {
  val program = "GRACES PHASE I/II MAPPING BPS"

  def instCompType: SPComponentType =
    VisitorInstrument.SP_TYPE

  def seqConfigCompType: SPComponentType =
    sys.error("No sequence component type for visitor instrument GRACES")

  // This seems to be necessary, sorry.
  var db:Option[TemplateDb] = None
  override def initialize(db:TemplateDb, pid: SPProgramID):Maybe[ISPGroup] =
    try {
      this.db = Some(db)
      super.initialize(db, pid)
    } finally {
      this.db = None
    }

  // R = Phase-I target R-band or V-band magnitude
  val rMag: Option[Double] =
    for {
      t <- exampleTarget
      m <- t.getMagnitude(MagnitudeBand.R) orElse
           t.getMagnitude(MagnitudeBand.V)
    } yield m.value

  //  IF FIBER-MODE == 1 AND (READ-MODE == Normal OR READ-MODE == Fast):
  //    IF   R> 10 INCLUDE {1}
  //  ELIF R<=10 INCLUDE {2}
  //  ELSE       INCLUDE {1,2} # Unknown brightness, so include both
  //
  //  ELIF FIBER-MODE == 1 AND READ-MODE == Slow:
  //    IF   R> 10 INCLUDE {3}
  //  ELIF R<=10 INCLUDE {4}
  //  ELSE       INCLUDE {3,4}
  //
  //  ELIF FIBER-MODE == 2 AND (READ-MODE == Normal OR READ-MODE == Fast):
  //    IF   R> 10 INCLUDE {5}
  //  ELIF R<=10 INCLUDE {6}
  //  ELSE       INCLUDE {5,6}
  //
  //  ELIF FIBER-MODE == 2 AND READ-MODE == Slow:
  //    IF   R> 10 INCLUDE {7}
  //  ELIF R<=10 INCLUDE {8}
  //  ELSE       INCLUDE {7,8}

  def select(gt10: Int, lte10: Int): List[Int] =
    rMag.map(m => if (m > 10) List(gt10) else List(lte10)).getOrElse(List(gt10, lte10))

  val sci = (blueprint.getFiberMode, blueprint.getReadMode) match {

    case (ONE_FIBER, NORMAL | FAST) => select(1, 2)
    case (ONE_FIBER, SLOW)          => select(3, 4)

    case (TWO_FIBER, NORMAL | FAST) => select(5, 6)
    case (TWO_FIBER, SLOW)          => select(7, 8)

  }

  include(sci : _*) in TargetGroup

  // these should be in top level, but will appear in each template group for now
  addNote("How to prepare your program", "GRACES set-up") in TopLevel

}
