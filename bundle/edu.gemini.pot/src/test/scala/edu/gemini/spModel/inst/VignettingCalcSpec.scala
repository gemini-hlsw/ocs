package edu.gemini.spModel.inst

import java.awt.geom.{Point2D, AffineTransform}

import edu.gemini.pot.ModelConverters._
import edu.gemini.skycalc.{Offset => SkyCalcOffset}
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.spModel.gemini.gmos._
import edu.gemini.spModel.gemini.gmos.GmosCommonType.FPUnitMode.{BUILTIN, CUSTOM_MASK}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.guide.VignettingCalculator
import edu.gemini.spModel.inst.FeatureGeometry._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.IssPort

import org.scalacheck._
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

object VignettingCalcSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

  implicit val arbTargetEnv: Arbitrary[TargetEnvironment] =
    Arbitrary {
      arbitrary[Coordinates].map { c =>
        val ra  = c.ra.toAngle.toDegrees
        val dec = c.dec.toDegrees
        TargetEnvironment.create(new SPTarget(ra, dec))
      }
    }

  implicit val arbGmosN: Arbitrary[InstGmosNorth] =
    Arbitrary {
      for {
        fpu      <- Gen.oneOf(GmosNorthType.FPUnitNorth.values)
        mode     <- Gen.frequency((1, CUSTOM_MASK), (9, BUILTIN))
        port     <- Gen.oneOf(IssPort.values)
        posAngle <- arbitrary[Angle]
      } yield
        new InstGmosNorth             <|
              (_.setFPUnit(fpu))      <|
              (_.setFPUnitMode(mode)) <|
              (_.setIssPort(port))    <|
              (_.setPosAngle(posAngle.toDegrees))
    }

  val genSmallOffset: Gen[SkyCalcOffset] =
    for {
      pi <- Gen.chooseNum(-50, 50)
      qi <- Gen.chooseNum(-50, 50)
    } yield Offset(pi.toDouble.arcsecs[OffsetP], qi.toDouble.arcsecs[OffsetQ]).toOldModel

  implicit val arbSciencePosSet: Arbitrary[java.util.Set[SkyCalcOffset]] =
    Arbitrary {
      for {
        count <- Gen.chooseNum(1, 4)
        offs  <- Gen.listOfN(count, genSmallOffset)
      } yield new java.util.HashSet(offs.asJava)
    }

  implicit val arbContext: Arbitrary[ObsContext] =
    Arbitrary {
      for {
        env  <- arbitrary[TargetEnvironment]
        gmos <- arbitrary[InstGmosNorth]
        offs <- arbitrary[java.util.Set[SkyCalcOffset]]
      } yield ObsContext.create(env, gmos, Conditions.NOMINAL, offs, null)
    }

  def formatCoordinates(c: Coordinates): String =
    s"coordinates(${formatRa(c.ra)}, ${formatDec(c.dec)})"

  def formatRa(ra: RightAscension): String = Angle.formatHMS(ra.toAngle)
  def formatDec(dec: Declination): String = Declination.formatDMS(dec)

  case class TestEnv(ctx: ObsContext, candidates: List[Coordinates]) {
    val vc = VignettingCalculator(ctx, GmosNorthOiwfsProbeArm, GmosScienceAreaGeometry)

    override def toString: String = {
      val gmosN = ctx.getInstrument.asInstanceOf[InstGmosNorth]

      s"""---- Test Env ----
         |  Base        = ${formatCoordinates(ctx.getBaseCoordinates.toNewModel)}
         |  Instrument:
         |    Pos Angle = ${ctx.getPositionAngle}
         |    ISS Port  = ${ctx.getIssPort}
         |    Mode      = ${gmosN.getFPUnitMode}
         |    IFU       = ${gmosN.getFPUnit}
         |  Offsets     = ${ctx.getSciencePositions.asScala.map(_.toNewModel.shows).mkString(",")}
         |  Candidates:
         |    ${candidates.map(formatCoordinates).mkString("\n    ")}
      """.stripMargin
    }
  }

  implicit val arbTest: Arbitrary[TestEnv] =
    Arbitrary {
      arbitrary[ObsContext].flatMap { ctx =>
        // Get the usable area at the position angle.
        val usable  = GmosOiwfsGuideProbe.instance.getCorrectedPatrolField(ctx).getValue.usableArea(ctx)

        // Un-rotate the usable area to bring to position angle 0. Note, pos
        // angle rotates counter clockwise whereas AffineTransform rotates
        // clockwise.
        val unRot   = AffineTransform.getRotateInstance(ctx.getPositionAngle.toRadians.getMagnitude)
        val usable0 = unRot.createTransformedShape(usable).getBounds2D

        // Generate a candidate that falls in (or just off) of the usable
        // area.
        val genCandidate =
          for {
            p  <- Gen.chooseNum(usable0.getMinX, usable0.getMaxX)
            q  <- Gen.chooseNum(usable0.getMinY, usable0.getMaxY)
          } yield {
            // Rotate the offset in p and q by the position angle. To get the
            // p delta and q delta (which are negated because screen coords
            // are flipped)
            val rot      = AffineTransform.getRotateInstance(-ctx.getPositionAngle.toRadians.getMagnitude)
            val offsetPt = rot.transform(new Point2D.Double(p, q), new Point2D.Double())
            val pd       = -offsetPt.getX // arcsecs
            val qd       = -offsetPt.getY // arcsecs

            // The deltaDec is just qd but we have to adjust the offset in p
            // to take into account the declination.
            //    p = delta RA * cos(dec)
            // so
            //    deltaRA = p / cos(dec)
            val base     = ctx.getBaseCoordinates.toNewModel
            val cos      = math.cos(math.toRadians(base.dec.toDegrees))
            val deltaRa  = if (cos == 0) 0.0 else pd/cos
            val deltaDec = qd

            // Finally we have a candidate that falls int the usable area.
            base.offset(Angle.fromArcsecs(deltaRa), Angle.fromArcsecs(deltaDec))
          }

        for {
          count <- Gen.chooseNum(0, 100)
          cands <- Gen.listOfN(count, genCandidate)
        } yield TestEnv(ctx, cands)
      }
    }

  "VignettingCalculator" should {
    "generate vignetting ratios" !
      forAll { (env: TestEnv) =>
        // maximum vignetting is 1.0 but since the GMOS probe arm is much
        // smaller than the gmos science area for imaging, we can reduce it
        // even further sometimes.  For spectroscopy, the slit is small
        // so we stick with 1.0 in those cases
        val gmosArea     = GmosScienceAreaGeometry.unadjustedGeometry(env.ctx).map(approximateArea)
        val maxProbeArea = GmosNorthOiwfsProbeArm.unadjustedGeometry(env.ctx).map(approximateArea)
        val maxVig       = 1.0 min (maxProbeArea.get / gmosArea.get)

        env.candidates.forall { gs =>
          val vig = env.vc.calc(gs)
          (0 <= vig) && (vig <= maxVig)
        }
      }

    "only generate 0 vignetting if the guide star does not all on the science area at any offset" !
      forAll { (env: TestEnv) =>
        // Figure out the offset from the base of each candidate that does not
        // vignette the science area.
        val zeroVigCandidates = env.candidates.filter(env.vc.calc(_) == 0)
        val base              = env.ctx.getBaseCoordinates.toNewModel
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
