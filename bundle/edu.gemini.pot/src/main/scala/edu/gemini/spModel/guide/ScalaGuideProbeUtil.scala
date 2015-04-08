package edu.gemini.spModel.guide

import java.awt.geom.{AffineTransform, Area}

import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.inst.{VignettableScienceAreaInstrument, FeatureGeometry, ProbeArmGeometry, ScienceAreaGeometry}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.telescope.IssPort

import scala.collection.JavaConversions._

object ScalaGuideProbeUtil {
  def calculateVignetting[I <: SPInstObsComp with VignettableScienceAreaInstrument](ctx0: ObsContext,
                                                                                    coordinates: Coordinates,
                                                                                    probeArmGeometry0: ProbeArmGeometry[I]): Double = {
    for {
      ctx                 <- Option(ctx0)
      probeArmGeometry    <- Option(probeArmGeometry0)
      inst                =  ctx.getInstrument.asInstanceOf[I]
      scienceAreaGeometry <- Option(inst.getVignettableScienceArea)
    } yield {
      val flip = if (ctx.getIssPort == IssPort.SIDE_LOOKING) -1.0 else 1.0
      val probeArmShapes = probeArmGeometry.geometry(inst)

      // Combine all the science areas together as we are only interested in the total final shape.
      val scienceArea = FeatureGeometry.transformScienceAreaForContext(scienceAreaGeometry.geometry, ctx).foldLeft(new Area) {
        case (area,s) =>
          area.add(new Area(s))
          area
      }
      val vignettingSum = ctx.getSciencePositions.toList.foldLeft(0.0){
        case (currentSum,offset) =>
          // Find the probe arm adjustment, which consists of the arm angle and guide star location in arcsec.
          // If an adjustment exists, calculate the vignetting for this offset.
          val vignetting = probeArmGeometry.armAdjustment(ctx, coordinates, offset).map { armAdjustment =>
            // Adjust the science area for the current offset.
            val x = -(offset.p.toNewModel.toNormalizedArcseconds)
            val y = -(offset.q.toNewModel.toNormalizedArcseconds * flip)
            val trans = AffineTransform.getTranslateInstance(x, y)
            val adjScienceArea = scienceArea.transform(trans)

            val probeArmArea = probeArmShapes.foldLeft(new Area){
              case (area,s) =>
                val sp = FeatureGeometry.transformProbeArmForContext(s, armAdjustment)
                area.add(new Area(sp))
                area
            }

            // Calculate the vignetting area, which is the intersection of the probe arm with the science area.
            val vignettingArea = new Area(scienceArea)
            vignettingArea.intersect(probeArmArea)

            // Now we approximate the vignetting, which is the ratio of the length of the diagonal of the bounding
            // box of the vignetting area to the length of the diagonal of the bounding box of the entire probe arm.
            def diagonal2(a: Area): Double = {
              val bounds = a.getBounds2D
              val w      = bounds.getWidth
              val h      = bounds.getHeight
              w * w + h * h
            }

            val vlength2 = diagonal2(vignettingArea)
            val plength2 = diagonal2(probeArmArea)
            if (plength2 > 0) math.sqrt(vlength2 / plength2) else 0.0
          }.getOrElse(0.0)

          currentSum + vignetting
      }

      // Now average out the vignetting across all the science positions.
      if (ctx.getSciencePositions.isEmpty) 0.0 else vignettingSum / ctx.getSciencePositions.size()
    }
  }.getOrElse(0.0)
}
