package edu.gemini.spModel.inst

import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.ImOption
import edu.gemini.spModel.ags.AgsStrategyKey.GmosNorthOiwfsKey
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gmos._
import edu.gemini.spModel.guide.VignettingCalculator
import edu.gemini.spModel.inst.FeatureGeometry._
import edu.gemini.spModel.obs.context.ObsContext

import org.scalacheck._
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

import edu.gemini.shared.util.immutable.ScalaConverters._

object VignettingCalcSpec extends Specification with ScalaCheck with VignettingArbitraries with Helpers {

  case class TestEnv(ctx: ObsContext, candidates: List[Coordinates]) {
    val vc = VignettingCalculator(ctx, GmosOiwfsProbeArm, GmosScienceAreaGeometry)

    override def toString: String = {
      val gmosN = ctx.getInstrument.asInstanceOf[InstGmosNorth]

      s"""---- Test Env ----
         |  Base        = ${ctx.getBaseCoordinates.asScalaOpt.map(_.toNewModel.shows)}
         |  Instrument:
         |    Pos Angle = ${ctx.getPositionAngle}
         |    ISS Port  = ${ctx.getIssPort}
         |    Mode      = ${gmosN.getFPUnitMode}
         |    IFU       = ${gmosN.getFPUnit}
         |  Offsets     = ${ctx.getSciencePositions.asScala.map(_.toNewModel.shows).mkString(",")}
         |  Candidates:
         |    ${candidates.map(_.shows).mkString("\n    ")}
      """.stripMargin
    }
  }

  implicit val arbTest: Arbitrary[TestEnv] =
    Arbitrary {
      for {
        ctx  <- arbitrary[ObsContext]
        gmos <- arbitrary[InstGmosNorth]
        gmosCtx = ctx.withInstrument(gmos).withAgsStrategyOverride(ImOption.apply(GmosNorthOiwfsKey))
        can  <- genCandidates(gmosCtx)
      } yield TestEnv(gmosCtx, can)
    }

  "VignettingCalculator" should {
    "generate vignetting ratios" !
      forAll { (env: TestEnv) =>
        // maximum vignetting is 1.0 but since the GMOS probe arm is much
        // smaller than the gmos science area for imaging, we can reduce it
        // even further sometimes.  For spectroscopy, the slit is small
        // so we stick with 1.0 in those cases
        val gmosArea     = GmosScienceAreaGeometry.unadjustedGeometry(env.ctx).map(approximateArea)
        val maxProbeArea = GmosOiwfsProbeArm.unadjustedGeometry(env.ctx).map(approximateArea)
        val maxVig       = 1.0 min (maxProbeArea.get / gmosArea.get)

        env.candidates.forall { gs =>
          val vig = env.vc.calc(gs)
          (0 <= vig) && (vig <= maxVig)
        }
      }

    "only generate 0 vignetting if the guide star does not fall on the science area at any offset" !
      forAll { (env: TestEnv) =>
        // Figure out the offset from the base of each candidate that does not
        // vignette the science area.
        val zeroVigCandidates = env.candidates.filter(env.vc.calc(_) == 0)
        val base              = env.ctx.getBaseCoordinates.getValue.toNewModel
        val candidateOffsets  = zeroVigCandidates.map(Coordinates.difference(base, _).offset)

        // Check that at each offset, the candidate isn't on the science area.
        env.ctx.getSciencePositions.asScala.map(_.toNewModel).forall { off =>
          val geo = GmosScienceAreaGeometry.geometry(env.ctx, off).get
          candidateOffsets.forall { candidate => !geo.contains(candidate.toPoint) }
        }

        // Note it can be the case that the guide star causes vignetting w/o
        // falling on the science area, particularly for spectroscopy or when
        // using the IFU.
      }

    /*  Struggling to figure out a property that applies in all cases.

    "(for GMOS) calculate higher vignetting for candidates that fall in the half of the usable area closest to the base position + offset" !
      forAll { (env: TestEnv) =>
        val base    = env.ctx.getBaseCoordinates.toNewModel

        val pf      = GmosOiwfsGuideProbe.instance.getPatrolField
        val usable  = pf.usableArea(env.ctx)
        val unRot   = AffineTransform.getRotateInstance(env.ctx.getPositionAngle.toRadians.getMagnitude)
        val usable0 = unRot.createTransformedShape(usable)
        val bounds  = usable0 .getBounds2D
        val close0  = new Rectangle2D.Double(bounds.getX, bounds.getY, bounds.getWidth/2, bounds.getHeight)
        val far0     = new Area(usable0 ) <| (_.subtract(new Area(close0)))

        val rot     = AffineTransform.getRotateInstance(-env.ctx.getPositionAngle.toRadians.getMagnitude)
        val close   = rot.createTransformedShape(close0)
        val far     = rot.createTransformedShape(far0)

        env.ctx.getSciencePositions.asScala.map(_.toNewModel).forall { off =>
          // Separate the candidates into those that fall in the closest
          // quadrant vs. the rest.
          val points = env.candidates.map { coords =>
            val candidateOffset = Coordinates.difference(base, coords).offset
            val candidatePoint  = candidateOffset.toPoint
            (coords, candidatePoint)
          }

          val containedPoints = points.filter { case (_,p) => usable.contains(p) }
          val (closePoints, farPoints) = containedPoints.partition { case (_,p) =>
            close.contains(p)
          }

          val cTups = closePoints.map { case (c,_) => (c, env.vc.calc(c)) }
          val fTups = farPoints.map { case (f,_) => (f, env.vc.calc(f)) }

          cTups.forall { case (cCoords, cVig) =>
            fTups.forall { case (fCoords, fVig) =>
              val res = cVig >= fVig
              if (!res) {
                Console.err.println("FAILS: " + formatCoordinates(cCoords) + " \t" + formatCoordinates(fCoords) + " \t" + cVig + " < " + fVig)
              }
              res
            }
          }
        }
      }

      */
  }

}
