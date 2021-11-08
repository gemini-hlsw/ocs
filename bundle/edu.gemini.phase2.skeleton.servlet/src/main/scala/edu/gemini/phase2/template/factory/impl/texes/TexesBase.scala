package edu.gemini.phase2.template.factory.impl.texes

import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.gemini.texes.blueprint.SpTexesBlueprint
import edu.gemini.spModel.gemini.texes.{TexesParams, InstTexes}
import scala.Some
import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.core.SPProgramID

trait TexesBase extends GroupInitializer[SpTexesBlueprint] with TemplateDsl {
  val program = "TEXES PHASE I/II MAPPING BPS"
  val seqConfigCompType = InstTexes.SP_TYPE

  implicit def pimpInst(obs: ISPObservation) = new {

    val ed = StaticObservationEditor[edu.gemini.spModel.gemini.texes.InstTexes](obs, instrumentType)

    def setDisperser(d: TexesParams.Disperser): Either[String, Unit] =
      ed.updateInstrument(_.setDisperser(d))
  }

  // HACK: override superclass initialize to hang onto db reference
  var db: Option[TemplateDb] = None

  override def initialize(db: TemplateDb, pid: SPProgramID): Maybe[ISPGroup] =
    try {
      this.db = Some(db)
      super.initialize(db, pid)
    } finally {
      this.db = None
    }

  def attempt[A](a: => A) = tryFold(a) {
    e =>
      e.printStackTrace()
      e.getMessage
  }

   // DSL Setters
  def setDisperser = Setter[TexesParams.Disperser](blueprint.disperser)(_.setDisperser(_))
}
