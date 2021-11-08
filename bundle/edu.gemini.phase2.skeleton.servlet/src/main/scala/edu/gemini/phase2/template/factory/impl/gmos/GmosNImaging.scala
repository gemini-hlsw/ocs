package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosNBlueprintImaging

import scala.collection.JavaConverters._
import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.gmos.GmosCommonType.Binning.ONE
import edu.gemini.spModel.gemini.gmos.GmosNorthType._
import edu.gemini.phase2.template.factory.impl.TemplateDb
import edu.gemini.spModel.core.SPProgramID


case class GmosNImaging(blueprint:SpGmosNBlueprintImaging) extends GmosNBase[SpGmosNBlueprintImaging] {

  import blueprint._

  //  **** IF MODE == IMAGING ****
  //  INCLUDE BP{1}
  //          Put FILTERS from PI into the GMOS ITERATOR and set the
  //          exposure time for each filter from
  //          http://dmt.gemini.edu/docushare/dsweb/Get/Document-333602/GMOS_img_exptimes.xlsx

  val targetGroup = Seq(1)
  val baselineFolder = Seq.empty
  val notes = Seq.empty

  def initialize(grp:ISPGroup, db:TemplateDb, pid: SPProgramID):Either[String, Unit] = {

    def forAll(obs:ISPObservation):Either[String, Unit] = for {
      _ <- obs.setFilters(filters.asScala).right
      _ <- obs.setExposures(filters.asScala.map(defaultExposures.getOrElse(_, 0.0))).right
      _ <- obs.ifAo(_.setXyBin(ONE, ONE)).right
    } yield ()

    for {
      _ <- forObservations(grp, List(1), withAoUpdate(db)(forAll)).right
    } yield ()

  }

  val defaultExposures = {
    import FilterNorth._
    Map(
      g_G0301 -> 60.0,
      r_G0303 -> 40.0,
      i_G0302 -> 30.0,
      CaT_G0309 -> 30.0,
      z_G0304 -> 30.0,
      Z_G0322 -> 30.0,
      Y_G0323 -> 30.0,
      ri_G0349 -> 30.0,
      HeII_G0320 -> 300.0,
      HeIIC_G0321 -> 300.0,
      OIII_G0318 -> 300.0,
      OIIIC_G0319 -> 300.0,
      Ha_G0310 -> 300.0,
      HaC_G0311 -> 300.0,
      SII_G0317 -> 300.0,
      DS920_G0312 -> 300.0)
  }


}

