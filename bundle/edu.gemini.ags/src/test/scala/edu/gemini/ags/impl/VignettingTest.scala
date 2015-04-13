package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsRegistrar
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.pot.ModelConverters._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.skycalc.{DDMMSS, HHMMSS}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.spModel.gemini.gmos.{GmosOiwfsGuideProbe, InstGmosSouth}
import edu.gemini.spModel.gemini.inst.InstRegistry
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions._
import edu.gemini.spModel.guide.{VignettingGuideProbe, ValidatableGuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.{IssPortProvider, IssPort}

import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._
import scala.util.Random

/**
 * Right now, we only test for GMOS. In the future, this will be expanded to include other guide probes.
 */
class VignettingTest {
  sealed trait VignettingConfiguration {
    def inst: SPInstObsComp
    def probe: ValidatableGuideProbe with VignettingGuideProbe
    def site: Site
  }

  case class VignettingConfigurationWithISSPort(spType: SPComponentType, probe: ValidatableGuideProbe with VignettingGuideProbe,
    site: Site, port: IssPort) extends VignettingConfiguration {
    lazy val inst = InstRegistry.instance.prototype(spType.narrowType).getValue
    inst.asInstanceOf[IssPortProvider].setIssPort(port)
  }

  val GMOSSouthSideLookingWithOI = VignettingConfigurationWithISSPort(InstGmosSouth.SP_TYPE, GmosOiwfsGuideProbe.instance, Site.GS, IssPort.SIDE_LOOKING)
  val GMOSSouthUpLookingWithOI   = VignettingConfigurationWithISSPort(InstGmosSouth.SP_TYPE, GmosOiwfsGuideProbe.instance, Site.GS, IssPort.UP_LOOKING)

  val GS1  = siderealTarget("GS1",  "23:59:58.187 -00:00:10.20", 10.0)
  val GS2  = siderealTarget("GS2",  "23:59:55.693  00:00:49.30", 15.0)
  val GS3  = siderealTarget("GS3",  "23:59:49.687  00:01:02.90", 14.0)
  val GS4  = siderealTarget("GS4",  "23:59:48.440  00:03:03.60", 16.0)
  val GS5  = siderealTarget("GS5",  "23:59:45.380 -00:00:20.40",  9.0)
  val GS6  = siderealTarget("GS6",  "00:00:03.060  00:01:09.70",  9.0)
  val GS7  = siderealTarget("GS7",  "00:00:12.353  00:03:05.30", 12.0)
  val GS8  = siderealTarget("GS8",  "00:00:04.307 -00:00:59.50", 15.0)
  val GS9  = siderealTarget("GS9",  "00:00:12.240 -00:02:55.10", 16.0)
  val GS10 = siderealTarget("GS10", "00:00:10.200 -00:01:35.20", 15.5)
  val All = List(GS1, GS2, GS3, GS4, GS5, GS6, GS7, GS8, GS9, GS10)

  // Specifically non-vignetting guide stars for base position (0,0) to test that brightness is correctly chosen.
  val NVGS1 = siderealTarget("NGS1", "23:59:57.620  00:03:10.40", 11.0)
  val NVGS2 = siderealTarget("NGS2", "23:59:48.213  00:02:50.00",  9.0)
  val NVGS3 = siderealTarget("NGS3", "23:59:47.420  00:01:26.70", 10.0)
  val NVGS4 = siderealTarget("NGS4", "23:59:47.420  00:00:34.00", 12.0)
  val NVGS5 = siderealTarget("NGS5", "23:59:47.533 -00:00:11.90", 15.5)
  val NVGS6 = siderealTarget("NGS6", "23:59:53.087  00:03:17.20", 15.4)
  val NVGS7 = siderealTarget("NGS7", "23:59:47.873  00:02:14.30", 16.0)
  val NVGS8 = siderealTarget("NGS8", "23:59:59.320  00:03:15.50", 16.5)
  val AllNV = List(NVGS1, NVGS2, NVGS3, NVGS4, NVGS5, NVGS6, NVGS7, NVGS8)

  // Load the magnitude table and create the base position.
  val mt   = ProbeLimitsTable.loadOrThrow()
  val base = new SPTarget(0.0, 0.0)

  // Convert a string and magnitude to a SiderealTarget.
  def siderealTarget(name: String, raDecStr: String, rMag: Double): SiderealTarget = {
    val (raStr, decStr) = raDecStr.span(_ != ' ')
    val ra  = Angle.fromDegrees(HHMMSS.parse(raStr).toDegrees.getMagnitude)
    val dec = Angle.fromDegrees(DDMMSS.parse(decStr.trim).toDegrees.getMagnitude)
    val sc  = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
    SiderealTarget(name, sc, None, List(new Magnitude(rMag, MagnitudeBand.R)), None)
  }

  /**
   * Perform an AGS selection test with vignetting taken into account using the instrument and guide probe as specified
   * in the info, using the offsets provided, and with the list of candidates. The expected list lists the candidates
   * in the order that they should be selected, e.g. expected[0] should be the first candidate selected, expected[1]
   * should be the candidate selected if expected[0] is thrown away, etc. Note that, of course, if a candidate is not
   * a valid choice, it should not appear in expected.
   *
   * @param expected   the order in which the candidates should be selected
   * @param candidates the list of candidates to pass to AGS
   * @param config     the instrument and guide probe to test
   * @param posAngle   the position angle to use in degrees
   * @param offsets    the offsets to use
   * @param conditions the conditions required
   */
  def executeTest(expected: List[SiderealTarget],
                  candidates: List[SiderealTarget] = All,
                  config: VignettingConfiguration = GMOSSouthSideLookingWithOI,
                  posAngle: Double = 0.0,
                  offsets: List[Offset] = Nil,
                  conditions: Conditions = BEST): Unit = {
    config.inst.setPosAngleDegrees(posAngle)
    val targetEnv = TargetEnvironment.create(base).addActive(config.probe)
    val offsetSet = offsets.map(_.toOldModel).toSet.asJava
    val ctx       = ObsContext.create(targetEnv, config.inst, Some(config.site).asGeminiOpt, BEST, offsetSet, null)
    val strategy  = AgsRegistrar.currentStrategy(ctx).get.asInstanceOf[SingleProbeStrategy]

    def nextCandidate(candidates: List[SiderealTarget], expected: List[SiderealTarget]): Unit = {
      expected match {
        case next :: remain =>
          val selectionOpt = strategy.select(ctx, mt, candidates)
          assertTrue(selectionOpt.isDefined)

          val selection = selectionOpt.get
          assertEquals(selection.assignments.size, 1)

          val assignment = selection.assignments.head
          assertEquals(next, assignment.guideStar)

          nextCandidate(candidates.diff(List(next)), expected.drop(1))

        case Nil =>
          val selectionOpt = strategy.select(ctx, mt, candidates)
          assertTrue(selectionOpt.isEmpty)
      }
    }
    nextCandidate(Random.shuffle(candidates), expected)
  }

  @Test def testSideLookingBasePosAngle0() = {
    val expected   = List(GS3, GS2, GS1, GS4)
    executeTest(expected)
  }

  @Test def testSideLookingBasePosAngle90() = {
    val expected = List(GS7, GS6, GS1)
    executeTest(expected, posAngle = 90.0)
  }

  @Test def testSideLookingBasePosAngle180() = {
    val expected = List(GS8, GS10, GS9)
    executeTest(expected, posAngle = 180.0)
  }

  @Test def testUpLookingBasePosAngle0() = {
    val expected = List(GS1)
    executeTest(expected, config = GMOSSouthUpLookingWithOI)
  }

  @Test def testUpLookingBasePosAngle90() = {
    val expected = List(GS3, GS2, GS1, GS4)
    executeTest(expected, config = GMOSSouthUpLookingWithOI, posAngle = 90.0)
  }

  @Test def testUpLookingBasePosAngle180() = {
    val expected = List(GS7, GS6)
    executeTest(expected, config = GMOSSouthUpLookingWithOI, posAngle = 180.0)
  }

  @Test def testSideLookingOneOffsetPosAngle0() = {
    val expected = List(GS2, GS6)
    executeTest(expected, offsets = List(Offset(50.arcsecs[OffsetP], 50.arcsecs[OffsetQ])))
  }

  @Test def testSideLookingOneOffset2PosAngle0() = {
    // The vignetting exclusively on the offset would result in GS6 and then GS7, but the vignetting averaged across
    // the base position and the offset would result in GS7 and then GS6.
    val expected = List(GS7, GS6)
    executeTest(expected, offsets = List(Offset(200.arcsecs[OffsetP], 50.arcsecs[OffsetQ])))
  }

  @Test def testSideLookingOneNegOffset2PosAngle0() = {
    val expected = List(GS5, GS3, GS2)
    executeTest(expected, offsets = List(Offset(-50.arcsecs[OffsetP], -50.arcsecs[OffsetQ])))
  }

  @Test def testAllNoVignetting() = {
    val expected = List(NVGS2, NVGS3, NVGS1, NVGS4, NVGS6, NVGS5, NVGS7)
    executeTest(expected, candidates = AllNV)
  }

  @Test def testBadCase() = {
    val BGS1 = siderealTarget("BGS1", "05:24:31.028 -24:21:38.56", 11.882)
    val BGS2 = siderealTarget("BGS2", "05:24:38.738 -24:19:58.79", 12.746)
    val candidates = List(BGS1, BGS2)
    val expected   = List(BGS2, BGS1)
    val conditions = new Conditions(CloudCover.PERCENT_70, ImageQuality.PERCENT_70, SkyBackground.ANY, WaterVapor.ANY)
    executeTest(expected, candidates = candidates, conditions = conditions)
  }
}
