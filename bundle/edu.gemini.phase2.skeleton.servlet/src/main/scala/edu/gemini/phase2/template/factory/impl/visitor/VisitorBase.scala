package edu.gemini.phase2.template.factory.impl.visitor

import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.gemini.visitor.blueprint.SpVisitorBlueprint
import edu.gemini.spModel.gemini.visitor.{VisitorConfig, VisitorInstrument}
import edu.gemini.pot.sp.{ISPGroup, ISPObservation, SPComponentType}
import edu.gemini.spModel.core.SPProgramID

//noinspection MutatorLikeMethodIsParameterless
trait VisitorBase extends GroupInitializer[SpVisitorBlueprint] with TemplateDsl {

  override val program: String =
    "VISITOR INSTRUMENT PHASE I/II MAPPING BPS"

  override val seqConfigCompType: SPComponentType =
    VisitorInstrument.SP_TYPE

  implicit def pimpInst(obs: ISPObservation) = new {

    val ed: StaticObservationEditor[VisitorInstrument] =
      StaticObservationEditor[VisitorInstrument](obs, instrumentType)

    def setName(n: String): Either[String, Unit] =
      ed.updateInstrument(_.setName(n))

    def setVisitorConfig(c: VisitorConfig): Either[String, Unit] =
      ed.updateInstrument(_.setVisitorConfig(c))

    def setWavelength(microns: Double): Either[String, Unit] =
      ed.updateInstrument(_.setWavelength(microns))

    def setPosAngle(degrees: Double): Either[String, Unit] =
      ed.updateInstrument(_.setPosAngleDegrees(degrees))

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

  def attempt[A](a: => A): Either[String, A] =
    tryFold(a) { e =>
      e.printStackTrace()
      e.getMessage
    }

  // DSL Setters
  def setName: Setter[String] =
    Setter[String](blueprint.name)(_.setName(_))

  def setVisitorConfig: Setter[VisitorConfig] =
    Setter[VisitorConfig](blueprint.visitorConfig)(_.setVisitorConfig(_))

  def visitorConfig: VisitorConfig =
    blueprint.visitorConfig

  override def notes: List[String] =
    visitorConfig.noteTitles

  def setWavelength: Setter[Double] =
    Setter[Double](visitorConfig.wavelength.toMicrons)(_.setWavelength(_))

  def setPosAngle: Setter[Double] =
    Setter[Double](visitorConfig.positionAngle.toDegrees)(_.setPosAngle(_))
}
