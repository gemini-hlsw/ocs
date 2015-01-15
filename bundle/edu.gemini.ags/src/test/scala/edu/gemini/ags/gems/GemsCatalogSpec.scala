package edu.gemini.ags.gems

import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates
import edu.gemini.shared.util.immutable.None
import edu.gemini.spModel.core.{Angle, Site}
import edu.gemini.spModel.gemini.gems.GemsInstrument
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gems.GemsTipTiltMode
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.IssPort
import edu.gemini.ags.impl._
import jsky.catalog.skycat.SkycatConfigFile
import jsky.coords.WorldCoords

import org.specs2.mutable.Specification

class GemsCatalogSpec extends Specification {
  "GemsCatalog" should {
    "support executing queries" in {
      skipped("Skipped as it requires GUI interaction")

      val url = getClass.getResource("/edu/gemini/spModel/gemsGuideStar/test.skycat.cfg")
      SkycatConfigFile.setConfigFile(url)

      val coords = new WorldCoords("03:19:48.2341", "+41:30:42.078")
      val target = new SPTarget(coords.getRaDeg, coords.getDecDeg)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi
      inst.setPosAngle(0.0)
      inst.setIssPort(IssPort.SIDE_LOOKING)
      val ctx = ObsContext.create(env, inst, None.instance[Site], SPSiteQuality.Conditions.BEST, null, null)
      val base = new HmsDegCoordinates.Builder(Angle.fromDegrees(coords.getRaDeg).toOldModel, Angle.fromDegrees(coords.getDecDeg).toOldModel).build
      val opticalCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG
      val nirCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG
      val instrument = GemsInstrument.gsaoi
      val tipTiltMode = GemsTipTiltMode.instrument

      val posAngles = new java.util.HashSet[Angle]()
      val options = new GemsGuideStarSearchOptions(opticalCatalog, nirCatalog,
              instrument, tipTiltMode, posAngles)

      val band = None.instance[Magnitude.Band]()
      val results = new GemsCatalog().search(ctx, base, options, band, null)
      results should have size 2
    }
  }
}