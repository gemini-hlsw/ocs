package edu.gemini.phase2.template.factory.impl

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.gemini.altair.blueprint.{SpAltairNgs, SpAltairLgs, SpAltair}
import edu.gemini.spModel.gemini.altair.{AltairParams, InstAltair}
import edu.gemini.spModel.gemini.altair.AltairParams.{Mode, FieldLens}

trait AltairSupport { this:TemplateDsl =>

  def db:Option[TemplateDb]

  /** Adds an Altair component. */
  def AltairSetter(a: => SpAltair) = Setter(a) {(o:ISPObservation, a:SpAltair) =>
    attempt {

      // Find our Altair mode
      import AltairParams.Mode
      val mode:Option[Mode] = a match {
        case lgs:SpAltairLgs => if (lgs.usePwfs1) Some(Mode.LGS_P1) else Some(Mode.LGS)
        case ngs:SpAltairNgs => if (ngs.fieldLens == FieldLens.IN) Some(Mode.NGS_FL) else Some(Mode.NGS)
        case _ => None
      }

      // And now construct and add an Altair component with that mode
      mode.foreach {m =>
        val oc = db.get.odb.getFactory.createObsComponent(o.getProgram, InstAltair.SP_TYPE, null)
        val altair = new InstAltair
        altair.setMode(m)
        oc.setDataObject(altair)
        o.addObsComponent(oc)
      }

    }
  }

  implicit def pimpSpAltair(a:SpAltair) = new {
    def mode = a match {
      case lgs:SpAltairLgs => if (lgs.usePwfs1) Some(Mode.LGS_P1) else Some(Mode.LGS)
      case ngs:SpAltairNgs => if (ngs.fieldLens == FieldLens.IN) Some(Mode.NGS_FL) else Some(Mode.NGS)
      case _ => None
    }
  }
  
  implicit def pimpAltairMode(m:Mode) = new {
    def isNGS = m == Mode.NGS || m == Mode.NGS_FL
    def isLGS = m == Mode.LGS || m == Mode.LGS_P1
  }
  
  def addAltair(m:Mode)(o:ISPObservation):Maybe[Unit] = {
    val oc = db.get.odb.getFactory.createObsComponent(o.getProgram, InstAltair.SP_TYPE, null)
    val altair = new InstAltair
    altair.setMode(m)
    oc.setDataObject(altair)
    o.addObsComponent(oc)
    Right(())
  }

}
