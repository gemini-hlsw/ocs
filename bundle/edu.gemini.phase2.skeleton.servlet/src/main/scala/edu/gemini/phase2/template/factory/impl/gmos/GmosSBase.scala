package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.template.SpBlueprint
import edu.gemini.phase2.template.factory.impl.ObservationEditor
import edu.gemini.spModel.gemini.gmos.{SeqConfigGmosSouth, GmosSouthType, InstGmosSouth}
import edu.gemini.spModel.obscomp.InstConstants
import edu.gemini.spModel.gemini.gmos.GmosCommonType.CustomSlitWidth
import edu.gemini.phase2.template.factory.impl.TargetFolder
import edu.gemini.spModel.core.SPProgramID


trait GmosSBase[B <: SpBlueprint] extends GmosBase[B] {

  implicit def pimpGmosS(obs:ISPObservation) = new {

    val ed = ObservationEditor[InstGmosSouth](obs, instrumentType, SeqConfigGmosSouth.SP_TYPE)

    def setDisperser(d:GmosSouthType.DisperserSouth):Either[String, Unit] =
      ed.updateInstrument(_.setDisperser(d))

    def setExposureTime(f:Double):Either[String, Unit] =
      ed.updateInstrument(_.setExposureTime(f))

    def setDisperserLambda(f:Double):Either[String, Unit] =
      ed.updateInstrument(_.setDisperserLambda(f))

    def setFilter(f:GmosSouthType.FilterSouth):Either[String, Unit] =
      ed.updateInstrument(_.setFilter(f))

    def setFilters(lst:Iterable[GmosSouthType.FilterSouth]):Either[String, Unit] =
      for {
        _ <- lst.headOption.toRight("One or more filters must be specified.").right
        _ <- setFilter(lst.head).right
        _ <- ed.iterate(InstGmosSouth.FILTER_PROP.getName, lst.toList).right
      } yield ()

    def setExposures(lst:Iterable[Double]):Either[String, Unit] =
      for {
        _ <- lst.headOption.toRight("One or more exposures must be specified.").right
        _ <- setExposureTime(lst.head).right
        _ <- ed.iterate(InstConstants.EXPOSURE_TIME_PROP, lst.toList).right
      } yield ()

    def setFpu(fpu:GmosSouthType.FPUnitSouth):Either[String, Unit] =
      ed.updateInstrument(_.setFPUnit(fpu))

    def setDefaultCustomMaskName(pid: SPProgramID): Either[String, Unit] =
      ed.updateInstrument(_.setFPUnitCustomMask(defaultCustomMaskName(pid)))

    def getFpu:Either[String, GmosSouthType.FPUnitSouth] = for {
      i <- ed.instrumentDataObject.right
    } yield i.getFPUnit

    def fpuToCustomSlitWidth(fpu: GmosSouthType.FPUnitSouth): CustomSlitWidth =
      Option(fpu.getWidth).flatMap { slit =>
        CustomSlitWidth.values().find { csw =>
          (csw.getWidth - slit).abs < 0.00001
        }
      }.getOrElse(CustomSlitWidth.OTHER) // old templates don't require you to pick a slit width

    def setCustomSlitWidth(fpu: GmosSouthType.FPUnitSouth): Either[String, Unit] = {
      val csw = fpuToCustomSlitWidth(fpu)
      ed.updateInstrument(_.setCustomSlitWidth(csw))
    }
  }

  val closestUGRIZ = {
    import GmosSouthType.FilterSouth._
    closestFilter(u_G0332, g_G0325, r_G0326, i_G0327, z_G0328) _
  }

  val program = "GMOS-S PHASE I/II MAPPING BPS"

}

object GmosSBase {
  trait WithTargetFolder[B <: SpBlueprint] extends GmosSBase[B] with TargetFolder[B]
}