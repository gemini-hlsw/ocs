package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosSBlueprintLongslitNs
import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.spModel.gemini.gmos.{GmosSouthType, InstGmosSouth}
import edu.gemini.spModel.core.SPProgramID

case class GmosSLongslitNs(blueprint:SpGmosSBlueprintLongslitNs) extends GmosSBase.WithTargetFolder[SpGmosSBlueprintLongslitNs] {

  // IF SPECTROSCOPY MODE == LONGSLIT N&S
  //         INCLUDE FROM 'LONGSLIT N&S BP' IN
  //             Target folder: {9} - {11}
  //             Baseline folder: {12} - {16}
  //         For spec observations: {10}, {11}, {13}-{15}
  //             SET DISPERSER FROM PI
  //             SET FILTER FROM PI
  //             SET FPU FROM PI
  //         For acquisitions: {9}, {12}
  //             if FPU!=None in the OT inst. iterators, then SET FPU FROM PI

  val targetFolder = 9 to 11
  val baselineFolder = 12 to 16
  val notes = Seq.empty

  def initialize(grp:ISPGroup, db:TemplateDb, pid: SPProgramID):Either[String, Unit] = {

    def forSpecObservations(o:ISPObservation):Either[String, Unit] = for {
      _ <- o.setDisperser(blueprint.disperser).right
      _ <- o.setFilter(blueprint.filter).right
      _ <- o.setFpu(blueprint.fpu).right
    } yield ()

    def forAcquisitions(o:ISPObservation):Either[String, Unit] = for {
      _ <- o.setFpu(blueprint.fpu).right
      _ <- o.ed.modifySeqAllKey(InstGmosSouth.FPUNIT_PROP.getName) {
        case GmosSouthType.FPUnitSouth.FPU_NONE => GmosSouthType.FPUnitSouth.FPU_NONE
        case _ => blueprint.fpu
      }.right
    } yield ()

    for {
      _ <- forObservations(grp, List(10, 11, 13, 14, 15), forSpecObservations).right
      _ <- forObservations(grp, List(9, 12), forAcquisitions).right
    } yield ()

  }
}