package edu.gemini.ags.gems

import edu.gemini.ags.gems.mascot.MascotProgress
import edu.gemini.ags.gems.mascot.Strehl
import edu.gemini.catalog.votable.TestVoTableBackend
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates
import edu.gemini.shared.util.immutable.None
import edu.gemini.skycalc.Coordinates
import edu.gemini.skycalc.Offset
import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.core.{AlmostEqual, Angle, Site}
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gems.Canopus
import edu.gemini.spModel.gemini.gems.Gems
import edu.gemini.spModel.gemini.gems.GemsInstrument
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.gems.GemsTipTiltMode
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import jsky.coords.WorldCoords
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._

import org.specs2.mutable.Specification
import AlmostEqual.AlmostEqualOps
import scala.concurrent.Await

/**
 * See OT-27
 */
class GemsCatalogResultsSpec extends MascotProgress with Specification with NoTimeConversions {
  class TestGemsVoTableCatalog(file: String) extends GemsVoTableCatalog {
    override val backend = TestVoTableBackend(file)
  }

  "GemsCatalogResultsSpec" should {
    "support Gsaoi Search on TYC 8345-1155-1" in {
      val base = new WorldCoords("17:25:17.633", "-48:28:01.47")
      val inst = new Gsaoi
      val tipTiltMode = GemsTipTiltMode.canopus

      val (results, gemsGuideStars) = search(inst, base.getRA.toString, base.getDec.toString, tipTiltMode, SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY).wv(SPSiteQuality.WaterVapor.ANY), new TestGemsVoTableCatalog("/gemscatalogresultsquery.xml"))

      val expectedResults = if (tipTiltMode == GemsTipTiltMode.both) 4 else 2
      results should have size expectedResults

      results.zipWithIndex.foreach { case (r, i) =>
        System.out.println("Result #" + i)
        System.out.println(" Criteria:" + r.criterion)
        System.out.println(" Results size:" + r.results.size)
      }

      System.out.println("gems results: size = " + gemsGuideStars.size)
      gemsGuideStars should have size 63

      val result = gemsGuideStars.head
      result.getPa.toDegrees should beCloseTo(0, 0.0001)

      val group = result.getGuideGroup
      val set = group.getReferencedGuiders
      // Found a star on CWFS1, CWFS2, CWFS3 and ODWG3
      set.contains(Canopus.Wfs.cwfs1) should beTrue
      set.contains(Canopus.Wfs.cwfs2) should beTrue
      set.contains(Canopus.Wfs.cwfs3) should beTrue
      set.contains(GsaoiOdgw.odgw1) should beFalse
      set.contains(GsaoiOdgw.odgw2) should beFalse
      set.contains(GsaoiOdgw.odgw3) should beTrue
      set.contains(GsaoiOdgw.odgw4) should beFalse

      val cwfs1 = group.get(Canopus.Wfs.cwfs1).getValue.getPrimary.getValue.getTarget.getSkycalcCoordinates
      val cwfs2 = group.get(Canopus.Wfs.cwfs2).getValue.getPrimary.getValue.getTarget.getSkycalcCoordinates
      val cwfs3 = group.get(Canopus.Wfs.cwfs3).getValue.getPrimary.getValue.getTarget.getSkycalcCoordinates
      val odgw3 = group.get(GsaoiOdgw.odgw3).getValue.getPrimary.getValue.getTarget.getSkycalcCoordinates

      val cwfs1x = Coordinates.create("17:25:16.940", "-48:27:29.77")
      val cwfs2x = Coordinates.create("17:25:20.314", "-48:28:08.69")
      val cwfs3x = Coordinates.create("17:25:20.057", "-48:27:39.99")
      val odgw3x = Coordinates.create("17:25:13.703", "-48:27:39.48")

      (Angle.fromDegrees(cwfs1x.getRaDeg) ~= Angle.fromDegrees(cwfs1.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs1x.getDecDeg) ~= Angle.fromDegrees(cwfs1.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getRaDeg) ~= Angle.fromDegrees(cwfs2.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getDecDeg) ~= Angle.fromDegrees(cwfs2.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getRaDeg) ~= Angle.fromDegrees(cwfs3.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getDecDeg) ~= Angle.fromDegrees(cwfs3.getDecDeg)) should beTrue
      (Angle.fromDegrees(odgw3x.getRaDeg) ~= Angle.fromDegrees(odgw3.getRaDeg)) should beTrue
      (Angle.fromDegrees(odgw3x.getDecDeg) ~= Angle.fromDegrees(odgw3.getDecDeg)) should beTrue

      val cwfs1Mag = group.get(Canopus.Wfs.cwfs1).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.r).getValue.getBrightness
      val cwfs2Mag = group.get(Canopus.Wfs.cwfs2).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.UC).getValue.getBrightness
      val cwfs3Mag = group.get(Canopus.Wfs.cwfs3).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.UC).getValue.getBrightness
      cwfs1Mag < cwfs2Mag && cwfs3Mag < cwfs2Mag should beTrue
    }
    "support Gsaoi Search on SN-1987A" in {
      val base = new WorldCoords("05:35:28.020", "-69:16:11.07")
      val inst = new Gsaoi
      val tipTiltMode = GemsTipTiltMode.canopus

      val (results, gemsGuideStars) = search(inst, base.getRA.toString, base.getDec.toString, tipTiltMode, SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY), new TestGemsVoTableCatalog("/gems_sn1987A.xml"))

      val expectedResults = if (tipTiltMode == GemsTipTiltMode.both) 4 else 2
      results should have size expectedResults

      results.zipWithIndex.foreach { case (r, i) =>
        System.out.println("Result #" + i)
        System.out.println(" Criteria:" + r.criterion)
        System.out.println(" Results size:" + r.results.size)
      }

      System.out.println("gems results: size = " + gemsGuideStars.size)
      gemsGuideStars should have size 41

      val result = gemsGuideStars.head
      result.getPa.toDegrees should beCloseTo(0, 0.0001)

      val group = result.getGuideGroup
      val set = group.getReferencedGuiders
      // Found a star on CWFS1, CWFS2, CWFS3 and ODWG2
      set.contains(Canopus.Wfs.cwfs1) should beTrue
      set.contains(Canopus.Wfs.cwfs2) should beTrue
      set.contains(Canopus.Wfs.cwfs3) should beTrue
      set.contains(GsaoiOdgw.odgw1) should beFalse
      set.contains(GsaoiOdgw.odgw2) should beTrue
      set.contains(GsaoiOdgw.odgw3) should beFalse
      set.contains(GsaoiOdgw.odgw4) should beFalse

      val cwfs1 = group.get(Canopus.Wfs.cwfs1).getValue.getPrimary.getValue.getTarget
      val cwfs2 = group.get(Canopus.Wfs.cwfs2).getValue.getPrimary.getValue.getTarget
      val cwfs3 = group.get(Canopus.Wfs.cwfs3).getValue.getPrimary.getValue.getTarget
      val odgw2 = group.get(GsaoiOdgw.odgw2).getValue.getPrimary.getValue.getTarget
      cwfs1.getName must beEqualTo("104-014547")
      cwfs2.getName must beEqualTo("104-014564")
      cwfs3.getName must beEqualTo("104-014608")
      odgw2.getName must beEqualTo("104-014556")

      val cwfs1x = Coordinates.create("05:35:18.423", "-69:16:30.67")
      val cwfs2x = Coordinates.create("05:35:24.994", "-69:16:04.77")
      val cwfs3x = Coordinates.create("05:35:36.409", "-69:16:24.17")
      val odgw2x = Coordinates.create("05:35:23.887", "-69:16:18.20")

      (Angle.fromDegrees(cwfs1x.getRaDeg) ~= Angle.fromDegrees(cwfs1.getSkycalcCoordinates.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs1x.getDecDeg) ~= Angle.fromDegrees(cwfs1.getSkycalcCoordinates.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getRaDeg) ~= Angle.fromDegrees(cwfs2.getSkycalcCoordinates.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getDecDeg) ~= Angle.fromDegrees(cwfs2.getSkycalcCoordinates.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getRaDeg) ~= Angle.fromDegrees(cwfs3.getSkycalcCoordinates.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getDecDeg) ~= Angle.fromDegrees(cwfs3.getSkycalcCoordinates.getDecDeg)) should beTrue
      (Angle.fromDegrees(odgw2x.getRaDeg) ~= Angle.fromDegrees(odgw2.getSkycalcCoordinates.getRaDeg)) should beTrue
      (Angle.fromDegrees(odgw2x.getDecDeg) ~= Angle.fromDegrees(odgw2.getSkycalcCoordinates.getDecDeg)) should beTrue

      val cwfs1Mag = group.get(Canopus.Wfs.cwfs1).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.r).getValue.getBrightness
      val cwfs2Mag = group.get(Canopus.Wfs.cwfs2).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.UC).getValue.getBrightness
      val cwfs3Mag = group.get(Canopus.Wfs.cwfs3).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.UC).getValue.getBrightness
      cwfs1Mag < cwfs2Mag && cwfs3Mag < cwfs2Mag should beTrue
    }
    "support Gsaoi Search on M6" in {
      val base = new WorldCoords("17:40:20.000", "-32:15:12.00")
      val inst = new Gsaoi
      val tipTiltMode = GemsTipTiltMode.canopus

      val (results, gemsGuideStars) = search(inst, base.getRA.toString, base.getDec.toString, tipTiltMode, SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY), new TestGemsVoTableCatalog("/gems_m6.xml"))

      val expectedResults = if (tipTiltMode == GemsTipTiltMode.both) 4 else 2
      results should have size expectedResults

      results.zipWithIndex.foreach { case (r, i) =>
        System.out.println("Result #" + i)
        System.out.println(" Criteria:" + r.criterion)
        System.out.println(" Results size:" + r.results.size)
      }

      System.out.println("gems results: size = " + gemsGuideStars.size)
      gemsGuideStars should have size 59

      val result = gemsGuideStars.head
      result.getPa.toDegrees should beCloseTo(0, 0.0001)

      val group = result.getGuideGroup
      val set = group.getReferencedGuiders
      // Found a star on CWFS1, CWFS2, CWFS3 and ODWG2
      set.contains(Canopus.Wfs.cwfs1) should beTrue
      set.contains(Canopus.Wfs.cwfs2) should beTrue
      set.contains(Canopus.Wfs.cwfs3) should beTrue
      set.contains(GsaoiOdgw.odgw1) should beFalse
      set.contains(GsaoiOdgw.odgw2) should beTrue
      set.contains(GsaoiOdgw.odgw3) should beFalse
      set.contains(GsaoiOdgw.odgw4) should beFalse

      val cwfs1 = group.get(Canopus.Wfs.cwfs1).getValue.getPrimary.getValue.getTarget.getSkycalcCoordinates
      val cwfs2 = group.get(Canopus.Wfs.cwfs2).getValue.getPrimary.getValue.getTarget.getSkycalcCoordinates
      val cwfs3 = group.get(Canopus.Wfs.cwfs3).getValue.getPrimary.getValue.getTarget.getSkycalcCoordinates
      val odgw2 = group.get(GsaoiOdgw.odgw2).getValue.getPrimary.getValue.getTarget.getSkycalcCoordinates
      val cwfs1x = Coordinates.create("17:40:22.572", "-32:14:43.58")
      val cwfs2x = Coordinates.create("17:40:15.912", "-32:15:06.01")
      val cwfs3x = Coordinates.create("17:40:19.713", "-32:15:56.77")
      val odgw2x = Coordinates.create("17:40:16.855", "-32:15:55.83")

      (Angle.fromDegrees(cwfs1x.getRaDeg) ~= Angle.fromDegrees(cwfs1.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs1x.getDecDeg) ~= Angle.fromDegrees(cwfs1.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getRaDeg) ~= Angle.fromDegrees(cwfs2.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getDecDeg) ~= Angle.fromDegrees(cwfs2.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getRaDeg) ~= Angle.fromDegrees(cwfs3.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getDecDeg) ~= Angle.fromDegrees(cwfs3.getDecDeg)) should beTrue
      (Angle.fromDegrees(odgw2x.getRaDeg) ~= Angle.fromDegrees(odgw2.getRaDeg)) should beTrue
      (Angle.fromDegrees(odgw2x.getDecDeg) ~= Angle.fromDegrees(odgw2.getDecDeg)) should beTrue

      val cwfs1Mag = group.get(Canopus.Wfs.cwfs1).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.UC).getValue.getBrightness
      val cwfs2Mag = group.get(Canopus.Wfs.cwfs2).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.UC).getValue.getBrightness
      val cwfs3Mag = group.get(Canopus.Wfs.cwfs3).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.UC).getValue.getBrightness
      cwfs1Mag < cwfs2Mag && cwfs3Mag < cwfs2Mag should beTrue
    }
    "support Gsaoi Search on BPM 37093" in {
      val base = new WorldCoords("12:38:49.820", "-49:48:00.20")
      val inst = new Gsaoi
      val tipTiltMode = GemsTipTiltMode.canopus

      val (results, gemsGuideStars) = search(inst, base.getRA.toString, base.getDec.toString, tipTiltMode, SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY), new TestGemsVoTableCatalog("/gems_bpm_37093.xml"))

      val expectedResults = if (tipTiltMode == GemsTipTiltMode.both) 4 else 2
      results should have size expectedResults

      results.zipWithIndex.foreach { case (r, i) =>
        System.out.println("Result #" + i)
        System.out.println(" Criteria:" + r.criterion)
        System.out.println(" Results size:" + r.results.size)
      }

      System.out.println("gems results: size = " + gemsGuideStars.size)
      gemsGuideStars should have size 25

      val result = gemsGuideStars.head
      result.getPa.toDegrees should beCloseTo(0, 0.0001)

      val group = result.getGuideGroup
      val set = group.getReferencedGuiders
      // Found a star on CWFS1, CWFS2, CWFS3 and ODWG2
      set.contains(Canopus.Wfs.cwfs1) should beTrue
      set.contains(Canopus.Wfs.cwfs2) should beTrue
      set.contains(Canopus.Wfs.cwfs3) should beTrue
      set.contains(GsaoiOdgw.odgw1) should beFalse
      set.contains(GsaoiOdgw.odgw2) should beFalse
      set.contains(GsaoiOdgw.odgw3) should beFalse
      set.contains(GsaoiOdgw.odgw4) should beTrue

      val cwfs1 = group.get(Canopus.Wfs.cwfs1).getValue.getPrimary.getValue.getTarget.getSkycalcCoordinates
      val cwfs2 = group.get(Canopus.Wfs.cwfs2).getValue.getPrimary.getValue.getTarget.getSkycalcCoordinates
      val cwfs3 = group.get(Canopus.Wfs.cwfs3).getValue.getPrimary.getValue.getTarget.getSkycalcCoordinates
      val odgw4 = group.get(GsaoiOdgw.odgw4).getValue.getPrimary.getValue.getTarget.getSkycalcCoordinates
      val cwfs1x = Coordinates.create("12:38:50.130", "-49:47:38.07")
      val cwfs2x = Coordinates.create("12:38:44.500", "-49:47:58.38")
      val cwfs3x = Coordinates.create("12:38:50.005", "-49:48:00.89")
      val odgw4x = Coordinates.create("12:38:50.005", "-49:48:00.89")

      (Angle.fromDegrees(cwfs1x.getRaDeg) ~= Angle.fromDegrees(cwfs1.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs1x.getDecDeg) ~= Angle.fromDegrees(cwfs1.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getRaDeg) ~= Angle.fromDegrees(cwfs2.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getDecDeg) ~= Angle.fromDegrees(cwfs2.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getRaDeg) ~= Angle.fromDegrees(cwfs3.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getDecDeg) ~= Angle.fromDegrees(cwfs3.getDecDeg)) should beTrue
      (Angle.fromDegrees(odgw4x.getRaDeg) ~= Angle.fromDegrees(odgw4.getRaDeg)) should beTrue
      (Angle.fromDegrees(odgw4x.getDecDeg) ~= Angle.fromDegrees(odgw4.getDecDeg)) should beTrue

      val cwfs1Mag = group.get(Canopus.Wfs.cwfs1).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.r).getValue.getBrightness
      val cwfs2Mag = group.get(Canopus.Wfs.cwfs2).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.r).getValue.getBrightness
      val cwfs3Mag = group.get(Canopus.Wfs.cwfs3).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.r).getValue.getBrightness
      cwfs1Mag > cwfs2Mag && cwfs3Mag < cwfs2Mag should beTrue
    }
  }

  def search(inst: SPInstObsComp, raStr: String, decStr: String, tipTiltMode: GemsTipTiltMode, conditions: Conditions, catalog: TestGemsVoTableCatalog): (List[GemsCatalogSearchResults], List[GemsGuideStars]) = {
    val coords = new WorldCoords(raStr, decStr)
    val baseTarget = new SPTarget(coords.getRaDeg, coords.getDecDeg)
    val env = TargetEnvironment.create(baseTarget)
    val offsets = new java.util.HashSet[Offset]
    val obsContext = ObsContext.create(env, inst, None.instance[Site], conditions, offsets, new Gems)
    val baseRA = Angle.fromDegrees(coords.getRaDeg)
    val baseDec = Angle.fromDegrees(coords.getDecDeg)
    val base = new HmsDegCoordinates.Builder(baseRA.toOldModel, baseDec.toOldModel).build
    val instrument = if (inst.isInstanceOf[Flamingos2]) GemsInstrument.flamingos2 else GemsInstrument.gsaoi

    val posAngles = new java.util.HashSet[Angle]
    posAngles.add(GemsUtils4Java.toNewAngle(obsContext.getPositionAngle))
    posAngles.add(Angle.zero)

    val options = new GemsGuideStarSearchOptions(instrument, tipTiltMode, posAngles)

    val results = Await.result(catalog.search(obsContext, base.toNewModel, options, scala.None, null), 5.seconds)

    import scala.collection.JavaConverters._
    val gemsResults = new GemsCatalogResults().analyze(obsContext, posAngles, results.asJava, null)
    (results, gemsResults.asScala.toList)
  }

  def progress(s: Strehl, count: Int, total: Int, usable: Boolean): Boolean = true

  def setProgressTitle(s: String) {
    System.out.println(s)
  }
}