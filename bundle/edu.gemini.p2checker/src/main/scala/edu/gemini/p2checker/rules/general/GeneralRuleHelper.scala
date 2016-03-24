package edu.gemini.p2checker.rules.general

import edu.gemini.spModel.core.{NonSiderealTarget, TooTarget, SiderealTarget, Target}

import scalaz._, Scalaz._

class GeneralRuleHelper {

  def samePosition(a: Target, b: Target): Boolean =
    (a, b) match {

      // Sidereal targets must have identical coordinates, proper motion, redshift, and parallax
      case (SiderealTarget(_, c1, pm1, rs1, px1, _, _, _),
            SiderealTarget(_, c2, pm2, rs2, px2, _, _, _)) =>
        (c1.ra.toAngle.formatHMS, c1.dec.formatDMS, pm1, rs1, px1) ==
       ((c2.ra.toAngle.formatHMS, c2.dec.formatDMS, pm2, rs2, px2))

      // TOO targets are always identical
      case (TooTarget(_), TooTarget(_)) => true

      // NonSidereal must have the same ephemeris (good enough?)
      case (NonSiderealTarget(_, e1, _, _, _, _),
            NonSiderealTarget(_, e2, _, _, _, _)) => e1 === e2

      // Otherwise not the same position
      case _ => false

    }

}
