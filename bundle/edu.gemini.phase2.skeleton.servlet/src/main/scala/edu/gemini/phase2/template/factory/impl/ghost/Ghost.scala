package edu.gemini.phase2.template.factory.impl.ghost

import edu.gemini.phase2.template.factory.impl.Maybe
import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.pot.sp.ISPGroup
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.ghost.blueprint.SpGhostBlueprint

case class Ghost(blueprint: SpGhostBlueprint) extends GhostBase[SpGhostBlueprint] {

//INCLUDE {1}     # Science
//
//SET RESOLUTION MODE FROM PI
//
//SET TARGET MODE FROM PI
//
//ON TEMPATE INSTANTIATION (adding the target(s))
//	IF RESOLUTION MODE == STANDARD
//	    IF TARGET MODE == 'Single'
//	    	Add target from PI to SRIFU1
//
//	    IF TARGET MODE == 'Dual'  # Position from PI is the fainter target
//	    	Add target from PI to SRIFU1
//	    	Add target from PI with RA - 2 arcmin to SRIFU2, set target name to 'Target 2'
//
//		IF TARGET MODE ==  'SRIFU + Sky'
//			Add target from PI to SRIFU1
//			Add target from PI with ra - 2 arcmin to SRIFU2, set target name to 'Sky'
//
//	IF RESOLUTION MODE == HIGH or PRECISION RADIAL VELOCITY
//		Add target from PI to HRIFU
//		Add target from PI with ra - 2 arcmin to HRSKY, the target name should be 'Sky'

  override def targetGroup: Seq[Int] =
    List(1)

  override def baselineFolder: Seq[Int] =
    List.empty

  override def notes: Seq[String] =
    List.empty

  // Records the preferred asterism type from the PI in the GHOST component.
  // This is then used on instantiation to create corresponding GHOST asterism.
  private def setAsterismType(o: ISPObservation): Maybe[Unit] =
    o.setAsterismType(blueprint.asterismType)

  override def initialize(group: ISPGroup, db: TemplateDb, pid: SPProgramID): Maybe[Unit] =
    forObservations(group, targetGroup, setAsterismType)

}
