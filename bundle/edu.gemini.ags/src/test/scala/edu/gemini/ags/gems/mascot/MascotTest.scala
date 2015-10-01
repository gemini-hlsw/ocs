package edu.gemini.ags.gems.mascot

import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import org.junit.Test
import org.junit.Assert._
import java.util.Date

/**
 * Test cases for Scala port of the Yorick based Mascot sources.
 */
class MascotTest {
  // nomad.dat for ngc1275
  //#...aclient 130.79.129.161 1660 nomad1  -c 03 19 48.2341 +41 30 42.078 -r 1.20 -E
  //#====== NOMAD1 server (2006-06, V1.31) ======  CDS, Strasbourg ======
  //#Center: 03 19 48.2341 +41 30 42.078
  //#---------Position recomputed to Epoch.
  //#NOMAD1.0   |BCTYM|   RA  (J2000) Dec     r  sRA  sDE| Ep.RA  EpDec|    pmRA     pmDE  spRA  spDE| Bmag.r  Vmag.r  Rmag.r| Jmag   Hmag   Kmag |R| ;     r(")
  //1315-0066023|B..YM|049.9351778+41.5020167 B   68  179|1981.1 1981.1|     +.0      +.0    .0    .0| ---    17.970Y 19.720B|16.870 16.199 15.374| | ;     55.01
  //1315-0066024|BC.YM|049.9352419+41.5160950 C   26   34|2001.9 2001.3|    +2.3     -2.5   5.4   5.5|14.710Y 14.060Y 13.110B|12.677 12.144 12.063| | ;     45.28
  //1315-0066027|B....|049.9392806+41.5187056 B  999  659|1973.8 1973.8|     +.0      +.0    .0    .0|16.620B  ---     ---   | ---    ---    ---  | | ;     40.40
  //1315-0066035|....M|049.9429658+41.5124969 M    0    0|1998.8 1998.8|     +.0      +.0    .0    .0| ---     ---     ---   |16.579 15.986 15.317| | ;     21.79
  //1315-0066042|B..YM|049.9449139+41.5292444 B  119   61|1979.6 1979.6|     +.0      +.0    .0    .0|17.520Y 17.250Y 15.250B|15.621 15.054 14.551| | ;     65.28
  //1315-0066051|B....|049.9499639+41.5121417 B   50   95|1981.5 1981.5|   -12.0     +4.0   2.0   4.0|12.670B  ---    11.590B| ---    ---    ---  | | ;      3.18
  //1315-0066052|B..YM|049.9505111+41.5119111 B  795  568|1981.1 1981.1|     +.0      +.0    .0    .0|11.340o  9.080Y 11.090B|12.769 11.977 11.298| | ;      1.49
  //1315-0066059|....M|049.9527450+41.5124889 M    0    0|1998.8 1998.8|     +.0      +.0    .0    .0| ---     ---     ---   |13.836 13.226 13.064| | ;      5.57
  //1315-0066071|B..YM|049.9554667+41.5307000 B   50   72|1979.6 1979.6|   -12.0     +2.0    .0   4.0|18.350Y 17.900Y 18.020B|16.042 15.803 15.201| | ;     69.51
  //1315-0066075|B....|049.9570139+41.5025167 B   50   79|1981.1 1981.1|    -2.0    +20.0    .0   4.0| ---     ---    18.560B| ---    ---    ---  | | ;     36.81
  //1315-0066083|B...M|049.9605083+41.5302417 B   50   61|1979.6 1979.6|   -10.0     +4.0   1.0   3.0|19.680B  ---    18.030B|16.236 15.846 15.368| | ;     71.56
  //1315-0066087|BC.YM|049.9632019+41.5237503 C   15   24|2002.5 2002.0|    -3.5     -3.3   5.4   5.4|14.190Y 13.550Y 12.630B|11.678 11.088 10.979| | ;     54.51
  //1315-0066091|B...M|049.9644556+41.5256472 B   88  267|1981.5 1981.5|   -32.0    -40.0   5.0  14.0|18.660B  ---    15.520B|15.287 14.712 14.452| | ;     62.01
  //1315-0066095|B....|049.9655056+41.5272611 B   50  172|1981.4 1981.4|   +12.0    +88.0   3.0  10.0| ---     ---    19.610B| ---    ---    ---  | | ;     68.39
  //1315-0066100|B....|049.9688667+41.5122250 B   86   50|1992.8 1992.8|     +.0      +.0    .0    .0| ---     ---    19.110B| ---    ---    ---  | | ;     48.27
  //1315-0066105|B....|049.9694889+41.5071000 B  123   96|1979.6 1979.6|     +.0      +.0    .0    .0|19.690B  ---    18.870B| ---    ---    ---  | | ;     52.57
  //1315-0066108|B....|049.9710750+41.5020583 B   50  145|1973.8 1973.8|     +.0      +.0    .0    .0|18.160B  ---     ---   | ---    ---    ---  | | ;     64.33
  //1315-0066109|B....|049.9714250+41.5010639 B  999  999|1993.2 1993.2|     +.0      +.0    .0    .0|20.320B  ---    19.220B| ---    ---    ---  | | ;     67.10
  //1315-0066114|B..YM|049.9734472+41.5146056 B   74   50|1979.6 1979.6|     +.0      +.0    .0    .0|16.120Y 15.540Y 15.390B|14.666 14.211 14.210| | ;     61.48
  //#--- 19 matches (210 tested)

  // starlist for ngc1275 in col order (y, x, Bmag.r  Vmag.r,  Rmag.r, Jmag,   Hmag,   Kmag, m?, RA, DEC)
  //  [[1.25168,0.801961,11.34,9.08,11.09,12.769,11.977,11.298,2, 49.9505,41.5119],
  //   [-32.9534,43.4231,14.19,13.55,12.63,11.678,11.088,10.979,2,49.9632,41.5238],
  //   [42.4108,15.864,14.71,14.06,13.11,12.677,12.144,12.063,2,49.9352,41.5161],
  //   [16.3359,63.2018,17.52,17.25,15.25,15.621,15.054,14.551,2,49.9449,41.5292],
  //   [-60.5757,10.5022,16.12,15.54,15.39,14.666,14.211,14.21,2,49.9734,41.5146],
  //   [-36.3314,50.2519,18.66,-27,15.52,15.287,14.712,14.452,2,49.9645,41.5256]]
  val starList = List(
    MascotTest.star(1.25168, 0.801961, 11.34, 9.08, 11.09, 12.769, 11.977, 11.298, 49.9505, 41.5119),
    MascotTest.star(-32.9534, 43.4231, 14.19, 13.55, 12.63, 11.678, 11.088, 10.979, 49.9632, 41.5238),
    MascotTest.star(42.4108, 15.864, 14.71, 14.06, 13.11, 12.677, 12.144, 12.063, 49.9352, 41.5161),
    MascotTest.star(16.3359, 63.2018, 17.52, 17.25, 15.25, 15.621, 15.054, 14.551, 49.9449, 41.5292),
    MascotTest.star(-60.5757, 10.5022, 16.12, 15.54, 15.39, 14.666, 14.211, 14.21, 49.9734, 41.5146),
    MascotTest.star(-36.3314, 50.2519, 18.66, -27, 15.52, 15.287, 14.712, 14.452, 49.9645, 41.5256)
  )

  val allStarList = List(
    MascotTest.star(1.25168, 0.801961, 11.34, 9.08, 11.09, 12.769, 11.977, 11.298, 49.9505, 41.5119),
    MascotTest.star(2.72678, 1.63212, 12.67, -27, 11.59, -27, -27, -27, 49.95, 41.5121),
    MascotTest.star(-32.9534, 43.4231, 14.19, 13.55, 12.63, 11.678, 11.088, 10.979, 49.9632, 41.5238),
    MascotTest.star(42.4108, 15.864, 14.71, 14.06, 13.11, 12.677, 12.144, 12.063, 49.9352, 41.5161),
    MascotTest.star(16.3359, 63.2018, 17.52, 17.25, 15.25, 15.621, 15.054, 14.551, 49.9449, 41.5292),
    MascotTest.star(-60.5757, 10.5022, 16.12, 15.54, 15.39, 14.666, 14.211, 14.21, 49.9734, 41.5146),
    MascotTest.star(-36.3314, 50.2519, 18.66, -27, 15.52, 15.287, 14.712, 14.452, 49.9645, 41.5256),
    MascotTest.star(-12.1038, 68.442, 18.35, 17.9, 18.02, 16.042, 15.803, 15.201, 49.9555, 41.5307),
    MascotTest.star(-25.6909, 66.7921, 19.68, -27, 18.03, 16.236, 15.846, 15.368, 49.9605, 41.5302),
    MascotTest.star(-16.2806, -33.0179, -27, -27, 18.56, -27, -27, -27, 49.957, 41.5025),
    MascotTest.star(-49.9113, -16.518, 19.69, -27, 18.87, -27, -27, -27, 49.9695, 41.5071),
    MascotTest.star(-48.2301, 1.932, -27, -27, 19.11, -27, -27, -27, 49.9689, 41.5122),
    MascotTest.star(-55.1361, -38.248, 20.32, -27, 19.22, -27, -27, -27, 49.9714, 41.5011),
    MascotTest.star(-39.1604, 56.062, -27, -27, 19.61, -27, -27, -27, 49.9655, 41.5273),
    MascotTest.star(42.5929, -34.8179, -27, 17.97, 19.72, 16.87, 16.199, 15.374, 49.9352, 41.502)
  )

  @Test def testFindBestAsterism() {
    val xxx = new Date().getTime
    val (starList, strehlList) = Mascot.findBestAsterism(allStarList)
    println("XXX findBestAsterism: " + ((new Date().getTime() - xxx) / 1000.0) + " sec")

//    assertEquals(6, starList.size)
//    assertEquals(41, strehlList.size)
    // Note: change in results is due to change in MascotConf.mag_max_threshold, to include GSAOI limits
    assertEquals(7, starList.size)
    assertEquals(56, strehlList.size)

    val err = 0.1
    val it = strehlList.iterator

    //    Asterism #1, [1.3,0.8], [-33.0,43.4], [42.4,15.9]
    //    Strehl over 80.0": avg=88.3  rms=0.4  min=86.7  max=88.8
    val s = it.next
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

  @Test def testSelectStarsOnMag() {
    val slist = Mascot.selectStarsOnMag(allStarList)
    assertEquals(7, slist.size)
    // Note: change in results is due to change in MascotConf.mag_max_threshold, to include GSAOI limits
//    assertEquals(slist.toString, starList.toString)
  }
}

object MascotTest {
  def star(centerX: Double, centerY: Double,
                 bmag: Double, vmag: Double,
                 rmag: Double, jmag: Double,
                 hmag: Double, kmag: Double,
                 ra: Double, dec: Double): Star = {
    val coords = Coordinates(RightAscension.fromDegrees(ra), Declination.fromAngle(Angle.fromDegrees(dec)).getOrElse(Declination.zero))
    val magnitudes = List(new Magnitude(bmag, MagnitudeBand.B), new Magnitude(vmag, MagnitudeBand.V), new Magnitude(rmag, MagnitudeBand.R), new Magnitude(jmag, MagnitudeBand.J), new Magnitude(hmag, MagnitudeBand.H), new Magnitude(kmag, MagnitudeBand.K))
    val target = SiderealTarget("name", coords, None, None, magnitudes)
    Star.makeStar(target, centerX, centerY).copy(x = centerX, y = centerY)
  }
}