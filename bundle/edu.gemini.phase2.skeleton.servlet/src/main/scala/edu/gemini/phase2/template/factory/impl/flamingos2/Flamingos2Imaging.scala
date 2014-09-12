package edu.gemini.phase2.template.factory.impl.flamingos2

import edu.gemini.pot.sp._
import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.flamingos2.blueprint.SpFlamingos2BlueprintImaging
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.seqcomp.SeqRepeatDarkObs

import scala.collection.JavaConverters._

case class Flamingos2Imaging(blueprint:SpFlamingos2BlueprintImaging) extends Flamingos2Base[SpFlamingos2BlueprintImaging] {

//  **** IF INSTRUMENT MODE == IMAGING ****
//
//        INCLUDE {1,2,3}
//        FOR {1, 2, 3}:
//  	    Put FILTERS from PI into F2 ITERATOR
//  	    SET EXPOSURE TIME in Iterator/Static component:
//                  Y = 60s
//                  F1056 = 60s
//                  F1063 = 60s
//                  J-lo = 60s
//                  J = 60s
//                  H = 10s
//                  Ks = 30s
//
//          FOR EACH UNIQUE COMBINATION OF EXPOSURE TIME & READ MODE in {1,2}:
//              Add a Manual Dark to {3} with 6X Observes before the flats

  val targetGroup = Seq(1, 2, 3)
  val baselineFolder = Seq.empty
  val notes = Seq("F2 Imaging Notes")

  val science = Seq(1, 2)
  val cal     = Seq(3)

  val exposureTimes: Map[Flamingos2.Filter, Double] = {
    import Flamingos2.Filter._
    Map(H       -> 10.0,
        K_SHORT -> 30.0) withDefaultValue 60.0
  }

  def initialize(grp:ISPGroup, db:TemplateDb): Maybe[Unit] = for {
    _ <- forObservations(grp, science, forScience).right
    _ <- forObservations(grp, cal,     forCalibrations(db)).right
  } yield ()

  def forScience(obs: ISPObservation): Maybe[Unit] = for {
    _ <- obs.setFilters(blueprint.filters.asScala).right
    _ <- obs.setExposureTimes(blueprint.filters.asScala.map(exposureTimes)).right
  } yield ()

  def forCalibrations(db: TemplateDb)(obs: ISPObservation): Maybe[Unit] = for {
    _ <- setFlatFilters(obs, blueprint.filters.asScala).right
    _ <- addDarks(db.odb.getFactory, obs).right
  } yield ()

  // Update the static component and the first iterator to set the filters to use.
  private def setFlatFilters(obs: ISPObservation, lst: Iterable[Flamingos2.Filter]): Maybe[Unit] = for {
    _ <- lst.headOption.toRight("One or more filters must be specified.").right
    _ <- obs.setFilter(lst.head).right
    _ <- obs.ed.iterateFirst(Flamingos2.FILTER_PROP.getName, lst.toList).right
  } yield ()

  private def addDarks(f: ISPFactory, obs: ISPObservation): Maybe[Unit] = for {
    s <- Option(obs.getSeqComponent).toRight("Observation '%s' missing sequence component".format(obs.libraryId)).right
  } yield addDarks(f, s)

  private def addDarks(f: ISPFactory, root: ISPSeqComponent) {
    for {
      t <- blueprint.filters.asScala.map(exposureTimes).reverse.distinct
    } addDark(f, root, t)
  }

  private def addDark(f: ISPFactory, root: ISPSeqComponent, exposureTime: Double) {
    root.addSeqComponent(makeDark(f, root.getProgram, exposureTime))
  }

  private def makeDark(f: ISPFactory, p: ISPProgram, exposureTime: Double): ISPSeqComponent = {
    val sc = f.createSeqComponent(p, SeqRepeatDarkObs.SP_TYPE, null)
    val dj = sc.getDataObject.asInstanceOf[SeqRepeatDarkObs]
    dj.setStepCount(6)
    dj.setExposureTime(exposureTime)
    sc.setDataObject(dj)
    sc
  }
}
