package edu.gemini.phase2.template.factory.impl.nifs

import edu.gemini.spModel.gemini.nifs.{SeqConfigNIFS, InstNIFS}
import edu.gemini.spModel.gemini.nifs.NIFSParams._
import edu.gemini.spModel.gemini.nifs.blueprint.SpNifsBlueprintBase
import edu.gemini.phase2.template.factory.impl._
import edu.gemini.pot.sp.ISPGroup
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.core.SPProgramID

trait NifsBase[B <: SpNifsBlueprintBase] extends GroupInitializer[B] with TemplateDsl2[InstNIFS] {

  // Our program and sequence types
  val program = "NIFS PHASE I/II MAPPING BPS"
  val seqConfigCompType = SeqConfigNIFS.SP_TYPE
  val instCompType = InstNIFS.SP_TYPE

  // We need a target brightness, derived from an example target
  def tb:Option[TargetBrightness]

  // HACK: override superclass initialize to hang onto db reference.
  var db:Option[TemplateDb] = None
  override def initialize(db:TemplateDb, pid: SPProgramID):Maybe[ISPGroup] =
    try {
      this.db = Some(db)
      super.initialize(db, pid)
    } finally {
      this.db = None
    }

  //  IF BT THEN SET Read Mode = Bright Object, Exposure Time=10
  //  IF MT THEN SET Read Mode = Medium Object, Exposure Time=80
  //  IF FT OR BAT THEN SET Read Mode = Faint Object, Exposure Time=600
  lazy val defaults = tb.map {
    case BT => (ReadMode.BRIGHT_OBJECT_SPEC, 10.0)
    case MT => (ReadMode.MEDIUM_OBJECT_SPEC, 80.0)
    case _ => (ReadMode.FAINT_OBJECT_SPEC, 600.0)
  }.getOrElse((ReadMode.MEDIUM_OBJECT_SPEC, 80.0))

  // DSL EXTENSIONS

  // Simple static mutators
  def setFilter = mutateStatic[Filter](_.setFilter(_))
  def setReadMode = mutateStatic[ReadMode](_.setReadMode(_))
  def setExposure = mutateStatic[Double](_.setExposureTime(_))

  // When we set the disperser, we also set the wavelength
  def setDisperser = mutateStatic[Disperser] { (o, d) =>
    o.setDisperser(d)
    o.setCentralWavelength(d.getWavelength)
  }

  // In a couple cases we have to dig down into the iterator
  def setMaskInSecondIterator(m:Mask) =
    mutateSeq.atIndex(1)(_.map(_ + (InstNIFS.MASK_PROP.getName -> m)))

  // Set the FPM in the static component, unless it's an acq in which case set it in the 2nd iterator
  def setFpmWithAcq(acq: List[Int], m:Mask):Mutator = { o =>
    if (o.libraryId.forall(acq.map(_.toString).contains)) {
      setMaskInSecondIterator(m)(o)
    } else {
      mutateStatic[Mask](_.setMask(_))(m)(o)
    }
  }

}
