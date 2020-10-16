package edu.gemini.phase2.template.factory.impl.flamingos2

import edu.gemini.pot.sp._
import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.flamingos2.blueprint.SpFlamingos2BlueprintImaging

import scala.collection.JavaConverters._

case class Flamingos2Imaging(blueprint:SpFlamingos2BlueprintImaging) extends Flamingos2Base[SpFlamingos2BlueprintImaging] {

//  **** IF INSTRUMENT MODE == IMAGING ****
//
//        INCLUDE {1,2,3}
//        FOR {1, 2, 3}:
//  	    Put FILTERS from PI into F2 ITERATOR
//  	    SET EXPOSURE TIME in Iterator/Static component:
//                  Y = 40s
//                  J-lo = 40s
//                  J = 40s
//                  H = 10s
//                  Ks = 15s
//                  K-red = 12s
//                  K-blue = 12s
//                  K-long = 8s
//

  val targetGroup = Seq(1, 2, 3)
  val baselineFolder = Seq.empty
  override val notes: Seq[String] = Seq(
    "F2 Imaging Notes",
    "Dithering patterns",
    "Imaging flats",
    "Imaging Baseline calibrations",
    "Detector readout modes",
    "Libraries"
  )

  val science = Seq(1, 2)
  val cal     = Seq(3)

  def exposureTimes(filter: Flamingos2.Filter): Double = {
    import Flamingos2.Filter._
    filter match {
      case H              => 10.0
      case K_SHORT        => 15.0
      case K_LONG         =>  8.0
      case K_RED | K_BLUE => 12.0
      case _              => 40.0
    }
  }

  def initialize(grp:ISPGroup, db:TemplateDb): Maybe[Unit] = for {
    _ <- forObservations(grp, science, forScience).right
    _ <- forObservations(grp, cal,     forCalibrations(db)).right
  } yield ()

  def forScience(obs: ISPObservation): Maybe[Unit] = for {
    _ <- obs.setFilters(blueprint.filters.asScala).right
    _ <- obs.setExposureTimes(blueprint.filters.asScala.map(exposureTimes)).right
  } yield ()

  def forCalibrations(db: TemplateDb)(obs: ISPObservation): Maybe[Unit] =
    setFlatFilters(obs, blueprint.filters.asScala)

  // Update the static component and the first iterator to set the filters to use.
  private def setFlatFilters(obs: ISPObservation, lst: Iterable[Flamingos2.Filter]): Maybe[Unit] = for {
    _ <- lst.headOption.toRight("One or more filters must be specified.").right
    _ <- obs.setFilter(lst.head).right
    _ <- obs.ed.iterateFirst(Flamingos2.FILTER_PROP.getName, lst.toList).right
  } yield ()
}
