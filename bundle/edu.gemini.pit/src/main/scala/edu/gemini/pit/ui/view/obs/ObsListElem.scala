package edu.gemini.pit.ui.view.obs

import edu.gemini.model.p1.immutable._
import javax.swing.Icon
import edu.gemini.pit.ui.util.{CompositeIcon, SharedIcons}

/**
 * The obs list contains elements that represent observations or groups of observations, where
 * grouping is displayed via ordering and indentation in the list.
 */
sealed trait ObsListElem

sealed trait ObsGroup[A] extends ObsListElem {

  def grouping: ObsListGrouping[A]
  def toObs(band:Band) = Observation(b, c, t, band, None)

  def c: Option[Condition]
  def b: Option[BlueprintBase]
  def t: Option[Target]

  def icon: Icon
  def text: Option[String]
  def isEmpty: Boolean

  /** Left-OR. Replace the properties of e with my properties (when defined for me). */
  def |>[A <: ObsListElem](e: A): A = e match { // because copy isn't polymorphic
    case e @ ObsElem(o)  => ObsElem(o.copy(
      condition = c orElse o.condition,
      blueprint = b orElse o.blueprint,
      target = t orElse o.target)).asInstanceOf[A]
    case e @ ConditionGroup(c0, b0, t0) => e.copy(c = c orElse c0, b = b orElse b0, t = t orElse t0).asInstanceOf[A]
    case e @ BlueprintGroup(c0, b0, t0) => e.copy(c = c orElse c0, b = b orElse b0, t = t orElse t0).asInstanceOf[A]
    case e @ TargetGroup(c0, b0, t0)   => e.copy(c = c orElse c0, b = b orElse b0, t = t orElse t0).asInstanceOf[A]
  }

}

case class ObsElem(o:Observation) extends ObsListElem

case class ConditionGroup(c: Option[Condition], b: Option[BlueprintBase], t: Option[Target]) extends ObsGroup[Condition] {
  val disIcon = new CompositeIcon(SharedIcons.ICON_CONDS_DIS, SharedIcons.OVL_ERROR)
  val grouping = ObsListGrouping.Condition
  def icon = if (isEmpty) disIcon else SharedIcons.ICON_CONDS
  val text = c.map(_.name)
  val isEmpty = c.isEmpty
}

case class BlueprintGroup(c: Option[Condition], b: Option[BlueprintBase], t: Option[Target]) extends ObsGroup[BlueprintBase] {
  val disIcon = new CompositeIcon(SharedIcons.ICON_DEVICE_DIS, SharedIcons.OVL_ERROR)
  val grouping = ObsListGrouping.Blueprint
  def icon = if (isEmpty) disIcon else SharedIcons.ICON_DEVICE
  val text = b.map(_.name)
  val isEmpty = b.isEmpty
}

import SharedIcons._


object TargetGroup {
  private val errSidereal = new CompositeIcon(ICON_SIDEREAL, OVL_ERROR)
  private val errNonSidereal = new CompositeIcon(ICON_NONSIDEREAL, OVL_ERROR)
}

case class TargetGroup(c: Option[Condition], b: Option[BlueprintBase], t: Option[Target]) extends ObsGroup[Target] {

  import TargetGroup._

  val disIcon = new CompositeIcon(SharedIcons.ICON_SIDEREAL_DIS, SharedIcons.OVL_ERROR)

  val grouping = ObsListGrouping.Target
  val icon = t match {
    case Some(t: SiderealTarget)    if t.isEmpty => errSidereal
    case Some(t: NonSiderealTarget) if t.isEmpty => errNonSidereal
    case Some(t: SiderealTarget)    => ICON_SIDEREAL
    case Some(t: NonSiderealTarget) => ICON_NONSIDEREAL
    case Some(t: TooTarget)         => ICON_TOO
      case _                        => disIcon
    }

  val text = t.map(_.name)
  val isEmpty = t.isEmpty
}


