package edu.gemini.spModel.inst

import java.awt.geom.{Point2D, AffineTransform}

import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gmos._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.telescope.PosAngleConstraintAware
import edu.gemini.spModel.telescope.PosAngleConstraint.FIXED_180

import org.scalacheck._

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

trait VignettingArbitraries extends edu.gemini.spModel.test.SpModelArbitraries {
  // Generate guide star candidates at all position angles supported by the
  // instrument configuration in the context.
  def genCandidates(ctx: ObsContext): Gen[List[Coordinates]] =
    ctx.getInstrument match {
      case paca: PosAngleConstraintAware if paca.getPosAngleConstraint == FIXED_180 =>
        val ctx180 = ctx.withPositionAngle(ctx.getPositionAngle.flip)
        for {
          a0 <- genFixedPosAngleCandidates(ctx)
          a1 <- genFixedPosAngleCandidates(ctx180)
        } yield a0 ++ a1
      case _ =>
        // FIXED for now
        genFixedPosAngleCandidates(ctx)
    }

  // Generate candidates for the specific position angle in the provided context
  def genFixedPosAngleCandidates(ctx: ObsContext): Gen[List[Coordinates]] = {
    // Get the (un-rotated) usable area at the position angle. Get the "safe"
    // area because rounding errors in back and forth model translations can
    // make edge cases appear to be in or out arbitrarily.
    val patField = GmosOiwfsGuideProbe.instance.getCorrectedPatrolField(ctx).getValue
    val usable   = patField.safeOffsetIntersection(ctx.getSciencePositions).getBounds2D

    // Generate a candidate that falls in the usable area.
    val genCandidate =
      for {
        p  <- Gen.chooseNum(usable.getMinX, usable.getMaxX)
        q  <- Gen.chooseNum(usable.getMinY, usable.getMaxY)
      } yield {
        // Rotate the offset in p and q by the position angle. To get the
        // p delta and q delta (which are negated because screen coords
        // are flipped)
        val rot      = AffineTransform.getRotateInstance(-ctx.getPositionAngle.toRadians)
        val offsetPt = rot.transform(new Point2D.Double(p, q), new Point2D.Double())
        val pd       = -offsetPt.getX // arcsecs
        val qd       = -offsetPt.getY // arcsecs

        // The deltaDec is just qd but we have to adjust the offset in p
        // to take into account the declination.
        //    p = delta RA * cos(dec)
        // so
        //    deltaRA = p / cos(dec)
        val base     = ctx.getBaseCoordinates.getValue.toNewModel
        val cos      = math.cos(math.toRadians(base.dec.toDegrees))
        val deltaRa  = if (cos == 0) 0.0 else pd/cos
        val deltaDec = qd

        // Finally we have a candidate that falls in the usable area.
        base.offset(Angle.fromArcsecs(deltaRa), Angle.fromArcsecs(deltaDec))
      }

    for {
      count <- Gen.chooseNum(0, 100)
      cands <- Gen.listOfN(count, genCandidate)
    } yield cands
  }
}
