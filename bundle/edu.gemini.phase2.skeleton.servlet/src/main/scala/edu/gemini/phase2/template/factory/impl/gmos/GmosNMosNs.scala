package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosNBlueprintMos
import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth._
import edu.gemini.spModel.gemini.gmos.InstGmosCommon
import edu.gemini.spModel.gemini.gmos.GmosCommonType.Binning.ONE
import edu.gemini.spModel.core.SPProgramID

case class GmosNMosNs(blueprint:SpGmosNBlueprintMos) extends GmosNBase.WithTargetFolder[SpGmosNBlueprintMos] {

  // IF SPECTROSCOPY MODE == MOS N&S
  //         INCLUDE IN
  //             Target folder: {29}, {22}, {32}
  //               IF PRE-IMAGING REQ == YES
  //                 INCLUDE {17}, {27}
  //               IF PRE-IMAGING REQ == NO
  //                 INCLUDE {28}
  //             Baseline folder: {30}, {33}-{35}
  //         For spec observations: {29}, {32}, {34}, {35}
  //             SET DISPERSER FROM PI
  //             SET FILTER FROM PI
  //             For {29}
  //                 SET MOS "Slit Width" from PI
  //             For {34}, {35}
  //                 SET FPU (built-in longslit) using the width specified in PI
  //         For MOS observations in the target folder (not pre-image): any
  //         of {27} - {29}, {32}
  //             SET "Custom Mask MDF" = G(N/S)YYYYS(Q/C/DD/SV/LP/FT)XXX-NN 
  //                 where: 
  //                 (N/S) is the site 
  //                 YYYYS is the semester, e.g. 2015A 
  //                 (Q/C/DD/SV/LP/FT) is the program type 
  //                 XXX is the program number, e.g. 001, or 012, or 123 
  //                 NN should be the string "NN" since the mask number is unknown
  //         For standard acquisition: {33}
  //             if FPU!=None in the OT inst. iterators, then SET FPU (built-in longslit) using the width specified in PI
  //         For acquisitions {27}, {28}; mask image {22}; and N&S dark {30}
  //             No actions needed

  //         IF AO in PI != None  # XBIN=YBIN=1 for AO imaging and YBIN=1 for AO spectroscopy
  //             For pre-imaging and acquisitions {17}, {27}, {28}, {33} SET XBIN=YBIN=1
  //             For spec observations {29},{30},{32}, {34}, {35} SET YBIN=1

  val targetFolder = Seq(29, 22, 32) ++ (if (blueprint.preImaging) Seq(17, 27) else Seq(28))
  val baselineFolder = Seq(30, 33, 34, 35)
  val all = targetGroup ++ baselineFolder
  val spec = Seq(29, 32, 34, 35).filter(all.contains)

  def noneOrPiFpu(libFpu: Any) = if (libFpu == FPU_NONE) FPU_NONE else blueprint.fpu

  def forSpecObservation(o:ISPObservation):Either[String, Unit] = for {
    _ <- o.setDisperser(blueprint.disperser).right
    _ <- o.setFilter(blueprint.filter).right
  } yield ()

  def forStandardAcq(o:ISPObservation):Either[String, Unit] = for {
    _ <- o.setFpu(blueprint.fpu).right
    _ <- o.ed.modifySeqAllKey(InstGmosCommon.FPU_PROP_NAME) { case libFpu => noneOrPiFpu(libFpu) }.right
  } yield ()

  val notes = Seq.empty

  def initialize(grp:ISPGroup, db:TemplateDb, pid: SPProgramID):Either[String, Unit] = {
    val iniAo = withAoUpdate(db) _
    for {
      _ <- forObservations(grp, spec, iniAo(forSpecObservation)).right
      _ <- forObservations(grp, Seq(29), _.setCustomSlitWidth(blueprint.fpu)).right
      _ <- forObservations(grp, Seq(34, 35), iniAo(_.setCustomSlitWidth(blueprint.fpu))).right
      _ <- forObservations(grp, List(22, 27, 28, 29, 32).filter(targetFolder.contains), _.setDefaultCustomMaskName(pid)).right
      _ <- forObservations(grp, Seq(33), iniAo(forStandardAcq)).right
      _ <- forObservations(grp, Seq(17, 27, 28, 33).filter(all.contains), _.ifAo(_.setXyBin(ONE, ONE))).right
      _ <- forObservations(grp, Seq(29, 30, 32, 34, 35), _.ifAo(_.setYbin(ONE))).right
    } yield ()
  }

}
