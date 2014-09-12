package edu.gemini.phase2.template.factory.impl.visitor

import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.gemini.visitor.blueprint.SpVisitorBlueprint
import edu.gemini.spModel.gemini.visitor.VisitorInstrument
import edu.gemini.pot.sp.{ISPGroup, ISPObservation}

trait VisitorBase extends GroupInitializer[SpVisitorBlueprint] with TemplateDsl {
  val program = "VISITOR INSTRUMENT PHASE I/II MAPPING BPS"
  val seqConfigCompType = VisitorInstrument.SP_TYPE

  implicit def pimpInst(obs: ISPObservation) = new {

    val ed = StaticObservationEditor[edu.gemini.spModel.gemini.visitor.VisitorInstrument](obs, instrumentType)

    def setName(n: String): Either[String, Unit] =
      ed.updateInstrument(_.setName(n))
  }

  // HACK: override superclass initialize to hang onto db reference
  var db: Option[TemplateDb] = None

  override def initialize(db: TemplateDb): Maybe[ISPGroup] =
    try {
      this.db = Some(db)
      super.initialize(db)
    } finally {
      this.db = None
    }

  def attempt[A](a: => A) = tryFold(a) {
    e =>
      e.printStackTrace()
      e.getMessage
  }

  // DSL Setters
  def setName = Setter[String](blueprint.name)(_.setName(_))
}
