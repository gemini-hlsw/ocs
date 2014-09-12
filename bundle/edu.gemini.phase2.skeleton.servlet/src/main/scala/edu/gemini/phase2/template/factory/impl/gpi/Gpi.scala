package edu.gemini.phase2.template.factory.impl.gpi

import edu.gemini.spModel.gemini.gpi.blueprint.SpGpiBlueprint
import edu.gemini.spModel.gemini.gpi.{Gpi => InstGpi}
import edu.gemini.spModel.gemini.gpi.Gpi.Disperser.{PRISM, WOLLASTON}

/*
Instrument: GPI
Blueprint templates: GPI_BP.xml

Last update: 2014 May 09, Fredrik Rantakyro

Observations are now identified by library IDs, indicated with {}

PI = Phase I
{} = Library ID

SET OBSERVING MODE FROM PI

IF DISPERSER == PRISM:
  IF FILTER == {Y or J or H}:  # No sky required
     INCLUDE {1} {2} {4}
  IF FILTER == {K1 or K2}:     # Sky required
    INCLUDE {1} {3} {4}
    CHANGE FILTER in {4} to H  # See Filter Change note below

ELSE IF DISPERSER == WOLLASTON:
  IF FILTER == {Y or J or H}: # No sky required
    INCLUDE {5} {6}
  IF FILTER == {K1 or K2}:    # Sky required
    INCLUDE {5} {7}

# Filter Change:  the goal is to leave the mode the same but change the filter
# from {K1 or K2} to {H}.  For example, if the mode is "Coronagraph K1" change
# it to "Coronagraph H".  If it is "K2 Direct" change it to "H Direct".  If it
# is "Non-Redundant K1" change it to "Non-Redundant H".
*/

case class Gpi(blueprint:SpGpiBlueprint) extends GpiBase[SpGpiBlueprint] {
  import blueprint._
  import GpiFilterGroup.{Yjh, K1k2}

  val changeFilterToH = mutateStatic[InstGpi.ObservingMode] { (gpi, m) =>
    gpi.setFilter(InstGpi.Filter.H)
    gpi.setObservingMode(new edu.gemini.shared.util.immutable.Some(m.correspondingH()))
  }

  val incList = GpiFilterGroup.lookup(observingMode).map { fg =>
    (disperser, fg) match {
      case (PRISM, Yjh)      => List(1,2,4)
      case (PRISM, K1k2)     => List(1,3,4)
      case (WOLLASTON, Yjh)  => List(5,6)
      case (WOLLASTON, K1k2) => List(5,7)
      case _                 => Nil
    }
  }.getOrElse(Nil)

  include(incList: _*) in TargetGroup
  forGroup(TargetGroup)(setObservingMode(observingMode))

  GpiFilterGroup.lookup(observingMode).foreach { fg =>
    (disperser, fg) match {
      case (PRISM, K1k2) => forObs(4)(changeFilterToH(observingMode))
      case _             => // nothing else required
    }
  }
}
