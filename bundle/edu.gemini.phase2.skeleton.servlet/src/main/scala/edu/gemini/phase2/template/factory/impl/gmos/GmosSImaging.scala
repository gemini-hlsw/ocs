package edu.gemini.phase2.template.factory.impl.gmos

import edu.gemini.phase2.template.factory.impl._
import edu.gemini.spModel.gemini.gmos.blueprint.SpGmosSBlueprintImaging

import scala.collection.JavaConverters._
import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.gmos.GmosSouthType._
import edu.gemini.spModel.core.SPProgramID

case class GmosSImaging(blueprint:SpGmosSBlueprintImaging) extends GmosSBase[SpGmosSBlueprintImaging] {

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
    } yield ()

    for {
      _ <- forObservations(grp, List(1), forAll).right
    } yield ()

  }

  val defaultExposures = {
    import FilterSouth._
    Map(
      u_G0332 -> 180.0,
      g_G0325 -> 60.0,
      r_G0326 -> 40.0,
      i_G0327 -> 30.0,
      CaT_G0333 -> 30.0,
      z_G0328 -> 30.0,
      Z_G0343 -> 30.0,
      Y_G0344 -> 30.0,
      HeII_G0340 -> 300.0,
      HeIIC_G0341 -> 300.0,
      OIII_G0338 -> 300.0,
      OIIIC_G0339 -> 300.0,
      Ha_G0336 -> 300.0,
      HaC_G0337 -> 300.0,
      SII_G0335 -> 300.0)
  }

}

