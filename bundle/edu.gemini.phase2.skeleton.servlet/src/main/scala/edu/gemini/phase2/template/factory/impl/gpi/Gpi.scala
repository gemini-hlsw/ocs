package edu.gemini.phase2.template.factory.impl.gpi

import edu.gemini.spModel.gemini.gpi.blueprint.SpGpiBlueprint
import edu.gemini.spModel.gemini.gpi.{Gpi => InstGpi}
import edu.gemini.spModel.gemini.gpi.Gpi.Disperser.{PRISM, WOLLASTON}

/*
Instrument: GPI
Blueprint templates: GPI_BP.xml

Version 2014 May 09, Fredrik Rantakyro
Version 2014 Nov 13, Andrew Stephens
Version 2014 Nov 19, Bryan Miller
Version 2015 Aug 24, Fredrik Rantakyro
# Change Note FR: Merged ARC into Acquisition for ALL Y, J and H PRISM
# ARC {4} now is fixed to H-direct and ONLY used for K-band (but fixed to H)
# Updated Next libID=9 (from 8)

Observations are identified by library IDs, indicated with {}

PI = Phase I
{} = Library ID

IF DISPERSER == PRISM:
  IF FILTER == {Y or J or H}:  # No sky required
     INCLUDE {8} {2}
  IF FILTER == {K1 or K2}:     # Sky required
    INCLUDE {1} {3} {4}

ELSE IF DISPERSER == WOLLASTON:
  IF FILTER == {Y or J or H}: # No sky required
    INCLUDE {5} {6}
  IF FILTER == {K1 or K2}:    # Sky required
    INCLUDE {5} {7}

SET OBSERVING MODE FROM PI (all included observations)

# {4} has a fixed Observing Mode (H Direct) and this should NOT change.
*/

case class Gpi(blueprint:SpGpiBlueprint) extends GpiBase[SpGpiBlueprint] {
  import blueprint._
  import GpiFilterGroup.{Yjh, K1k2}

  val incList = GpiFilterGroup.lookup(observingMode).map { fg =>
    (disperser, fg) match {
      case (PRISM, Yjh)      => List(8,2)
      case (PRISM, K1k2)     => List(1,3,4)
      case (WOLLASTON, Yjh)  => List(5,6)
      case (WOLLASTON, K1k2) => List(5,7)
      case _                 => Nil
    }
  }.getOrElse(Nil)

  include(incList: _*) in TargetGroup
  forObs(incList.filterNot(_ == 4): _*)(setObservingMode(observingMode))

}
