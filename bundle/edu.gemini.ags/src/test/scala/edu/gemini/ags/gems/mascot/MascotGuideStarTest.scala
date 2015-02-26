package edu.gemini.ags.gems.mascot

import org.junit.{Before, Ignore, Test}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.telescope.IssPort
import edu.gemini.spModel.obs.context.ObsContext
import jsky.catalog.skycat.SkycatConfigFile
import jsky.coords.{CoordinateRadius, WorldCoords}
import jsky.catalog.{TableQueryResult, BasicQueryArgs}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.shared.util.immutable.{None => JNone, Option => JOption, Some => JSome}

/**
 * Tests the MascotGuideStar class
 */
@Ignore class MascotGuideStarTest {

  @Before def initialize() {
    val url = getClass.getResource("/edu/gemini/spModel/gemsGuideStar/test.skycat.cfg")
    assert(url != null)
    SkycatConfigFile.setConfigFile(url)
  }

  @Test def testFindBestAsterism() {
    val coords = new WorldCoords("03:19:48.2341", "+41:30:42.078")
    val base = new SPTarget(coords.getRaDeg, coords.getDecDeg)
    val env = TargetEnvironment.create(base)
    val inst = new Gsaoi()
    inst.setPosAngle(0.0)
    inst.setIssPort(IssPort.SIDE_LOOKING)
    val ctx = ObsContext.create(env, inst, JNone.instance(), SPSiteQuality.Conditions.BEST, null, null)
    val basePos = ctx.getBaseCoordinates

    val result = MascotGuideStar.findBestAsterism(ctx, MascotGuideStar.CWFS, 180.0, 10.0)

    for ((strehlList, pa, ra, dec) <- result) {
      val d = MascotGuideStar.dist(ra, dec, basePos.getRaDeg, basePos.getDecDeg) * 3600.0
      println("XXX num asterisms = " + strehlList.size + " at " + new WorldCoords(ra, dec) + " and pa = " + pa +
        ", dist = " + d + " arcsec")
    }
  }

  @Test def testFileBestAsterismByQueryResult() {
    val configFile = SkycatConfigFile.getConfigFile
    val cat = configFile.getCatalog(MascotCat.defaultCatalogName)
    assert(cat != null)
    val queryArgs = new BasicQueryArgs(cat)
    val coords = new WorldCoords("03:19:48.2341", "+41:30:42.078")
    val region = new CoordinateRadius(coords, MascotCat.defaultMinRadius, MascotCat.defaultMaxRadius)
    queryArgs.setRegion(region)
    queryArgs.setMaxRows(MascotCat.defaultMaxRows)
    val queryResult = cat.query(queryArgs)

    val base = new SPTarget(coords.getRaDeg, coords.getDecDeg)
    val env = TargetEnvironment.create(base)
    val inst = new Gsaoi()
    inst.setPosAngle(0.0)
    inst.setIssPort(IssPort.SIDE_LOOKING)
    val ctx = ObsContext.create(env, inst, JNone.instance(), SPSiteQuality.Conditions.BEST, null, null)

    val result = MascotGuideStar.findBestAsterismInQueryResult(
      queryResult.asInstanceOf[TableQueryResult], ctx, MascotGuideStar.CWFS, 180.0, 10.0)

    val basePos = ctx.getBaseCoordinates
    for ((strehlList, pa, ra, dec) <- result) {
      val d = MascotGuideStar.dist(ra, dec, basePos.getRaDeg, basePos.getDecDeg) * 3600.0
      println("XXX num asterisms = " + strehlList.size + " at " + new WorldCoords(ra, dec) + " and pa = " + pa +
        ", dist = " + d + " arcsec")
    }
  }
}