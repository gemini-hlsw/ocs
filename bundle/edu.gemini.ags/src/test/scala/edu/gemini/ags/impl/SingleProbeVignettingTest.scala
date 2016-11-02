package edu.gemini.ags.impl

import edu.gemini.ags.api.{AgsGuideQuality, AgsStrategy}
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.catalog.votable.CannedBackend
import edu.gemini.pot.ModelConverters._
import edu.gemini.skycalc.{Offset => SkyCalcOffset}
import edu.gemini.shared.util.immutable.ImOption
import edu.gemini.spModel.ags.AgsStrategyKey.GmosNorthOiwfsKey
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.MagnitudeBand.{_r, R, UC}
import edu.gemini.spModel.gemini.gmos.{InstGmosNorth, GmosOiwfsGuideProbe}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.inst.VignettingArbitraries
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.PosAngleConstraintAware
import edu.gemini.spModel.telescope.PosAngleConstraint.{FIXED, FIXED_180}

import org.scalacheck._
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scalaz._
import Scalaz._

object SingleProbeVignettingTest extends Specification with ScalaCheck with VignettingArbitraries with Helpers {
  private val magTable = ProbeLimitsTable.loadOrThrow()

  val genRMag: Gen[Magnitude] =
    for {
      band   <- Gen.oneOf(MagnitudeBand._r, MagnitudeBand.R, MagnitudeBand.UC)
      bright <- Gen.chooseNum(5, 20)
    } yield Magnitude(bright, band, None, MagnitudeSystem.Vega)

  def genGuideStar(ctx: ObsContext, coords: Coordinates): Gen[SiderealTarget] =
    for {
      name    <- Gen.alphaStr
      bands   <- Gen.someOf(List(_r, R, UC))
      brights <- Gen.listOfN(bands.size, Gen.chooseNum(5, 20))
    } yield {
      val mags = bands.zip(brights).map { case (band, bright) => Magnitude(bright, band, None, MagnitudeSystem.Vega) }
      SiderealTarget.empty.copy(name = name, coordinates = coords, magnitudes = mags.toList)
    }

  // why do i have to write this myself?
  def sequence[A](lst: List[Gen[A]]): Gen[List[A]] =
    (lst:\Gen.const[List[A]](Nil)) { (ga, glst) =>
      for {
        a   <- ga
        lst <- glst
      } yield a :: lst
    }

  def genGuideStars(ctx: ObsContext): Gen[List[SiderealTarget]] = {
    def farEnough(c: Coordinates): Boolean = {
      val minDistance = SingleProbeStrategyParams.GmosOiwfsParams.apply(Site.GN).minDistance.map(_.toDegrees) | 0
      val diff        = Coordinates.difference(ctx.getBaseCoordinates.getValue.toNewModel, c).distance.toDegrees
      diff > minDistance
    }

    for {
      coords  <- genCandidates(ctx)
      targets <- sequence(coords.collect { case c if farEnough(c) => genGuideStar(ctx, c) })
    } yield targets
  }


  implicit val arbTest: Arbitrary[(ObsContext, List[SiderealTarget])] =
    Arbitrary {
      for {
        env  <- arbitrary[TargetEnvironment]
        gmos <- arbitrary[InstGmosNorth]
        offs <- arbitrary[java.util.Set[SkyCalcOffset]]
        pac  <- Gen.oneOf(FIXED, FIXED_180)
        ctx   = ObsContext.create(env, gmos <| (_.setPosAngleConstraint(pac)), Conditions.NOMINAL, offs, null, ImOption.empty())
        gs   <- genGuideStars(ctx)
      } yield (ctx, gs)
    }

  def ctx180(ctx: ObsContext): ObsContext =
    ctx.withPositionAngle(ctx.getPositionAngle.flip)

  def contexts(ctx: ObsContext): List[ObsContext] =
    ctx.getInstrument match {
      case paca: PosAngleConstraintAware if paca.getPosAngleConstraint == FIXED_180 =>
        List(ctx, ctx180(ctx))
      case _ =>
        List(ctx)
    }

  def analyze(strategy: AgsStrategy, ctx: ObsContext, guideStar: SiderealTarget): AgsGuideQuality =
    strategy.analyze(ctx, magTable, GmosOiwfsGuideProbe.instance, guideStar).get.quality

  "SingleProbeStrategy" should {
    // This is just a slightly different, more direct/less efficient, means of
    // calculating the brightest, least vignetting star than the
    // SingleProbeStrategy uses.  Really all that is being tested here is the
    // magnitude comparison.
    "pick the brightest candidate when they have equal vignetting" !
      forAll { (env: (ObsContext, List[SiderealTarget])) =>
        val (ctx, candidates) = env

        val strategy  = SingleProbeStrategy(GmosNorthOiwfsKey, SingleProbeStrategyParams.GmosOiwfsParams(Site.GN), CannedBackend(candidates))
        val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

        val analyzedCandidates = for {
          gs <- candidates
          c  <- contexts(ctx)
          q = analyze(strategy, c, gs)
          if q < AgsGuideQuality.PossiblyUnusable
          m  <- strategy.params.referenceMagnitude(gs)
        } yield (gs, c, q, GmosOiwfsGuideProbe.instance.calculator(c).calc(gs.coordinates), m.value)

        analyzedCandidates match {
          case Nil =>
            selection.isEmpty

          case acs =>
            val best              = acs.minBy(_._3)._3
            val qualityCandidates = analyzedCandidates.filter(_._3 == best)
            val sorted = qualityCandidates.sortWith { case ((_,_,_,vig0,mag0), (_,_,_,vig1,mag1)) =>
              if (vig0 == vig1) mag0 < mag1 else vig0 < vig1
            }
            val empty = List.empty[(SiderealTarget, ObsContext, AgsGuideQuality, Double, Double)]
            val winners = sorted.headOption.fold(empty) { case (_, _, _, vig0,mag0) =>
              sorted.takeWhile { case (_, _, _, vig1, mag1) =>
                (vig0 == vig1) && (mag0 == mag1)
              }
            }

            def almostEqual(d0: Double, d1: Double) = math.abs(d0 - d1) < 0.000001

            val res = selection.exists { sel =>
              winners.exists { case (gs, c, _, _, _) =>
                almostEqual(sel.posAngle.toDegrees, c.getPositionAngle.toDegrees) &&
                  sel.assignments.exists { _.guideStar == gs }
              }
            }

            if (!res) {
              println("Selection is: " + selection)
              println("  " + selection.flatMap(_.assignments.headOption.map(_.guideStar.coordinates.shows)))
              println("Winners are.: " + winners.mkString("\n\t", "\n\t", ""))

              val gmosN = ctx.getInstrument.asInstanceOf[InstGmosNorth]
              println(
              s"""--- Test Env ---
                 |  Base        = ${ctx.getBaseCoordinates.getValue.toNewModel.shows}
                 |  Instrument:
                 |    Pos Angle = ${ctx.getPositionAngle}
                 |    PA Mode   = ${gmosN.getPosAngleConstraint}
                 |    ISS Port  = ${ctx.getIssPort}
                 |    Mode      = ${gmosN.getFPUnitMode}
                 |    IFU       = ${gmosN.getFPUnit}
                 |  Conditions  = ${ctx.getConditions}
                 |  Offsets     = ${ctx.getSciencePositions.asScala.map(_.toNewModel.shows).mkString(",")}
                 |  Candidates:
                 |    ${winners.map { case (gs, c, _, _, _) =>
                         s"${gs.coordinates.shows} ${c.getPositionAngle}"
                       }.mkString("\n    ")}
               """.stripMargin
              )
            }
            res
        }
      }
  }

}
