package edu.gemini.phase2.template.factory.impl.flamingos2

import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.flamingos2.blueprint.SpFlamingos2BlueprintLongslit
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.core.MagnitudeBand.H

import scala.collection.JavaConverters._
import edu.gemini.spModel.gemini.flamingos2.Flamingos2

case class Flamingos2Longslit(blueprint:SpFlamingos2BlueprintLongslit, exampleTarget: Option[SPTarget]) extends Flamingos2Base[SpFlamingos2BlueprintLongslit] {

//
//  **** IF INSTRUMENT MODE == SPECTROSCOPY ***
//
//  IF SPECTROSCOPY MODE = LONGSLIT
//
//    INCLUDE {11,12}                           # Telluric std
//
//          IF TARGET H-MAGNITUDE <= 12 INCLUDE {13}  # Bright, no sky subtraction
//          IF TARGET H-MAGNITUDE  > 12 INCLUDE {14}  # Faint, with sky subtraction
//          ELSE INCLUDE {6,7}                      # Unknown mag so include both acq templates
//
//    INCLUDE {15,16}                           # Science
//    INCLUDE {17,18}                           # Telluric std
//
//    FOR {11,12,13,14,15,16,17,18}:            # All
//        SET CONDITIONS FROM PI
//        SET FPU FROM PI
//
//        FOR {12,15,16,18}:                        # Science and Tellurics
//        SET DISPERSER FROM PI
//            Put FILTERS from PI into F2 ITERATOR
//
//        FOR {21,22,23,24,25,26,27}:
//            SET CONDITIONS FROM PI

  val acq = exampleTarget.flatMap(t => t.getMagnitude(H)).map(_.value) match {
    case Some(h) if h <= 12 => Seq(13)
    case Some(h) if h >  12 => Seq(14)
    case _                  => Seq(6,7)
  }

  val targetGroup = Seq(11,12) ++ acq ++ Seq(15,16,17,18)
  val baselineFolder = Seq.empty
  val notes = Seq("F2 Long-Slit Notes", "Repeats contain the ABBA offsets", "Use same PA for science and Telluric")

  val scienceAndTellurics = Seq(12,15,16,18)

  def initialize(grp:ISPGroup, db:TemplateDb): Either[String, Unit] = for {
      _ <- forObservations(grp, forAll).right
      _ <- forObservations(grp, scienceAndTellurics, forScienceAndTellurics).right
    } yield ()

  def forAll(obs:ISPObservation): Either[String, Unit] = for {
    _ <- obs.setFpu(blueprint.fpu).right
    _ <- obs.ed.iterateFirst(Flamingos2.FPU_PROP.getName, List(blueprint.fpu)).right
  } yield ()

  def forScienceAndTellurics(obs: ISPObservation): Either[String, Unit] = for {
    _ <- obs.setFilters(blueprint.filters.asScala).right
    _ <- obs.setDisperser(blueprint.disperser).right
  } yield ()

}
