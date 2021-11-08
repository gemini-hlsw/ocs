package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosNBlueprintIfu
import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.spModel.gemini.gmos.{InstGmosCommon, GmosNorthType}
import edu.gemini.spModel.gemini.gmos.GmosCommonType.Binning.ONE
import GmosNorthType.FPUnitNorth._
import GmosNorthType.FilterNorth._
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FilterNorth
import edu.gemini.spModel.core.SPProgramID

case class GmosNIfu(blueprint:SpGmosNBlueprintIfu) extends GmosNBase.WithTargetFolder[SpGmosNBlueprintIfu] {

  // IF SPECTROSCOPY MODE == IFU
  //         INCLUDE FROM 'IFU BP' IN,
  //             Target folder: {36}-{38}
  //             Baseline folder: {39}-{42}
  //         Where FPU!=None in BP (static and iterator), SET FPU from PI
  //             IFU acq obs have an iterator titled "Field image" with
  //                 FPU=None, the FPU must not be set here.
  //             IF FPU = 'IFU 1 SLIT' in PI then FPU='IFU Right Slit (red)'  in OT
  //         If not acq SET DISPERSER FROM PI
  //         For filter changes below, do not adjust exposure times.
  //         If acq ({36}, {41})
  //            If filter from PI != None, SET FILTER in static component
  //              to griz filter closest in central
  //              wavelength to the filter from PI
  //            else SET FILTER=r (as in BP)
  //         else SET FILTER FROM PI:
  //             IF FPU = "IFU 2 slits" in PI (IFU mode):
  // 	        IF FILTER=None, SET FILTER=r_G0303
  //                 SET CENTRAL WAVELENGTHS TO THE FILTER EFF WAVELENGTH
  //                  AND EFF WAVELENGTH +/- 10 nm (in GMOS iterators)
  //                 See http://www.gemini.edu/node/10637

  //         IF AO in PI != None  # XBIN=YBIN=1 for AO imaging
  //             For acquisition {36}, {41} SET XBIN=YBIN=1

  val targetFolder = 36 to 38
  val baselineFolder = 39 to 42
  val notes = Seq.empty

  val acq = Seq(36, 41)

  def initialize(grp:ISPGroup, db:TemplateDb, pid: SPProgramID):Either[String, Unit] = forObservations(grp, withAoUpdate(db)(forAll))

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
      for {
        _ <- (if (blueprint.filter != NONE) o.setFilter(closestGRIZ(blueprint.filter.getWavelength.toDouble))
              else o.setFilter(r_G0303)).right
        _ <- o.ifAo(_.setXyBin(ONE,ONE)).right
      } yield ()
    } else {
      for {
        _ <- o.setFilter(blueprint.filter).right

        // For IFU 2-slit, set wavelengths
        _ <- when(blueprint.fpu == IFU_1) {

          // Handle empty filter
          val f = if (blueprint.filter == FilterNorth.NONE) FilterNorth.r_G0303 else blueprint.filter

          // Set observing wavelength
          val lambda = f.getWavelength.toDouble * 1000.0 // seriously ?!??
          for {
            _ <- o.setFilter(f).right
            _ <- o.setDisperserLambda(lambda).right
            _ <- o.ed.modifySeqAllKey(InstGmosCommon.DISPERSER_LAMBDA_PROP.getName){
              case 620 => lambda - 10.0
              case 630 => lambda
              case 640 => lambda + 10.0
            }.right
          } yield ()
        }.right

      } yield ()
    }).right

  } yield ()


}
