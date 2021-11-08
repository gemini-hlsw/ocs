package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosNBlueprintLongslit
import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.spModel.gemini.gmos.{GmosNorthType, InstGmosNorth}
import edu.gemini.spModel.gemini.gmos.GmosCommonType.Binning.ONE
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.phase2.template.factory.impl.TargetFolder
import edu.gemini.spModel.core.SPProgramID

case class GmosNLongslit(blueprint: SpGmosNBlueprintLongslit) extends GmosNBase.WithTargetFolder[SpGmosNBlueprintLongslit] {

  // IF SPECTROSCOPY MODE == LONGSLIT
  //         INCLUDE FROM 'LONGSLIT BP' IN
  //             Target folder: {2} - {4}
  //             Baseline folder: {5}-{8}
  //         For spec observations: {3}, {4}, {6}-{8}
  //             SET DISPERSER FROM PI
  //             SET FILTER FROM PI
  //             SET FPU FROM PI
  //         For acquisitions: {2}, {5}
  //             if FPU!=None in the OT inst. iterators, then SET FPU FROM PI

  val targetFolder = 2 to 4
  val baselineFolder = 5 to 8
  val notes = Seq.empty

  def initialize(grp: ISPGroup, db: TemplateDb, pid: SPProgramID): Either[String, Unit] = {
    val iniAo = withAoUpdate(db) _

    def forSpecObservation(o: ISPObservation): Either[String, Unit] = for {
      _ <- o.setDisperser(blueprint.disperser).right
      _ <- o.setFilter(blueprint.filter).right
      _ <- o.setFpu(blueprint.fpu).right
      _ <- o.ifAo(_.setYbin(ONE)).right
    } yield ()

    def forAcquisition(o: ISPObservation): Either[String, Unit] = for {
      _ <- o.setFpu(blueprint.fpu).right
      _ <- o.ed.modifySeqAllKey(InstGmosNorth.FPUNIT_PROP.getName) {
        case GmosNorthType.FPUnitNorth.FPU_NONE => GmosNorthType.FPUnitNorth.FPU_NONE
        case _ => blueprint.fpu
      }.right
      _ <- o.ifAo(_.setXyBin(ONE, ONE)).right
    } yield ()

    for {
      _ <- forObservations(grp, List(3, 4, 6, 7, 8), iniAo(forSpecObservation)).right
      _ <- forObservations(grp, List(2, 5), iniAo(forAcquisition)).right
    } yield ()

  }


}
