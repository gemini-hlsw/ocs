package edu.gemini.qv

import java.time.Instant

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.spModel.core.Coordinates

/**
  * Created by rnorris on 5/23/17.
  */
package object plugin {


  // Some extra operations to get time-parameterized coordinates for an Obs. The built-in operations
  // on Obs (getRa/getDec) use the scheduling block and yield zero if coordinates are unknown. We
  // have the same fallback behavior here.
  implicit class ObsOps(o: Obs) {

    def coords(ctx: QvContext): Coordinates = {
      val time = Instant.ofEpochMilli(ctx.referenceDate)
      val opt  = o.getTargetEnvironment.getAsterism.basePosition(Some(time))
      opt.getOrElse(Coordinates.zero)
    }

    def raDeg(ctx: QvContext): Double =
      coords(ctx).ra.toDegrees

    def decDeg(ctx: QvContext): Double =
      coords(ctx).dec.toDegrees

  }

}
