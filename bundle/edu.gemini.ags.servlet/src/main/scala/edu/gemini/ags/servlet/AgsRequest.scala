package edu.gemini.ags.servlet

import edu.gemini.skycalc.Offset
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gmos.{InstGmosNorth, InstGmosSouth}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.obs.SchedulingBlock
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment

import TargetType._


import scalaz._
import Scalaz._


final case class AgsRequest(
  site:        Site,
  coordinates: Coordinates,
  targetType:  TargetType,
  conditions:  Conditions,
  instrument:  AgsInstrument
) {

  def target: Target =
    targetType match {
      case Sidereal    => SiderealTarget("", coordinates, None, None, None, Nil, None, None)
      case NonSidereal => NonSiderealTarget("", Ephemeris.singleton(site, 0L, coordinates), None, Nil, None, None)
    }

  def spTarget: SPTarget =
    new SPTarget(target)

  /**
   * Constructs an `ObsContext` from the request. The AGS service needs an
   * `ObsContext`, but that contains a lot more information than we need in
   * order to do an AGS search. This method creates a context that contains
   * the request data and defaults unimportant details.
   */
  def toContext: \/[String, ObsContext] =
    instrument.instObsComp.map { inst =>
      ObsContext.create(
        TargetEnvironment.create(spTarget),
        inst,
        conditions,
        java.util.Collections.emptySet[Offset],
        null, // :-(
        edu.gemini.shared.util.immutable.ImOption.empty[SchedulingBlock]
      )
    }

}