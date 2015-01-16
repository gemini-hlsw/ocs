package edu.gemini.ags.gems.mascot

import jsky.catalog.skycat.SkycatConfigFile
import org.junit.Assert._
import jsky.coords.WorldCoords
import org.junit.{Ignore, Before, Test}

/**
 */
@Ignore class MascotCatTest {

  @Before def initialize() {
    val url = getClass.getResource("/edu/gemini/spModel/gemsGuideStar/test.skycat.cfg")
    assert(url != null)
    SkycatConfigFile.setConfigFile(url)
  }

  @Test def testFindBestAsterism() {
//    val configFile = SkycatConfigFile.getConfigFile();
//    val cat = configFile.getCatalog("NOMAD1 catalog at CDS");
//    //    val cat = configFile.getCatalog(MascotCat.catName);
//    assert(cat != null)
//    val queryArgs = new BasicQueryArgs(cat);
    val coords = new WorldCoords("03:19:48.2341", "+41:30:42.078")
//    val region = new CoordinateRadius(coords, 0, 1.2)
//    queryArgs.setRegion(region)
//    queryArgs.setMaxRows(19)
    val (starList, strehlList) = MascotCat.findBestAsterism(coords, "NOMAD1 catalog at CDS",
                                      "R", Mascot.defaultFactor, Mascot.defaultProgress,
                                      (star: Star) => star.rmag <= 17.5 && star.rmag >= 10.0
                                  )

    // ... see MascotTest in mascot-core
//    assertEquals(6, starList.size)
//    assertEquals(41, strehlList.size)
    assertEquals(5, starList.size)
    assertEquals(25, strehlList.size)
    val err = 0.1
    val it = strehlList.iterator

    //    Asterism #1, [1.3,0.8], [-33.0,43.4], [42.4,15.9]
    //    Strehl over 80.0": avg=88.3  rms=0.4  min=86.7  max=88.8
    val s = it.next()
    assertEquals(1.3, s.stars(0).x, err)
    assertEquals(0.8, s.stars(0).y, err)
    assertEquals(-33.0, s.stars(1).x, err)
    assertEquals(43.4, s.stars(1).y, err)
    assertEquals(42.4, s.stars(2).x, err)
    assertEquals(15.9, s.stars(2).y, err)
    assertEquals(88.3, s.avgstrehl * 100, err)
    assertEquals(0.4, s.rmsstrehl * 100, err)
    assertEquals(86.7, s.minstrehl * 100, err)
    assertEquals(88.8, s.maxstrehl * 100, err)
  }
}