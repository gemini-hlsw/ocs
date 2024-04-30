package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosNBlueprintLongslitNs
import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.spModel.gemini.gmos.{GmosNorthType, InstGmosNorth}
import edu.gemini.spModel.gemini.gmos.GmosCommonType.Binning.ONE
import edu.gemini.spModel.core.SPProgramID

case class GmosNLongslitNs(blueprint:SpGmosNBlueprintLongslitNs) extends GmosNBase.WithTargetFolder[SpGmosNBlueprintLongslitNs] {

  // IF SPECTROSCOPY MODE == LONGSLIT N&S
  //         INCLUDE FROM 'LONGSLIT N&S BP' IN
  //             Target folder: {9} - {10}
  //             Baseline folder: {12} - {16}
  //         For spec observations: {10}, {13}-{15}    
  //             SET DISPERSER FROM PI
  //             SET FILTER FROM PI
  //             SET FPU FROM PI
  //         For acquisitions: {9}, {12}
  //             if FPU!=None in the OT inst. iterators, then SET FPU FROM PI

  val targetFolder = 9 to 10
  val baselineFolder = 12 to 16
  val notes = Seq.empty

  def initialize(grp:ISPGroup, db:TemplateDb, pid: SPProgramID):Either[String, Unit] = {
    val iniAo = withAoUpdate(db) _

    def forSpecObservations(o:ISPObservation):Either[String, Unit] = for {
      _ <- o.setDisperser(blueprint.disperser).right
      _ <- o.setFilter(blueprint.filter).right
      _ <- o.setFpu(blueprint.fpu).right
      _ <- o.ifAo(_.setYbin(ONE)).right
    } yield ()

    def forAcquisitions(o:ISPObservation):Either[String, Unit] = for {
      _ <- o.setFpu(blueprint.fpu).right
      _ <- o.ed.modifySeqAllKey(InstGmosNorth.FPUNIT_PROP.getName) {
        case GmosNorthType.FPUnitNorth.FPU_NONE => GmosNorthType.FPUnitNorth.FPU_NONE
        case _ => blueprint.fpu
      }.right
      _ <- o.ifAo(_.setXyBin(ONE, ONE)).right
    } yield ()

    for {
      _ <- forObservations(grp, List(10, 13, 14, 15), iniAo(forSpecObservations)).right
      _ <- forObservations(grp, List(16), _.ifAo(_.setYbin(ONE))).right
      _ <- forObservations(grp, List(9, 12), iniAo(forAcquisitions)).right
    } yield ()

  }
}