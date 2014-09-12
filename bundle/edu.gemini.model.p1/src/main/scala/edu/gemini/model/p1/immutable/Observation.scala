package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import scalaz._
import Scalaz._

object Observation {

  // Some lenses
  val blueprint:Lens[Observation, Option[BlueprintBase]] = Lens.lensu((a, b) => a.copy(blueprint = b), _.blueprint)
  val condition:Lens[Observation, Option[Condition]] = Lens.lensu((a, b) => a.copy(condition = b), _.condition)
  val target:Lens[Observation, Option[Target]] = Lens.lensu((a, b) => a.copy(target = b), _.target)
  val meta:Lens[Observation, Option[ObservationMeta]] = Lens.lensu((a, b) => a.copy(meta = b), _.meta)

  def apply(blueprint:Option[BlueprintBase],
            condition:Option[Condition],
            target:Option[Target],
            band:Band,
            time:Option[TimeAmount]) = new Observation(blueprint, condition, target, time, band)

  def apply(m:M.Observation) = new Observation(m)

  def unapply(o:Observation) = Some((o.blueprint, o.condition, o.target, o.time, o.band))

  val empty = new Observation(None, None, None, None, M.Band.BAND_1_2)

}

class Observation private (val blueprint:Option[BlueprintBase],
                           val condition:Option[Condition],
                           val realTarget:Option[Target],
                           val time:Option[TimeAmount],
                           val band:Band,
                           val meta:Option[ObservationMeta] = None,
                           val enabled:Boolean = true) {

  // When a Proposal is constructed, it sets this variable to point to itself. This is a hack to get around needing to
  // pass a proposal in every time we want to get the target out. The idea is, if you have a disembodied Observation
  // then it just has an Option[Target]. But if it's owned by a Proposal then it defers to the Proposal's target list.
  // I think this is probably very bad, and we should revisit it.
  private[immutable] var proposal:Option[Proposal] = None
  def target:Option[Target] = proposal match { // NB this match is *not* the same as doing proposal.flatMap(...)
    case Some(p) => realTarget.flatMap(_.ref(p))
    case None => realTarget
  }

  def copy(blueprint:Option[BlueprintBase] = blueprint,
           condition:Option[Condition] = condition,
           target:Option[Target] = target,
           time:Option[TimeAmount] = time,
           band:Band = band,
           meta:Option[ObservationMeta] = None, // metadata resets to None on copy, by default
           enabled:Boolean = enabled) =
    new Observation(blueprint, condition, target, time, band, meta, enabled)

  def this(m:M.Observation) = this (
    Option(m.getBlueprint).map(BlueprintBase(_)),
    Option(m.getCondition).map(Condition(_)),
    Option(m.getTarget).map(Target(_)),
    Option(m.getTime).map(TimeAmount(_)),
    Option(m.getBand).getOrElse(M.Band.BAND_1_2),
    Option(m.getMeta).map(ObservationMeta(_)),
    m.isEnabled
  )

  def mutable(n:Namer) = {
    val m = Factory.createObservation
    m.setBlueprint(blueprint.map(_.mutable(n)).orNull)
    m.setCondition(condition.map(_.mutable(n)).orNull)
    m.setTarget(target.map(_.mutable(n)).orNull)
    m.setTime(time.map(_.mutable).orNull)
    m.setBand(band)
    m.setMeta(meta.map(_.mutable).orNull)
    m.setEnabled(enabled)
    m
  }

  def isEmpty = blueprint.isEmpty && condition.isEmpty && target.isEmpty && time.isEmpty

  override def equals(a:Any) = a match {
    case o:Observation => kernel == o.kernel
    case _             => false
  }

  private lazy val kernel = (blueprint, condition, target, time, band, meta)

  override lazy val hashCode = kernel.hashCode()

  def isPartialObservationOf(o:Observation) =
    (time.isEmpty)  && // If I have time defined, I might be incomplete but I'm not partial
    (band == o.band) && // must be the same band
    (blueprint.isEmpty || target.isEmpty || condition.isEmpty || time.isEmpty) && // Must have *some* empty component
    isPartial(blueprint, o.blueprint) &&
    isPartial(target, o.target) &&
    isPartial(condition, o.condition) &&
    isPartial(time, o.time)

  private def isPartial[A](a:Option[A], b:Option[A]) = a.isEmpty || a == b

  override def toString =
    "Observation(%s, %s, %s, %s, %s)".format(blueprint, condition, target, time, band)

}