package edu.gemini.phase2.template.factory.impl.flamingos2

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.gemini.flamingos2.blueprint.SpFlamingos2BlueprintBase
import edu.gemini.phase2.template.factory.impl.{ObservationEditor, GroupInitializer}
import edu.gemini.spModel.gemini.flamingos2.{SeqConfigFlamingos2, Flamingos2}

trait Flamingos2Base[B <: SpFlamingos2BlueprintBase] extends GroupInitializer[B] {

  implicit def pimpGmosN(obs:ISPObservation) = new {

    val ed = ObservationEditor[Flamingos2](obs, instrumentType, SeqConfigFlamingos2.SP_TYPE)

    def setDisperser(d:Flamingos2.Disperser):Either[String, Unit] =
      ed.updateInstrument(_.setDisperser(d))

    def setFilter(f:Flamingos2.Filter):Either[String, Unit] =
      ed.updateInstrument(_.setFilter(f))

    def setFilters(lst:Iterable[Flamingos2.Filter]):Either[String, Unit] =
      for {
        _ <- lst.headOption.toRight("One or more filters must be specified.").right
        _ <- setFilter(lst.head).right
        _ <- ed.iterateFirst(Flamingos2.FILTER_PROP.getName, lst.toList).right
      } yield ()

    def setFpu(fpu:Flamingos2.FPUnit):Either[String, Unit] =
      ed.updateInstrument(_.setFpu(fpu))

    def setExposureTimes(lst: Iterable[Double]): Either[String, Unit] =
      for {
        _ <- lst.headOption.toRight("One or more exposure time must be specified").right
        _ <- setExposureTime(lst.head).right
        _ <- ed.iterate(Flamingos2.EXPOSURE_TIME_PROP.getName, lst.toList).right
      } yield ()

    def setExposureTime(d: Double): Either[String, Unit] =
      ed.updateInstrument(_.setExposureTime(d))
  }

  val program = "FLAMINGOS-2 PHASE I/II MAPPING BPS"

}
