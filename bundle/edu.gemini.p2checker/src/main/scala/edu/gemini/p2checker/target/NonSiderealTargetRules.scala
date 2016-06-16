package edu.gemini.p2checker.target

import edu.gemini.p2checker.api.{P2Problems, IP2Problems, ObservationElements, IRule}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core._
import edu.gemini.spModel.obs.context.ObsContext
import scalaz._, Scalaz._

final class NonSiderealTargetRules extends IRule {
  import NonSiderealTargetRules._

  implicit class SemesterOps(s: Semester) {
    def contains(time: Long, site: Site): Boolean =
      time >= s.getStartDate(site).getTime &&
      time <=   s.getEndDate(site).getTime
  }

  override def check(es: ObservationElements): IP2Problems = {
    val p2p = new P2Problems
    p2p.append(checkNoHorizonsDesignation(es))
    p2p.append(checkNoSchedulingBlock(es))
    p2p.append(checkNoEphemerisForSchedulingBlock(es))
    p2p.append(checkEphemerisTooSparse(es))
    p2p.append(checkEphemerisWrongSite(es))
    p2p
  }

  def checkNoHorizonsDesignation(es: ObservationElements): IP2Problems =
    new P2Problems <| { p2p =>
      for {
        ocn <- es.getTargetObsComponentNode.asScalaOpt.toList
        toc <- es.getTargetObsComp.asScalaOpt.toList
        tar <- toc.getTargetEnvironment.getTargets.asScalaList
        nst <- tar.getNonSiderealTarget.toList
               if nst.horizonsDesignation.isEmpty
      } p2p.addError(ERR_NO_HORIZONS_DESIGNATION,
          s"Target ${nst.name} has no HORIZONS identifier; high-resolution ephemeris cannot be updated automatically.",
          ocn)
    }

  def checkNoSchedulingBlock(es: ObservationElements): IP2Problems =
    new P2Problems <| { p2p =>
      for {
        ocn <- es.getTargetObsComponentNode.asScalaOpt
        toc <- es.getTargetObsComp.asScalaOpt
        if toc.getTargetEnvironment.getTargets.asScalaList.exists(_.isNonSidereal) &&
           es.getSchedulingBlock.isEmpty
      } p2p.addError(ERR_NO_SCHEDULING_BLOCK,
          s"Observation ${Option(es.getObservationNode.getObservationID).getOrElse(es.getObservation.getTitle)} has nonsidereal targets but no scheduling block.",
          ocn)
    }

  def checkNoEphemerisForSchedulingBlock(es: ObservationElements): IP2Problems =
    new P2Problems <| { p2p =>
      for {
        ocn <- es.getTargetObsComponentNode.asScalaOpt.toList
        toc <- es.getTargetObsComp.asScalaOpt.toList
        tar <- toc.getTargetEnvironment.getTargets.asScalaList
        nst <- tar.getNonSiderealTarget.toList
        sb  <- es.getObservation.getSchedulingBlock.asScalaOpt
               if nst.coords(sb.start).isEmpty
      } p2p.addError(ERR_NO_EPHEMERIS_FOR_BLOCK,
          s"Target ${nst.name} has no defined coordinates for its scheduling block.",
          ocn)
    }

  def checkEphemerisTooSparse(es: ObservationElements): IP2Problems =
    new P2Problems <| { p2p =>
      for {
        ocn <- es.getTargetObsComponentNode.asScalaOpt.toList
        toc <- es.getTargetObsComp.asScalaOpt.toList
        tar <- toc.getTargetEnvironment.getTargets.asScalaList
        nst <- tar.getNonSiderealTarget.toList
        sb  <- es.getObservation.getSchedulingBlock.asScalaOpt
        cs  <- nst.coords(sb.start)
        d   <- nst.ephemeris.lookupClosest(sb.start).map(cs.angularDistance(_).toArcmins)
        if d > 10 // arcmins
      } p2p.addError(ERR_EPHEMERIS_TOO_SPARSE,
        f"""
           |Nearest ephemeris data point for ${nst.name}%s is ${d.toInt}' away.
           |Refresh to re-center the high-resolution portion of the ephemeris.
         """.stripMargin,
        ocn)
    }

  def checkEphemerisWrongSite(es: ObservationElements): IP2Problems =
    new P2Problems <| { p2p =>
      for {
        ocn <- es.getTargetObsComponentNode.asScalaOpt.toList
        toc <- es.getTargetObsComp.asScalaOpt.toList
        tar <- toc.getTargetEnvironment.getTargets.asScalaList
        nst <- tar.getNonSiderealTarget.toList
        sit <- ObsContext.getSiteFromObservation(es.getObservationNode).asScalaOpt.toList
        eph  = nst.ephemeris
        if eph.size > 1 && eph.site != sit
      } p2p.addError(ERR_EPHEMERIS_WRONG_SITE,
        s"The ephemeris for ${nst.name} was computed for ${eph.site} but the observation is " +
        s"associated with ${sit}. Refresh the ephemeris to correct this.",
        ocn)
    }

}

object NonSiderealTargetRules {

  val ERR_NO_SCHEDULING_BLOCK     = "NoSchedulingBlock"
  val ERR_NO_EPHEMERIS_FOR_BLOCK  = "NoEphemerisForSchedulingBlock"
  val ERR_EPHEMERIS_TOO_SPARSE    = "EphemerisTooSparse"
  val ERR_NO_HORIZONS_DESIGNATION = "NoHorizonsDesignation"
  val ERR_EPHEMERIS_WRONG_SITE    = "EphemerisWrongSite"

}