package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.gemini.altair.{AltairParams, InstAltair}
import edu.gemini.spModel.gemini.gmos.{GmosCommonType, SeqConfigGmosNorth, GmosNorthType, InstGmosNorth}
import edu.gemini.spModel.gemini.gmos.GmosCommonType.CustomSlitWidth
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosNBlueprintBase
import edu.gemini.spModel.gemini.altair.blueprint.{SpAltairNgs, SpAltairLgs, SpAltairNone}
import edu.gemini.spModel.obs.ObsClassService
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.obscomp.InstConstants
import edu.gemini.spModel.gemini.altair.AltairParams.Mode
import edu.gemini.spModel.rich.pot.sp.obsWrapper

trait GmosNBase[B <: SpGmosNBlueprintBase] extends GmosBase[B] {

  implicit def pimpGmosN(obs:ISPObservation) = new {

    val ed = ObservationEditor[InstGmosNorth](obs, instrumentType, SeqConfigGmosNorth.SP_TYPE)

    def setDisperser(d:GmosNorthType.DisperserNorth):Either[String, Unit] =
      ed.updateInstrument(_.setDisperser(d))

    def setFilter(f:GmosNorthType.FilterNorth):Either[String, Unit] =
      ed.updateInstrument(_.setFilter(f))

    def setExposureTime(f:Double):Either[String, Unit] =
      ed.updateInstrument(_.setExposureTime(f))

    def setFilters(lst:Iterable[GmosNorthType.FilterNorth]):Either[String, Unit] =
      for {
        _ <- lst.headOption.toRight("One or more filters must be specified.").right
        _ <- setFilter(lst.head).right
        _ <- ed.iterate(InstGmosNorth.FILTER_PROP.getName, lst.toList).right
      } yield ()

    def setExposures(lst:Iterable[Double]):Either[String, Unit] =
      for {
        _ <- lst.headOption.toRight("One or more exposures must be specified.").right
        _ <- setExposureTime(lst.head).right
        _ <- ed.iterate(InstConstants.EXPOSURE_TIME_PROP, lst.toList).right
      } yield ()

    def setFpu(fpu:GmosNorthType.FPUnitNorth):Either[String, Unit] =
      ed.updateInstrument(_.setFPUnit(fpu))

    def setDefaultCustomMaskName: Either[String, Unit] =
      ed.updateInstrument(_.setFPUnitCustomMask(defaultCustomMaskName(obs)))

    def getFpu:Either[String, GmosNorthType.FPUnitNorth] = for {
      i <- ed.instrumentDataObject.right
    } yield i.getFPUnit

    def fpuToCustomSlitWidth(fpu: GmosNorthType.FPUnitNorth): CustomSlitWidth =
      Option(fpu.getWidth).flatMap { slit =>
        CustomSlitWidth.values().find { csw =>
          (csw.getWidth - slit).abs < 0.00001
        }
      }.getOrElse(CustomSlitWidth.OTHER) // old templates don't require you to pick a slit width

    def setCustomSlitWidth(fpu: GmosNorthType.FPUnitNorth): Either[String, Unit] = {
      val csw = fpuToCustomSlitWidth(fpu)
      ed.updateInstrument(_.setCustomSlitWidth(csw))
    }

    def setDisperserLambda(d:Double):Either[String, Unit] =
      ed.updateInstrument(_.setDisperserLambda(d))

    def setXbin(b: GmosCommonType.Binning): Either[String, Unit] =
      ed.updateInstrument(_.setCcdXBinning(b))

    def setYbin(b: GmosCommonType.Binning): Either[String, Unit] =
      ed.updateInstrument(_.setCcdYBinning(b))

    def setXyBin(x: GmosCommonType.Binning, y: GmosCommonType.Binning): Either[String, Unit] =
      ed.updateInstrument { i =>
        i.setCcdXBinning(x)
        i.setCcdYBinning(y)
      }

    def ifAo(op: ISPObservation => Maybe[Unit]): Maybe[Unit] =
      if (blueprint.altair.useAo()) op(obs) else Right(())
  }

  val closestGRIZ = {
    import GmosNorthType.FilterNorth._
    closestFilter(g_G0301, r_G0303, i_G0302, z_G0304) _
  }

  val program = "GMOS-N PHASE I/II MAPPING BPS"

  def withAoUpdate(db: TemplateDb)(ini: ISPObservation => Maybe[Unit]): ISPObservation => Maybe[Unit] = {
//    # In NGS mode the science and standards use the Altair guide mode set in the Phase-I (with or without the Field Lens).
//    # In LGS mode the science uses the mode set in the Phase-I (AOWFS, PWFS1, or OIWFS) but the standards use NGS + Field Lens.
//    # Altair components are not added to daytime calibrations.
//    IF AO in PI != None AND CLASS != Daytime Calibration:
//       ADD Altair Adaptive Optics component
//       IF AO in PI == "Altair Natural Guidestar":
//          SET Guide Star Type = "Natural Guide Star"
//       ELIF AO in PI == "Altair Natural Guidestar w/ Field Lens":
//          SET Guide Star Type = "Natural Guide Star with Field Lens"
//       ELIF AO in PI includes "Laser" AND CLASS != ACQ AND CLASS != SCI:  # standards
//          SET Guide Star Type = "Natural Guide Star with Field Lens"
//       ELIF AO in PI == "Altair Laser Guide Star":
//          SET Guide Star Type = "Laser Guide Star + AOWFS"
//       ELIF AO in PI == "Altair Laser Guide Star w/ PWFS1":
//          SET Guide Star Type = "Laser Guide Star + PWFS1"
//       ELIF AO in PI == "Altair Laser Guide Star w/ OIWFS":
//          SET Guide Star Type = "Laser Guide Star + OIWFS"

    // ripped from TemplateDb2 ... which Gmos isn't using
    def addAltair(o: ISPObservation, m: Mode): Maybe[Unit] =
      o.findObsComponentByType(InstAltair.SP_TYPE) match {

        // Ok sometimes there's already a altair component.
        case Some(oc) =>
          val altair = oc.getDataObject.asInstanceOf[InstAltair]
          altair.setMode(m)
          Right(oc.setDataObject(altair))

        case None =>
          val oc = db.odb.getFactory.createObsComponent(o.getProgram, InstAltair.SP_TYPE, null)
          val altair = new InstAltair
          altair.setMode(m)
          oc.setDataObject(altair)
          Right(o.addObsComponent(oc))
      }


    def addAo(o: ISPObservation): Maybe[Unit] = {
      val obsClass = ObsClassService.lookupObsClass(o)
      if ((obsClass == ObsClass.DAY_CAL) || !blueprint.altair.useAo()) Right(())
      else blueprint.altair match {
             case n: SpAltairNgs  => addAltair(o, n.mode)
             case l: SpAltairLgs  =>
               if ((obsClass == ObsClass.ACQ) || (obsClass == ObsClass.SCIENCE)) addAltair(o, l.mode)
               else addAltair(o, AltairParams.Mode.NGS_FL)
             case _: SpAltairNone => Right(()) // do nothing, shouldn't happen since useAo() was true
          }
    }

    (o: ISPObservation) =>
      for {
        _ <- ini(o).right
        _ <- addAo(o).right
      } yield ()
  }
}

object GmosNBase {
  trait WithTargetFolder[B <: SpGmosNBlueprintBase] extends GmosNBase[B] with TargetFolder[B]
}