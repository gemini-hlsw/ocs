package edu.gemini.phase2.template.factory.impl.michelle

import scala.collection.JavaConverters._
import edu.gemini.spModel.gemini.michelle.blueprint.SpMichelleBlueprintImaging
import edu.gemini.pot.sp.{ISPObservation, ISPGroup}
import edu.gemini.spModel.data.YesNoType

case class MichelleImaging(blueprint: SpMichelleBlueprintImaging) extends MichelleBase[SpMichelleBlueprintImaging] {

  import blueprint._

  // INCLUDE {1} - {3} in a Target Group
  // 	SET FILTER FROM PI INTO MICHELLE ITERATOR
  // 	SET POLARIMETRY FROM PI
  // 	IF POLARIMETRY == YES INCLUDE note 'Polarimetry' at top
  // 	level of the program (only neeed once)

  include(1, 2, 3) in TargetGroup
  forGroup(TargetGroup)(
    setFilter(filters.asScala.head),
    mutateSeq(iterate(PARAM_FILTER, filters.asScala)),
    setPolarimetry(polarimetry))
  if (polarimetry)
    addNote("Polarimetry") in TopLevel

}
