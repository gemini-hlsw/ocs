package edu.gemini.phase2.template.factory.impl.flamingos2

import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import scala.collection.JavaConverters._
import edu.gemini.spModel.gemini.flamingos2.blueprint.SpFlamingos2BlueprintMos
import edu.gemini.phase2.template.factory.impl.TemplateDb

case class Flamingos2Mos(blueprint:SpFlamingos2BlueprintMos) extends Flamingos2Base[SpFlamingos2BlueprintMos] {

//  IF PRE-IMAGING REQUIRED = YES
// 	    INCLUDE {21}
//
//  INCLUDE {30}                              # Mask daytime image
// 	INCLUDE {22,23}                           # Telluric std
// 	INCLUDE {24,25,26}                        # Science
// 	INCLUDE {28,29}                           # Telluric std
//
//  FOR {23,24,25,26,29}:                     # Science and Tellurics
// 	    SET DISPERSER FROM PI
//             Put FILTERS from PI into F2 ITERATOR
//
//  FOR {21,22,23,24,25,26,28,29}:
//      SET CONDITIONS FROM PI

  def preImaging = if (blueprint.preImaging) Seq(21) else Seq.empty
  val targetGroup = preImaging ++ Seq(30,22,23,24,25,26,28,29)
  val baselineFolder = Seq.empty
  val notes = Seq("F2 MOS Notes")

  val scienceAndTellurics = Seq(23,24,25,26,29)

  def initialize(grp:ISPGroup, db:TemplateDb):Either[String, Unit] = forObservations(grp, scienceAndTellurics, forScienceAndTellurics)

  def forScienceAndTellurics(obs:ISPObservation):Either[String, Unit] = for {
    _ <- obs.setDisperser(blueprint.disperser).right
    _ <- obs.setFilters(blueprint.filters.asScala).right
  } yield ()

}
