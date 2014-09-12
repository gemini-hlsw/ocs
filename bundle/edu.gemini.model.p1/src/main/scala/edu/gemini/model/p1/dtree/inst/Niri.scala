package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree._
import edu.gemini.model.p1.immutable._

object Niri {
  def apply() = new AdaptiveOpticsNode

  class AdaptiveOpticsNode extends SingleSelectNode[Unit, Altair, Altair](()) with AltairNode {
    override val allGuideStarTypes = false
    def apply(a: Altair) = Left(new CameraNode(a))
    def unapply = {
      case b: NiriBlueprint => b.altair
    }
  }

  class CameraNode(s: Altair) extends SingleSelectNode[Altair, NiriCamera, (Altair, NiriCamera)](s) {
    val title       = "Camera"
    val description = "Please select the camera."
    val choices     = s match {
      case AltairNone => NiriCamera.values.toList
      case _          => NiriCamera.values.filterNot(_ == NiriCamera.F6).toList
    }

    override def default: Option[NiriCamera] = s match {
      case AltairNone => Some(NiriCamera.F6)
      case _          => Some(NiriCamera.F32)
    }

    def apply(c: NiriCamera) = Left(new FilterNode((s, c)))

    def unapply = {
      case b: NiriBlueprint => b.camera
    }
  }

  import NiriFilter._
  lazy val NOT_F6_COMPATIBLE  = Set(BBF_LPRIME, BBF_MPRIME)
  lazy val NOT_F14_COMPATIBLE = Set(BBF_MPRIME)

  class FilterNode(s: (Altair, NiriCamera)) extends MultiSelectNode[(Altair, NiriCamera), NiriFilter, NiriBlueprint](s) {
    val title     = "Filters"
    val description = "Select at least one filter for your configuration.  More than one filter may be selected."

    val (altair, camera) = s

    def choices: List[NiriFilter] =
      camera match {
        case NiriCamera.F6  => NiriFilter.values.filterNot(NOT_F6_COMPATIBLE.contains).toList
        case NiriCamera.F14 => NiriFilter.values.filterNot(NOT_F14_COMPATIBLE.contains).toList
        case NiriCamera.F32 => NiriFilter.values.toList
      }

    def apply(fs: List[NiriFilter]) = Right(NiriBlueprint(altair, camera, fs))
    def unapply = {
      case b: NiriBlueprint => b.filters
    }
  }
}