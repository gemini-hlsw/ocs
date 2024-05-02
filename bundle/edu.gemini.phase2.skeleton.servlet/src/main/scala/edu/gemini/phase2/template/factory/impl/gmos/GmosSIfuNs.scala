package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosSBlueprintIfuNs
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth._
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth._
import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.spModel.gemini.gmos.InstGmosCommon
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth
import edu.gemini.spModel.core.SPProgramID

case class GmosSIfuNs(blueprint:SpGmosSBlueprintIfuNs) extends GmosSBase.WithTargetFolder[SpGmosSBlueprintIfuNs] {

  // IF SPECTROSCOPY MODE == IFU N&S
  //         INCLUDE FROM 'IFU N&S BP' IN,
  //             Target folder: {43}-{44}
  //             Baseline folder: {46}-{50}
  //         Where FPU!=None in BP (static and iterator), SET FPU from PI
  //             IFU acq obs have an iterator titled "Field image" with
  //                 FPU=None, the FPU must not be set here.
  //             IF FPU = 'IFU 1 SLIT' in PI then SET FPU='IFU N & S Right Slit (red)' in OT
  //         If not acq SET DISPERSER FROM PI
  //         For filter changes below, do not adjust exposure times.
  //         If acq ({43}, {49})
  //            If filter from PI != None, SET FILTER in static component
  //              to ugriz filter closest in central wavelength to the filter
  //              from PI
  //            else SET FILTER=r (as in BP)
  //         else SET FILTER FROM PI
  //             IF FPU = 'IFU 2 slits' in PI (IFU or IFU N&S mode):
  //             IF FILTER=None, SET FILTER=r_G0326
  //                 SET CENTRAL WAVELENGTHS TO THE FILTER EFF WAVELENGTH
  //                  AND EFF WAVELENGTH + 5nm (if iteration over wavelength)
  //                 See http://www.gemini.edu/node/10637

  val targetFolder = 43 to 44
  val baselineFolder = 46 to 50
  val notes = Seq.empty

  val acq = Seq(43, 49)

  def initialize(grp:ISPGroup, db:TemplateDb, pid: SPProgramID):Either[String, Unit] = forObservations(grp, forAll)

  def piFpu                    = if (blueprint.fpu == IFU_2) IFU_3 else blueprint.fpu
  def noneOrPiFpu(libFpu: Any) = if (libFpu == FPU_NONE) FPU_NONE else piFpu

  def forAll(o:ISPObservation):Either[String, Unit] = for {

    // Where FPU!=None in BP (static and iterator), SET FPU from PI
    // (here BP means the library not the blueprint!)
    fpuInLIB <- o.getFpu.right
    _ <- o.setFpu(noneOrPiFpu(fpuInLIB)).right
    _ <- o.ed.modifySeqAllKey(InstGmosCommon.FPU_PROP_NAME) {
           case libFpu => noneOrPiFpu(libFpu)
         }.right

    // If not acq SET DISPERSER FROM PI
    _ <- when(!o.memberOf(acq)) {
      o.setDisperser(blueprint.disperser)
    }.right

    _ <- (if (o.memberOf(acq)) {
      if (blueprint.filter != NONE)
        o.setFilter(closestUGRIZ(blueprint.filter.getWavelength.toDouble))
      else
        o.setFilter(r_G0326)
    } else {
      for {
        _ <- o.setFilter(blueprint.filter).right

        // For IFU 2-slit N&S, set wavelengths
        _ <- when(blueprint.fpu == IFU_N) {

          // Handle empty filter
          val f = if (blueprint.filter == FilterSouth.NONE) FilterSouth.r_G0326 else blueprint.filter

          // Set observing wavelength
          val lambda = f.getWavelength.toDouble * 1000.0 // seriously ?!??
          for {
            _ <- o.setFilter(f).right
            _ <- o.setDisperserLambda(lambda).right
            _ <- o.ed.modifySeqAllKey(InstGmosCommon.DISPERSER_LAMBDA_PROP.getName){
              case 780 => lambda
              case 785 => lambda + 5.0
            }.right
          } yield ()
        }.right

      } yield ()
    }).right

  } yield ()

}
