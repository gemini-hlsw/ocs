package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Nifs {

  def apply() = new AdaptiveOpticsNode

  class AdaptiveOpticsNode extends SingleSelectNode[Unit, Altair, Altair](()) with AltairNode {
    override val allGuideStarTypes = false
    def apply(a: Altair) = a match {
      case AltairNone => Left(new DisperserNode(a, d => new NifsBlueprint(d)))
      case _          => Left(new OccultingDiskNode(a))
    }

    def unapply = {
      case ao: NifsBlueprintAo => ao.altair
      case b: NifsBlueprint    => AltairNone
    }
  }

  class OccultingDiskNode(s: Altair) extends SingleSelectNode[Altair, NifsOccultingDisk, (Altair, NifsOccultingDisk)](s) {
    val title       = "Occulting Disk"
    val description = "Please select the occulting disk option."
    val choices     = NifsOccultingDisk.values.toList

    def apply(o: NifsOccultingDisk) = Left(new DisperserNode((s, o), d => NifsBlueprintAo(s, o, d)))

    override def default = Some(NifsOccultingDisk.OD_NONE)
    
    def unapply = {
      case b: NifsBlueprintAo => b.occultingDisk
    }
  }

  class DisperserNode[I](s: I, f: NifsDisperer => NifsBlueprintBase) extends SingleSelectNode[I, NifsDisperer, NifsBlueprintBase](s) {
    val title       = "Disperser"
    val description = "Please select the disperser."
    val choices     = NifsDisperser.values.toList

    def apply(d: NifsDisperer) = Right(f(d))

    def unapply = {
      case b: NifsBlueprintBase => b.disperser
    }
  }
}