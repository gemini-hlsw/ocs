package edu.gemini.phase2.template.factory.impl.ghost

import edu.gemini.phase2.template.factory.impl.Maybe
import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.pot.sp.ISPGroup
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.ghost.blueprint.SpGhostBlueprint

case class Ghost(blueprint: SpGhostBlueprint) extends GhostBase[SpGhostBlueprint] {

//  INCLUDE {1}     # Science
//
//  SET RESOLUTION MODE FROM PI  # Possible w/o a target component?
//
//  SET TARGET MODE FROM PI      # Possible w/o a target component?
//
//  ON TEMPATE INSTANTIATION (adding the target(s))
//    IF RESOLUTION MODE == STANDARD
//        IF TARGET MODE == 'Single'
//          Add target from PI to SRIFU1
//        IF TARGET MODE == 'Dual'  # Position from PI is the Base position
//          Add target1 from PI with RA + 1 arcmin to SRIFU1
//          Add target2 from PI with RA - 1 arcmin to SRIFU2
//
//      IF TARGET MODE ==  'SRIFU1 Sky + SRIFU2 Target'
//        Add target from PI to SRIFU2
//        Add target from PI with ra + 2 arcmin to SRIFU1
//      ELSE
//        Add target from PI to SRIFU1
//        IF TARGET MODE ==  'SRIFU1 Target + SRIFU2 Sky'
//          Add target from PI with ra - 2 arcmin to SRIFU2
//
//    IF RESOLUTION MODE == HIGH or PRECISION RADIAL VELOCITY
//      Add target from PI to HRIFU
//      Add target from PI with ra - 2 arcmin to HRSKY

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
