package edu.gemini.phase2.template.factory.impl.gnirs

import edu.gemini.spModel.gemini.gnirs.{InstGNIRS, SeqConfigGNIRS}
import edu.gemini.spModel.gemini.gnirs.blueprint.SpGnirsBlueprintBase
import edu.gemini.phase2.template.factory.impl._
import edu.gemini.pot.sp.{ISPGroup, ISPObservation}
import edu.gemini.spModel.gemini.gnirs.GNIRSParams._
import edu.gemini.spModel.telescope.PosAngleConstraint

trait GnirsBase[B <: SpGnirsBlueprintBase] extends GroupInitializer[B] with TemplateDsl2[InstGNIRS] {

  val program = "GNIRS PHASE I/II MAPPING BPS"
  val seqConfigCompType = SeqConfigGNIRS.SP_TYPE
  val instCompType = InstGNIRS.SP_TYPE

  type FPU = SlitWidth

  // Seq Params
  val PARAM_FILTER = InstGNIRS.FILTER_PROP.getName
  val PARAM_FPU = InstGNIRS.SLIT_WIDTH_PROP.getName
  val PARAM_DECKER = InstGNIRS.DECKER_PROP.getName
  val PARAM_EXPOSURE_TIME = InstGNIRS.EXPOSURE_TIME_PROP.getName
  val PARAM_WAVELENGTH = InstGNIRS.CENTRAL_WAVELENGTH_PROP.getName

  // DSL Extensions
  def setDisperser = mutateStatic[Disperser](_.setDisperser(_))
  def setCrossDispersed = mutateStatic[CrossDispersed](_.setCrossDispersed(_))
  def setFilter = mutateStatic[Filter](_.setFilter(_))
  def setSlitWidth = mutateStatic[SlitWidth](_.setSlitWidth(_))
  def setPixelScale = mutateStatic[PixelScale](_.setPixelScale(_))
  def setWellDepth = mutateStatic[WellDepth](_.setWellDepth(_))
  def setFPU = mutateStatic[FPU](_.setSlitWidth(_))
  def setExposureTime = mutateStatic[Double](_.setExposureTime(_))
  def setPositionAngleConstraint = mutateStatic[PosAngleConstraint](_.setPosAngleConstraint(_))


  // When we update the decker in sequences, only do so if the FPU isn't set to ACQ
  def updateDecker(d:Decker):SequenceMutator = mapSteps { s =>
    s.get(PARAM_FPU) match {
      case Some(SlitWidth.ACQUISITION) => s
      case _ => s + (PARAM_DECKER -> d)
    }
  }

  // HACK: override superclass initialize to hang onto db reference
  var db:Option[TemplateDb] = None
  override def initialize(db:TemplateDb):Maybe[ISPGroup] =
    try {
      this.db = Some(db)
      super.initialize(db)
    } finally {
      this.db = None
    }

}
