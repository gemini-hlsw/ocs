package edu.gemini.pit.ui.view.obs

import edu.gemini.model.p1.{immutable => I}
import I.{BlueprintBase, Observation}
import scalaz.Lens

/**
 * ObsElems can be grouped various ways. An ObsListGrouping is simply a named function of type
 * ObsElem &rarr; ObsGroup that can be passed to List[ObsElem].groupBy.
 */
sealed abstract class ObsListGrouping[A](name: String) extends (Observation => ObsGroup[A]) {

  def lens:Lens[Observation, Option[A]]
  def get(group:ObsGroup[_]):Option[A]

  override def toString() = name
  def stamp(group: ObsGroup[_], onto: Observation) = lens.set(onto, get(group))
  def clear(onto: Observation) = lens.set(onto, None)
  def matches(group: ObsGroup[_], obs: Observation) = lens.get(obs) == get(group)

}

object ObsListGrouping {

  /** A grouping from ObsElem to ConditionGroup */
  object Condition extends ObsListGrouping[I.Condition]("Conditions") {
    val lens = Observation.condition
    def get(group:ObsGroup[_]) = group.c
    def apply(o:Observation) = ConditionGroup(o.condition, None, None)
  }

  /** A grouping from ObsElem to BlueprintGroup */
  object Blueprint extends ObsListGrouping[BlueprintBase]("Resources") {
    val lens = Observation.blueprint
    def get(group:ObsGroup[_]) = group.b
    def apply(o:Observation) = BlueprintGroup(None, o.blueprint, None)
  }

  /** A grouping from ObsElem to TargetGroup */
  object Target extends ObsListGrouping[I.Target]("Targets") {
    val lens = Observation.target
    def get(group:ObsGroup[_]) = group.t
    def apply(o:Observation) = TargetGroup(None, None,  o.target)
  }

  /** Our default triple of groupings is via conditions, blueprints, the targets. */
  lazy val default:(ObsListGrouping[_], ObsListGrouping[_], ObsListGrouping[_]) = (Condition, Blueprint, Target)

}
