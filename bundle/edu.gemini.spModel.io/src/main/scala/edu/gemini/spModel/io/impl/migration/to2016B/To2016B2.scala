package edu.gemini.spModel.io.impl.migration.to2016B

import edu.gemini.spModel.io.impl.migration.Migration
import edu.gemini.spModel.obs.ObsParamSetCodecs._
import edu.gemini.spModel.obs.SchedulingBlock
import edu.gemini.spModel.obs.SchedulingBlock.Duration
import edu.gemini.spModel.obs.SchedulingBlock.Duration._
import edu.gemini.spModel.pio.{Document, Version}
import edu.gemini.spModel.pio.codec._
import edu.gemini.spModel.io.impl.migration.PioSyntax._

object To2016B2 extends Migration {

  val version = Version.`match`("2016B-2")

  val kSb         = "schedulingBlock";
  val kSbStart    = "schedulingBlockStart";
  val kSbDuration = "schedulingBlockDuration";

  val conversions: List[Document => Unit] = List(
    updateSchedulingBlocks // order matters!
  )

  // If there is a scheduling block, replace the old 2-Param encoding with the new ParamSet
  // encoding, interpreting any existing duration as explicit. There is no way to distinguish in
  // the old model.
  def updateSchedulingBlocks(d: Document): Unit =
    for {
      o <- obs(d)
      s <- o.long(kSbStart)
      d <- List(o.long(kSbDuration).fold[Duration](Unstated)(Explicit(_)))
    } {
      o.removeChild(kSbStart)
      o.removeChild(kSbDuration)
      o.addParamSet(SchedulingBlock(s, d).encode(kSb))
    }

}



