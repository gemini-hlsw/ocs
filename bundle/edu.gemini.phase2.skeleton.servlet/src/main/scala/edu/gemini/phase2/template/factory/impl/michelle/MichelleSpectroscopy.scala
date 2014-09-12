package edu.gemini.phase2.template.factory.impl.michelle

import edu.gemini.spModel.gemini.michelle.blueprint.SpMichelleBlueprintSpectroscopy
import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.gemini.michelle.MichelleParams._

case class MichelleSpectroscopy(blueprint:SpMichelleBlueprintSpectroscopy) extends MichelleBase[SpMichelleBlueprintSpectroscopy] {

  import blueprint._

  // Local Imports
  import Disperser.{LOW_RES_10 => LowN, LOW_RES_20 => LowQ}

  // INCLUDE note 'README FOR SPECTROSCOPY' at top level of program (only needed once)
  addNote("README FOR SPECTROSCOPY") in TopLevel

  // INCLUDE {4} - {12} in a Target Group
  // 	SET FPM FROM PI
  // 	SET DISPERSER FROM PI
  include(4 to 12:_*) in TargetGroup
  forGroup(TargetGroup)(
    setMask(fpu),
    setDisperser(disperser))

  // IF DISPERSER FROM PI == LowN OR LowQ,
  // 	SET TOTAL ON-SOURCE TIME TO 600.0 in bp10
  // ELSE
  // 	SET TOTAL ON-SOURCE TIME TO 1800.0 in bp10
  // 	INCLUDE note 'Using asteroids as standards' at top level of
  // 	program (only needed once)
  if (disperser == LowN || disperser == LowQ) {
    forObs(10)(
      setTimeOnSource(600.0))
  } else {
    forObs(10)(
      setTimeOnSource(1800.0))
    addNote("Using asteroids as standards") in TopLevel
  }


}
