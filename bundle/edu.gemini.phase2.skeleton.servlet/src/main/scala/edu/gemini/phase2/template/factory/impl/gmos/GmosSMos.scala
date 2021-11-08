package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosSBlueprintMos
import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth._
import edu.gemini.spModel.gemini.gmos.InstGmosCommon
import edu.gemini.spModel.core.SPProgramID

case class GmosSMos(blueprint:SpGmosSBlueprintMos) extends GmosSBase.WithTargetFolder[SpGmosSBlueprintMos] {

  // IF SPECTROSCOPY MODE == MOS
  //         INCLUDE FROM 'MOS BP' IN
  //             Target folder: {20}, {21} - {23}
  //               IF PRE-IMAGING REQ == YES
  //                 INCLUDE {18}, {17}
  //               IF PRE-IMAGING REQ == NO
  //                 INCLUDE {19}
  //             Baseline folder: {24}-{26}
  //         For spec observations: {20}, {21}, {23}, {25}, {26}
  //             SET DISPERSER FROM PI
  //             SET FILTER FROM PI
  //             For {20}, {21}
  //                  SET MOS "Slit Width" from PI
  //             For {25}, {26}
  //                     SET FPU (built-in longslit) using the width specified in PI
  //         For MOS observations in the target folder (not pre-image): any of {18} - {23}
  //             SET "Custom Mask MDF" = G(N/S)YYYYS(Q/C/DD/SV/LP/FT)XXX-NN
  //                 where:
  //                 (N/S) is the site
  //                 YYYYS is the semester, e.g. 2015A
  //                 (Q/C/DD/SV/LP/FT) is the program type
  //                 XXX is the program number, e.g. 001, or 012, or 123
  //                 NN should be the string "NN" since the mask number is unknown
  //         For standard acquisition: {24}
  //             if FPU!=None in the OT inst. iterators, then SET FPU (built-in longslit) using the width specified in PI
  //         For acquisitions: {18}, {19} and mask image {22}
  //             No actions needed

  val targetFolder = Seq(20, 21, 22, 23) ++ (if (blueprint.preImaging) Seq(18, 17) else Seq(19))
  val baselineFolder = 24 to 26
  val all = targetGroup ++ baselineFolder
  val spec = Seq(20, 21, 23, 25, 26).filter(all.contains)

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

  def initialize(group:ISPGroup, db:TemplateDb, pid: SPProgramID):Either[String, Unit] =
    for {
      _ <- forObservations(group, spec, forSpecObservation).right
      _ <- forObservations(group, Seq(20, 21), _.setCustomSlitWidth(blueprint.fpu)).right
      _ <- forObservations(group, Seq(25, 26), _.setFpu(blueprint.fpu)).right
      _ <- forObservations(group, (18 to 23).filter(targetFolder.contains), _.setDefaultCustomMaskName(pid)).right
      _ <- forObservations(group, Seq(24), forStandardAcq).right
    } yield ()

}
