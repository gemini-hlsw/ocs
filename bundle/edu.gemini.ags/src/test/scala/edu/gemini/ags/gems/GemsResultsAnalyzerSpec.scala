package edu.gemini.ags.gems

import edu.gemini.ags.TargetsHelper
import edu.gemini.ags.gems.mascot.MascotProgress
import edu.gemini.ags.gems.mascot.Strehl
import edu.gemini.catalog.votable.TestVoTableBackend
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates
import edu.gemini.shared.util.immutable.{None => JNone}
import edu.gemini.skycalc.Coordinates
import edu.gemini.skycalc.Offset
import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.core.Magnitude
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gems.CanopusWfs
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
import edu.gemini.spModel.telescope.IssPort
import jsky.coords.WorldCoords

import java.util.logging.Logger

import scala.concurrent.duration._

import org.specs2.mutable.SpecificationLike
import AlmostEqual.AlmostEqualOps
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

import scalaz._
import Scalaz._

/**
 * See OT-27
 */
class GemsResultsAnalyzerSpec extends MascotProgress with SpecificationLike with TargetsHelper {

  private val LOGGER = Logger.getLogger(classOf[GemsResultsAnalyzerSpec].getName)

  class TestGemsVoTableCatalog(file: String) extends GemsVoTableCatalog {
    override val backend = TestVoTableBackend(file)
  }

  val NoTime = JNone.instance[java.lang.Long]

  "GemsCatalogResultsSpec" should {
    "support Gsaoi Search on TYC 8345-1155-1" in {
      val base = new WorldCoords("17:25:27.529", "-48:27:24.02")
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.UP_LOOKING)}
      val tipTiltMode = GemsTipTiltMode.canopus

      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY).wv(SPSiteQuality.WaterVapor.ANY)
      val (results, gemsGuideStars) = search(inst, base.getRA.toString, base.getDec.toString, tipTiltMode, conditions, new TestGemsVoTableCatalog("/gems_TYC_8345_1155_1.xml"))

      val expectedResults = if (tipTiltMode == GemsTipTiltMode.both) 4 else 2
      results should have size expectedResults

      results.zipWithIndex.foreach { case (r, i) =>
        LOGGER.info("Result #" + i)
        LOGGER.info(" Criteria:" + r.criterion)
        LOGGER.info(" Results size:" + r.results.size)
      }

      LOGGER.info("gems results: size = " + gemsGuideStars.size)
      gemsGuideStars should have size 228

      val result = gemsGuideStars.head
      result.pa.toDegrees should beCloseTo(0, 0.0001)

      val group = result.guideGroup
      val set = group.getReferencedGuiders

      // Found a star on CWFS1, CWFS2, and CWFS3.
      set.contains(CanopusWfs.cwfs1) should beTrue
      set.contains(CanopusWfs.cwfs2) should beTrue
      set.contains(CanopusWfs.cwfs3) should beTrue

      val cwfs1 = group.get(CanopusWfs.cwfs1).getValue.getPrimary.getValue
      val cwfs2 = group.get(CanopusWfs.cwfs2).getValue.getPrimary.getValue
      val cwfs3 = group.get(CanopusWfs.cwfs3).getValue.getPrimary.getValue
      cwfs1.getName must beEqualTo("208-152095")
      cwfs2.getName must beEqualTo("208-152215")
      cwfs3.getName must beEqualTo("208-152039")

      val cwfs1x = Coordinates.create("17:25:27.151", "-48:28:07.67")
      val cwfs2x = Coordinates.create("17:25:32.541", "-48:27:30.06")
      val cwfs3x = Coordinates.create("17:25:24.719", "-48:26:58.00")

      (Angle.fromDegrees(cwfs1x.getRaDeg)  ~= Angle.fromDegrees(cwfs1.getSkycalcCoordinates(NoTime).getValue.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs1x.getDecDeg) ~= Angle.fromDegrees(cwfs1.getSkycalcCoordinates(NoTime).getValue.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getRaDeg)  ~= Angle.fromDegrees(cwfs2.getSkycalcCoordinates(NoTime).getValue.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getDecDeg) ~= Angle.fromDegrees(cwfs2.getSkycalcCoordinates(NoTime).getValue.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getRaDeg)  ~= Angle.fromDegrees(cwfs3.getSkycalcCoordinates(NoTime).getValue.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getDecDeg) ~= Angle.fromDegrees(cwfs3.getSkycalcCoordinates(NoTime).getValue.getDecDeg)) should beTrue

      val cwfs1Mag = cwfs1.getMagnitude(MagnitudeBand._r).map(_.value).get
      val cwfs2Mag = cwfs2.getMagnitude(MagnitudeBand.UC).map(_.value).get
      val cwfs3Mag = cwfs3.getMagnitude(MagnitudeBand._r).map(_.value).get
      cwfs3Mag < cwfs1Mag && cwfs2Mag < cwfs1Mag should beTrue
    }
    "support Gsaoi Search on SN-1987A" in {
      val base = new WorldCoords("05:35:28.020", "-69:16:11.07")
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.UP_LOOKING)}
      val tipTiltMode = GemsTipTiltMode.canopus

      val (results, gemsGuideStars) = search(inst, base.getRA.toString, base.getDec.toString, tipTiltMode, SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY), new TestGemsVoTableCatalog("/gems_sn1987A.xml"))

      val expectedResults = if (tipTiltMode == GemsTipTiltMode.both) 4 else 2
      results should have size expectedResults

      results.zipWithIndex.foreach { case (r, i) =>
        LOGGER.info("Result #" + i)
        LOGGER.info(" Criteria:" + r.criterion)
        LOGGER.info(" Results size:" + r.results.size)
      }

      LOGGER.info("gems results: size = " + gemsGuideStars.size)
      gemsGuideStars should have size 208

      val result = gemsGuideStars.head
      result.pa.toDegrees should beCloseTo(0, 0.0001)

      val group = result.guideGroup
      val set = group.getReferencedGuiders

      // Found a star on CWFS1, CWFS2, and CWFS3.
      set.contains(CanopusWfs.cwfs1) should beTrue
      set.contains(CanopusWfs.cwfs2) should beTrue
      set.contains(CanopusWfs.cwfs3) should beTrue

      val cwfs1 = group.get(CanopusWfs.cwfs1).getValue.getPrimary.getValue
      val cwfs2 = group.get(CanopusWfs.cwfs2).getValue.getPrimary.getValue
      val cwfs3 = group.get(CanopusWfs.cwfs3).getValue.getPrimary.getValue
      cwfs1.getName must beEqualTo("104-014597")
      cwfs2.getName must beEqualTo("104-014608")
      cwfs3.getName must beEqualTo("104-014547")

      val cwfs1x = Coordinates.create("05:35:32.630", "-69:15:48.64")
      val cwfs2x = Coordinates.create("05:35:36.409", "-69:16:24.17")
      val cwfs3x = Coordinates.create("05:35:18.423", "-69:16:30.67")

      (Angle.fromDegrees(cwfs1x.getRaDeg)  ~= Angle.fromDegrees(cwfs1.getSkycalcCoordinates(NoTime).getValue.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs1x.getDecDeg) ~= Angle.fromDegrees(cwfs1.getSkycalcCoordinates(NoTime).getValue.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getRaDeg)  ~= Angle.fromDegrees(cwfs2.getSkycalcCoordinates(NoTime).getValue.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getDecDeg) ~= Angle.fromDegrees(cwfs2.getSkycalcCoordinates(NoTime).getValue.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getRaDeg)  ~= Angle.fromDegrees(cwfs3.getSkycalcCoordinates(NoTime).getValue.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getDecDeg) ~= Angle.fromDegrees(cwfs3.getSkycalcCoordinates(NoTime).getValue.getDecDeg)) should beTrue

      val cwfs1Mag = cwfs1.getMagnitude(MagnitudeBand.UC).map(_.value).get
      val cwfs2Mag = cwfs2.getMagnitude(MagnitudeBand.UC).map(_.value).get
      val cwfs3Mag = cwfs3.getMagnitude(MagnitudeBand.UC).map(_.value).get
      cwfs3Mag < cwfs1Mag && cwfs2Mag < cwfs1Mag should beTrue
    }
    "support Gsaoi Search on M6" in {
      val base = new WorldCoords("17:40:20.000", "-32:15:12.00")
      val inst = new Gsaoi
      val tipTiltMode = GemsTipTiltMode.canopus

      val (results, gemsGuideStars) = search(inst, base.getRA.toString, base.getDec.toString, tipTiltMode, SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY), new TestGemsVoTableCatalog("/gems_m6.xml"))

      val expectedResults = if (tipTiltMode == GemsTipTiltMode.both) 4 else 2
      results should have size expectedResults

      results.zipWithIndex.foreach { case (r, i) =>
        LOGGER.info("Result #" + i)
        LOGGER.info(" Criteria:" + r.criterion)
        LOGGER.info(" Results size:" + r.results.size)
      }

      LOGGER.info("gems results: size = " + gemsGuideStars.size)
      gemsGuideStars should have size 252

      val result = gemsGuideStars.head
      result.pa.toDegrees should beCloseTo(90, 0.0001)

      val group = result.guideGroup
      val set = group.getReferencedGuiders

      // Found a star on CWFS1, CWFS2, and CWFS3.
      set.contains(CanopusWfs.cwfs1) should beTrue
      set.contains(CanopusWfs.cwfs2) should beTrue
      set.contains(CanopusWfs.cwfs3) should beTrue

      val cwfs1 = group.get(CanopusWfs.cwfs1).getValue.getPrimary.getValue
      val cwfs2 = group.get(CanopusWfs.cwfs2).getValue.getPrimary.getValue
      val cwfs3 = group.get(CanopusWfs.cwfs3).getValue.getPrimary.getValue
      cwfs1.getName must beEqualTo("289-128879")
      cwfs2.getName must beEqualTo("289-128909")
      cwfs3.getName must beEqualTo("289-128894")

      val cwfs1x = Coordinates.create("17:40:16.917", "-32:14:45.44")
      val cwfs2x = Coordinates.create("17:40:21.743", "-32:14:54.04")
      val cwfs3x = Coordinates.create("17:40:19.713", "-32:15:56.77")

      (Angle.fromDegrees(cwfs1x.getRaDeg)  ~= Angle.fromDegrees(cwfs1.getSkycalcCoordinates(NoTime).getValue.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs1x.getDecDeg) ~= Angle.fromDegrees(cwfs1.getSkycalcCoordinates(NoTime).getValue.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getRaDeg)  ~= Angle.fromDegrees(cwfs2.getSkycalcCoordinates(NoTime).getValue.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getDecDeg) ~= Angle.fromDegrees(cwfs2.getSkycalcCoordinates(NoTime).getValue.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getRaDeg)  ~= Angle.fromDegrees(cwfs3.getSkycalcCoordinates(NoTime).getValue.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getDecDeg) ~= Angle.fromDegrees(cwfs3.getSkycalcCoordinates(NoTime).getValue.getDecDeg)) should beTrue

      val cwfs1Mag = cwfs1.getMagnitude(MagnitudeBand.UC).map(_.value).get
      val cwfs2Mag = cwfs2.getMagnitude(MagnitudeBand.UC).map(_.value).get
      val cwfs3Mag = cwfs3.getMagnitude(MagnitudeBand.UC).map(_.value).get
      cwfs3Mag < cwfs1Mag && cwfs2Mag < cwfs1Mag should beTrue
    }
    "support Gsaoi Search on BPM 37093" in {
      val base = new WorldCoords("12:38:49.820", "-49:48:00.20")
      val inst = new Gsaoi
      val tipTiltMode = GemsTipTiltMode.canopus

      val (results, gemsGuideStars) = search(inst, base.getRA.toString, base.getDec.toString, tipTiltMode, SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY), new TestGemsVoTableCatalog("/gems_bpm_37093.xml"))

      val expectedResults = if (tipTiltMode == GemsTipTiltMode.both) 4 else 2
      results should have size expectedResults

      results.zipWithIndex.foreach { case (r, i) =>
        LOGGER.info("Result #" + i)
        LOGGER.info(" Criteria:" + r.criterion)
        LOGGER.info(" Results size:" + r.results.size)
      }

      LOGGER.info("gems results: size = " + gemsGuideStars.size)
      gemsGuideStars should have size 100

      val result = gemsGuideStars.head
      result.pa.toDegrees should beCloseTo(0, 0.0001)

      val group = result.guideGroup
      val set = group.getReferencedGuiders

      // Found a star on CWFS1, CWFS2, and CWFS3.
      set.contains(CanopusWfs.cwfs1) should beTrue
      set.contains(CanopusWfs.cwfs2) should beTrue
      set.contains(CanopusWfs.cwfs3) should beTrue

      val cwfs1 = group.get(CanopusWfs.cwfs1).getValue.getPrimary.getValue.getSkycalcCoordinates(NoTime).getValue
      val cwfs2 = group.get(CanopusWfs.cwfs2).getValue.getPrimary.getValue.getSkycalcCoordinates(NoTime).getValue
      val cwfs3 = group.get(CanopusWfs.cwfs3).getValue.getPrimary.getValue.getSkycalcCoordinates(NoTime).getValue

      val cwfs1x = Coordinates.create("12:38:50.130", "-49:47:38.07")
      val cwfs2x = Coordinates.create("12:38:44.500", "-49:47:58.38")
      val cwfs3x = Coordinates.create("12:38:50.005", "-49:48:00.89")

      (Angle.fromDegrees(cwfs1x.getRaDeg) ~= Angle.fromDegrees(cwfs1.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs1x.getDecDeg) ~= Angle.fromDegrees(cwfs1.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getRaDeg) ~= Angle.fromDegrees(cwfs2.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs2x.getDecDeg) ~= Angle.fromDegrees(cwfs2.getDecDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getRaDeg) ~= Angle.fromDegrees(cwfs3.getRaDeg)) should beTrue
      (Angle.fromDegrees(cwfs3x.getDecDeg) ~= Angle.fromDegrees(cwfs3.getDecDeg)) should beTrue
    }
    "sort targets by R magnitude" in {
      val st1 = target("n", edu.gemini.spModel.core.Coordinates.zero, List(new Magnitude(10.0, MagnitudeBand.J)))
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1)).head should beEqualTo(st1)

      val st2 = target("n", edu.gemini.spModel.core.Coordinates.zero, List(new Magnitude(15.0, MagnitudeBand.J)))
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2)).head should beEqualTo(st1)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2))(1) should beEqualTo(st2)

      val st3 = target("n", edu.gemini.spModel.core.Coordinates.zero, List(new Magnitude(15.0, MagnitudeBand.R)))
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3)).head should beEqualTo(st3)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3))(1) should beEqualTo(st1)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3))(2) should beEqualTo(st2)

      val st4 = target("n", edu.gemini.spModel.core.Coordinates.zero, List(new Magnitude(9.0, MagnitudeBand.R)))
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4)).head should beEqualTo(st4)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4))(1) should beEqualTo(st3)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4))(2) should beEqualTo(st1)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4))(3) should beEqualTo(st2)

      val st5 = target("n", edu.gemini.spModel.core.Coordinates.zero, List(new Magnitude(19.0, MagnitudeBand.R)))
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4, st5)).head should beEqualTo(st4)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4, st5))(1) should beEqualTo(st3)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4, st5))(2) should beEqualTo(st5)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4, st5))(3) should beEqualTo(st1)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4, st5))(4) should beEqualTo(st2)
    }
    "sort targets by R-like magnitude" in {
      val st1 = target("n", edu.gemini.spModel.core.Coordinates.zero, List(new Magnitude(10.0, MagnitudeBand.J)))
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1)).head should beEqualTo(st1)

      val st2 = target("n", edu.gemini.spModel.core.Coordinates.zero, List(new Magnitude(15.0, MagnitudeBand.J)))
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2)).head should beEqualTo(st1)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2))(1) should beEqualTo(st2)

      val st3 = target("n", edu.gemini.spModel.core.Coordinates.zero, List(new Magnitude(15.0, MagnitudeBand.R)))
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3)).head should beEqualTo(st3)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3))(1) should beEqualTo(st1)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3))(2) should beEqualTo(st2)

      val st4 = target("n", edu.gemini.spModel.core.Coordinates.zero, List(new Magnitude(9.0, MagnitudeBand._r)))
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4)).head should beEqualTo(st4)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4))(1) should beEqualTo(st3)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4))(2) should beEqualTo(st1)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4))(3) should beEqualTo(st2)

      val st5 = target("n", edu.gemini.spModel.core.Coordinates.zero, List(new Magnitude(19.0, MagnitudeBand.UC)))
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4, st5)).head should beEqualTo(st4)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4, st5))(1) should beEqualTo(st3)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4, st5))(2) should beEqualTo(st5)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4, st5))(3) should beEqualTo(st1)
      GemsResultsAnalyzer.sortTargetsByBrightness(List(st1, st2, st3, st4, st5))(4) should beEqualTo(st2)
    }

  }

  def search(inst: SPInstObsComp, raStr: String, decStr: String, tipTiltMode: GemsTipTiltMode, conditions: Conditions, catalog: TestGemsVoTableCatalog): (List[GemsCatalogSearchResults], List[GemsGuideStars]) = {
    import scala.collection.JavaConverters._

    val coords = new WorldCoords(raStr, decStr)
    val baseTarget = new SPTarget(coords.getRaDeg, coords.getDecDeg)
    val env = TargetEnvironment.create(baseTarget)
    val offsets = new java.util.HashSet[Offset]
    val obsContext = ObsContext.create(env, inst, JNone.instance[Site], conditions, offsets, new Gems, JNone.instance())
    val baseRA = Angle.fromDegrees(coords.getRaDeg)
    val baseDec = Angle.fromDegrees(coords.getDecDeg)
    val base = new HmsDegCoordinates.Builder(baseRA.toOldModel, baseDec.toOldModel).build
    val instrument = if (inst.isInstanceOf[Flamingos2]) GemsInstrument.flamingos2 else GemsInstrument.gsaoi

    val posAngles = Set(Angle.zero, Angle.fromDegrees(90), Angle.fromDegrees(180), Angle.fromDegrees(270)).asJava

    val options = new GemsGuideStarSearchOptions(instrument, tipTiltMode, posAngles)

    val results = Await.result(catalog.search(obsContext, base.toNewModel, options, scala.None)(implicitly), 5.seconds)
    val gemsResults = GemsResultsAnalyzer.analyze(obsContext, posAngles, results.asJava, scala.None)
    (results, gemsResults.asScala.toList)
  }

  def progress(s: Strehl, count: Int, total: Int, usable: Boolean): Boolean = true

  def setProgressTitle(s: String) {
    LOGGER.info(s)
  }
}
