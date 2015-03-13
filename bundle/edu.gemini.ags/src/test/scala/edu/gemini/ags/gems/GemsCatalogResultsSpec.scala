package edu.gemini.ags.gems

import edu.gemini.ags.gems.mascot.MascotProgress
import edu.gemini.ags.gems.mascot.Strehl
import edu.gemini.catalog.votable.TestVoTableBackend
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates
import edu.gemini.shared.util.immutable.None
import edu.gemini.shared.util.immutable.Some
import edu.gemini.skycalc.Coordinates
import edu.gemini.skycalc.Offset
import edu.gemini.spModel.core.{Angle, Site}
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gems.Canopus
import edu.gemini.spModel.gemini.gems.Gems
import edu.gemini.spModel.gemini.gems.GemsInstrument
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gems.GemsTipTiltMode
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import jsky.coords.WorldCoords
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._

import edu.gemini.ags.impl._
import org.specs2.mutable.Specification
import scala.concurrent.Await

/**
 * See OT-27
 */
class GemsCatalogResultsSpec extends MascotProgress with Specification with NoTimeConversions {
  object TestGemsVoTableCatalog extends GemsVoTableCatalog {
    override val backend = TestVoTableBackend("/gemscatalogresultsquery.xml")
  }

  "GemsCatalogResultsSpec" should {
    "support Gsaoi Search" in {
      val base = new WorldCoords("17:25:17.633", "-48:28:01.47")
      val inst = new Gsaoi
      val results = search(inst, base.getRA.toString, base.getDec.toString, GemsTipTiltMode.canopus)

      results should not be empty

      val result = results(0)
      result.getPa.toDegrees should beCloseTo(0, 0.0001)

      val obsContext = ObsContext.create(null, inst, None.instance[Site], null, null, null)
      GemsCatalogResults.getStrehlFactor(new Some[ObsContext](obsContext)) should beCloseTo(result.getStrehl.getAvg / 10.589586438901101, 0.001)

      val group = result.getGuideGroup
      val set = group.getReferencedGuiders
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
      val cwfs1x = Coordinates.create("17:25:20.057", "-48:27:39.99")
      val cwfs2x = Coordinates.create("17:25:20.321", "-48:28:47.20")
      val cwfs3x = Coordinates.create("17:25:16.940", "-48:27:29.76")
      val odgw1x = Coordinates.create("17:25:20.321", "-48:28:47.20")
      cwfs1x.getRaDeg should beCloseTo(cwfs1.getRaDeg, 0.1)
      cwfs1x.getDecDeg should beCloseTo(cwfs1.getDecDeg, 0.1)
      cwfs2x.getRaDeg should beCloseTo(cwfs2.getRaDeg, 0.1)
      cwfs2x.getDecDeg should beCloseTo(cwfs2.getDecDeg, 0.1)
      cwfs3x.getRaDeg should beCloseTo(cwfs3.getRaDeg, 0.1)
      cwfs3x.getDecDeg should beCloseTo(cwfs3.getDecDeg, 0.1)

      val cwfs1Mag = group.get(Canopus.Wfs.cwfs1).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.R).getValue.getBrightness
      val cwfs2Mag = group.get(Canopus.Wfs.cwfs2).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.R).getValue.getBrightness
      val cwfs3Mag = group.get(Canopus.Wfs.cwfs3).getValue.getPrimary.getValue.getTarget.getMagnitude(Magnitude.Band.R).getValue.getBrightness
      cwfs3Mag < cwfs1Mag && cwfs3Mag < cwfs2Mag should beTrue
    }.pendingUntilFixed
  }

  def search(inst: SPInstObsComp, raStr: String, decStr: String, tipTiltMode: GemsTipTiltMode): List[GemsGuideStars] = {
    val coords = new WorldCoords(raStr, decStr)
    val baseTarget = new SPTarget(coords.getRaDeg, coords.getDecDeg)
    val env = TargetEnvironment.create(baseTarget)
    val offsets = new java.util.HashSet[Offset]
    val obsContext = ObsContext.create(env, inst, None.instance[Site], SPSiteQuality.Conditions.BEST, offsets, new Gems)
    val baseRA = Angle.fromDegrees(coords.getRaDeg)
    val baseDec = Angle.fromDegrees(coords.getDecDeg)
    val base = new HmsDegCoordinates.Builder(baseRA.toOldModel, baseDec.toOldModel).build
    val opticalCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG
    val nirCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG
    val instrument = if (inst.isInstanceOf[Flamingos2]) GemsInstrument.flamingos2 else GemsInstrument.gsaoi

    val posAngles = new java.util.HashSet[Angle]
    posAngles.add(GemsUtils4Java.toNewAngle(obsContext.getPositionAngle))
    posAngles.add(Angle.zero)

    val options = new GemsGuideStarSearchOptions(opticalCatalog, nirCatalog, instrument, tipTiltMode, posAngles)
    val results = Await.result(TestGemsVoTableCatalog.search(obsContext, base.toNewModel, options, scala.None, null), 5.seconds)

    if (options.getTipTiltMode eq GemsTipTiltMode.both) {
      results should have size 4
    } else {
      results should have size 2
    }
    results.zipWithIndex.foreach { case (r, i) =>
      System.out.println("Result #" + i)
      System.out.println(" Criteria:" + r.criterion)
      System.out.println(" Results size:" + r.results.size)
    }
    import scala.collection.JavaConverters._
    val gemsResults = new GemsCatalogResults().analyze(obsContext, posAngles, results.asJava, null)
    System.out.println("gems results: size = " + gemsResults.size)
    gemsResults.asScala.toList
  }

  def progress(s: Strehl, count: Int, total: Int, usable: Boolean): Boolean = true

  def setProgressTitle(s: String) {
    System.out.println(s)
  }
}