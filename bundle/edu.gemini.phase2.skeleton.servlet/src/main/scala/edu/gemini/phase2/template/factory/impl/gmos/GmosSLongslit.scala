package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosSBlueprintLongslit
import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.spModel.gemini.gmos.{GmosSouthType, InstGmosSouth}

case class GmosSLongslit(blueprint:SpGmosSBlueprintLongslit) extends GmosSBase[SpGmosSBlueprintLongslit] {

  // IF SPECTROSCOPY MODE == LONGSLIT
  //         INCLUDE FROM 'LONGSLIT BP' IN
  //             Target Group: {2} - {3}
  //             Baseline Folder: {4} - {8}
  //         For spec observations: {3}, {4}, {6}-{8}
  //             SET DISPERSER FROM PI
  //             SET FILTER FROM PI
  //             SET FPU FROM PI
  //         For acquisitions: {2}, {5}
  //             if FPU!=None in the OT inst. iterators, then SET FPU FROM PI

  val targetGroup = 2 to 3
  val baselineFolder = 4 to 8
  val notes = Seq.empty

  def initialize(grp:ISPGroup, db:TemplateDb):Either[String, Unit] = {

    def forSpecObservation(o:ISPObservation):Either[String, Unit] = for {
      _ <- o.setDisperser(blueprint.disperser).right
      _ <- o.setFilter(blueprint.filter).right
      _ <- o.setFpu(blueprint.fpu).right
    } yield ()

    def forAcquisition(o:ISPObservation):Either[String, Unit] = for {
      _ <- o.setFpu(blueprint.fpu).right
      _ <- o.ed.modifySeqAllKey(InstGmosSouth.FPUNIT_PROP.getName) {
        case GmosSouthType.FPUnitSouth.FPU_NONE => GmosSouthType.FPUnitSouth.FPU_NONE
        case _ => blueprint.fpu
      }.right
    } yield ()

    for {
      _ <- forObservations(grp, List(3, 4, 6, 7, 8), forSpecObservation).right
      _ <- forObservations(grp, List(2, 5), forAcquisition).right
    } yield ()

  }


}
