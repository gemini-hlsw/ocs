package edu.gemini.phase2.template.factory.impl.nici

import edu.gemini.spModel.gemini.nici.blueprint.SpNiciBlueprintStandard

case class NiciStandard(blueprint:SpNiciBlueprintStandard) extends NiciBase[SpNiciBlueprintStandard] {

  import blueprint._

  // **** IF MODE == Standard ****
  // INCLUDE {1},{2} in target-specific Scheduling Group for each filter or
  // red/blue channel filter pair in PI (probably not possible to
  // implement in 12B, just first filter or filter pair)
  //         SET DICHROIC FROM PI
  //         SET RED CHANNEL FILTER FROM PI if defined ELSE SET to "Block"
  //         SET BLUE CHANNEL FILTER FROM PI if defined ELSE SET to "Block"

  include(1, 2) in TargetGroup
  forGroup(TargetGroup)(
    setDichroic(dichroic),
    setRedChannelFilter(firstRedFilterOrBlock),
    setBlueChannelFilter(firstBlueFilterOrBlock))

}
