package edu.gemini.phase2.template.factory.impl.nici

import edu.gemini.spModel.gemini.nici.blueprint.SpNiciBlueprintCoronagraphic
import edu.gemini.spModel.gemini.nici.NICIParams.{Channel2FW, Channel1FW}
import scala.collection.JavaConverters._

case class NiciCoronographic(blueprint:SpNiciBlueprintCoronagraphic) extends NiciBase[SpNiciBlueprintCoronagraphic] {

  import blueprint._

  // **** IF MODE == Coronagraphic ****
  // INCLUDE {3},{4}  in target-specific Scheduling Group
  //         SET DICHROIC FROM PI
  //         SET RED CHANNEL FILTER FROM PI if defined ELSE SET to "Block"
  //         SET BLUE CHANNEL FILTER FROM PI if defined ELSE SET to "Block"
  //         SET FPM FROM PI

  include(3, 4) in TargetGroup
  forGroup(TargetGroup)(
    setDichroic(dichroic),
    setRedChannelFilter(firstRedFilterOrBlock),
    setBlueChannelFilter(firstBlueFilterOrBlock),
    setFPM(fpm))

}
