package edu.gemini.catalog.image

import java.time.Instant

import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.shared.util.immutable.{Option => GemOption}
import edu.gemini.spModel.obs.context.ObsContext

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.util.hashing.{MurmurHash3 => M3}

/** Computes an `Int` hash value that corresponds to the set of inputs to the
  * Image loading, essentially the coordinates.
  * Closely based on {@see AgsHash}
  */
object ImageHash {
  /** Calculates the hash for this context, calculating the base position
    * corresponding to the given time.
    */
  def hash(ctx: ObsContext, when: Instant): Int =
    hash(ctx, when.toEpochMilli)

  /** Calculates the hash for this context, calculating the base position
    * corresponding to the given time expressed in milliseconds since 1970.
    */
  def hash(ctx: ObsContext, when: Long): Int = {

    val buf = ListBuffer.empty[Int]

    // Base Position
    Option(ctx.getTargets).foreach { t =>
      val time = Some(new java.lang.Long(when)).asGeminiOpt
      val base = t.getBase

      def toData(coord: GemOption[java.lang.Double]): Int =
        coord.asScalaOpt.map(_.doubleValue).##

      val ra  = toData(base.getRaDegrees(time))
      val dec = toData(base.getDecDegrees(time))

      ra +=: dec +=: buf
    }

    // Offset Positions, which are returned in a Set.  Order is not important
    M3.unorderedHash(ctx.getSciencePositions.asScala.map { o =>
      (o.p.arcsec, o.q.arcsec)
    }) +=: buf

    M3.orderedHash(buf)
  }

  private implicit class AngleOps(a: edu.gemini.skycalc.Angle) {
    def degrees: Double =
      a.toDegrees.getMagnitude

    def arcsec: Double =
      a.toArcsecs.getMagnitude
  }
}
