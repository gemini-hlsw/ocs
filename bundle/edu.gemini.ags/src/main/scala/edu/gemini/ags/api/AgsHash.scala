package edu.gemini.ags.api

import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.shared.util.immutable.{Option => GemOption}
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.gemini.altair.InstAltair
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.{InstGmosNorth, InstGmosSouth}
import edu.gemini.spModel.obs.context.ObsContext

import java.time.Instant

import scala.collection.mutable.ListBuffer
import scala.util.hashing.{MurmurHash3 => M3}

import scala.collection.JavaConverters._

/** Computes an `Int` hash value that corresponds to the set of inputs to the
  * AGS lookup.  If an observation is changed in some way and yet the hash
  * algorithm computes the same value, there is no need to re-do the AGS search.
  */
object AgsHash {

  /** Calculates the AGS hash for this context, using the current base position
    * location.
    *
    * <em>Warning.</em> This method is not referentially transparent as it may
    * provide distinct answers for the same input at different times
    * (particularly for non-sidereal targets).
    */
  def hashNow(ctx: ObsContext): Int =
    hash(ctx, Instant.now())

  /** Calculates the AGS hash for this context, calculating the base position
    * corresponding to the given time.
    */
  def hash(ctx: ObsContext, when: Instant): Int =
    hash(ctx, when.toEpochMilli)


  /** Calculates the AGS hash for this context, calculating the base position
    * corresponding to the given time expressed in milliseconds since 1970.
    */
  def hash(ctx: ObsContext, when: Long): Int = {

    val buf = ListBuffer.empty[Int]

    // AGS Strategy
    val strategyKey = AgsRegistrar.currentStrategy(ctx).map(_.key)
    strategyKey.foreach { s =>
      M3.stringHash(s.id) +=: buf
    }

    // Conditions
    Option(ctx.getConditions).foreach { c =>
      c.cc.## +=: c.iq.## +=: c.sb.## +=: buf
    }

    // Asterism
    Option(ctx.getTargets).foreach { t =>
      val time = Some(new java.lang.Long(when)).asGeminiOpt
      val asterism = t.getAsterism

      def toData(coord: GemOption[java.lang.Double]): Int =
        coord.asScalaOpt.map(_.doubleValue).##

      asterism.allSpTargets.map { sp =>
        val ra  = toData(sp.getRaDegrees(time))
        val dec = toData(sp.getDecDegrees(time))
        ra +=: dec +=: buf
      }

    }

    // Offset Positions, which are returned in a Set.  Order is not important
    // for the purpose of AGS calculations.
    M3.unorderedHash(ctx.getSciencePositions.asScala.map { o =>
      (o.p.arcsec, o.q.arcsec)
    }) +=: buf

    // Position Angle
    Option(ctx.getPositionAngle).foreach { a =>
      a.toDegrees.## +=: buf
    }

    // Position Angle Constraint
    Option(ctx.getPosAngleConstraint).foreach { pac =>
      pac.## +=: buf
    }

    // IssPort
    Option(ctx.getIssPort).foreach { iss =>
      iss.## +=: buf
    }

    import AgsStrategyKey._

    // Vignetting calculation. Instrument-specific features that have an impact
    // on the science area and probe arm position and hence, vignetting.
    Option(ctx.getInstrument).foreach {
      case i: Flamingos2 if strategyKey.contains(Flamingos2OiwfsKey)   =>
        i.getFpu.## +=: i.getLyotWheel.getPlateScale.## +=: buf

      case i: InstGmosNorth if strategyKey.contains(GmosNorthOiwfsKey) =>
        i.getFPUnit.## +=: i.getFPUnitMode.## +=: buf

      case i: InstGmosSouth if strategyKey.contains(GmosSouthOiwfsKey) =>
        i.getFPUnit.## +=: i.getFPUnitMode.## +=: buf

      case _                                                           =>
    }

    // Strategy-specific differences.  In some instruments PWFS vignetting
    // clearance changes based on instrument-specific features such as which
    // camera is in use.
    strategyKey.foreach {
      case Pwfs1NorthKey | Pwfs1SouthKey =>
        Option(ctx.getInstrument).foreach {
          _.pwfs1VignettingClearance.getMagnitude.## +=: buf
        }

      case Pwfs2NorthKey | Pwfs2SouthKey =>
        Option(ctx.getInstrument).foreach {
          _.pwfs2VignettingClearance.getMagnitude.## +=: buf
        }

      case GemsKey                       =>
        // GeMS works differently depending upon whether GSAOI or F2 is in use.
        Option(ctx.getInstrument).map(_.getPhaseIResourceName).foreach { n =>
          M3.stringHash(n) +=: buf
        }

      case _                             =>
    }

    // Altair mode, which impacts not only the strategy but also the
    // magnitude limits.
    ctx.getAOComponent.asScalaOpt.foreach {
      case a: InstAltair => a.getMode.## +=: buf
      case _             =>
    }

    M3.orderedHash(buf)
  }

  private implicit class AngleOps(a: edu.gemini.skycalc.Angle) {
    def degrees: Double =
      a.toDegrees.getMagnitude

    def arcsec: Double =
      a.toArcsecs.getMagnitude
  }
}
