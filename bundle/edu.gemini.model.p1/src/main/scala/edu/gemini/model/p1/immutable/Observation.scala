package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.overheads.Overheads
import edu.gemini.model.p1.{mutable => M}

import scalaz._
import Scalaz._

object Observation {
  type IntOrProgTime = TimeAmount \/ TimeAmount

  // Some lenses.
  val blueprint:Lens[Observation, Option[BlueprintBase]] = Lens.lensu((a, b) => a.copy(blueprint = b), _.blueprint)
  val condition:Lens[Observation, Option[Condition]] = Lens.lensu((a, b) => a.copy(condition = b), _.condition)
  val target:Lens[Observation, Option[Target]] = Lens.lensu((a, b) => a.copy(target = b), _.target)
  val meta:Lens[Observation, Option[ObservationMeta]] = Lens.lensu((a, b) => a.copy(meta = b), _.meta)
  val intTime:Lens[Observation, Option[TimeAmount]] = Lens.lensu((a, b) => a.copy(intTime = b), _.intTime)

  def apply(blueprint:Option[BlueprintBase],
            condition:Option[Condition],
            target:Option[Target],
            band:Band,
            time:Option[IntOrProgTime]) = new Observation(blueprint, condition, target, time, band)

  def apply(m:M.Observation) = new Observation(m)

  def unapply(o:Observation) = Some((o.blueprint, o.condition, o.target, o.intTime, o.calculatedTimes, o.band))

  val empty = new Observation(None, None, None, None, M.Band.BAND_1_2)
}

// REL-2985: It is unfortunate that we have to pass progTime here, but it is necessary for the migration to 2017B,
// since the old "time" user-specified parameter is now progTime, and we have changed to have progTime now calculated
// as a function of parameter intTime: thus, if progTime is defined, we must calculate intTime from it.
class Observation private (val blueprint:Option[BlueprintBase],
                           val condition:Option[Condition],
                           val realTarget:Option[Target],
                           private val timeParam:Option[Observation.IntOrProgTime],
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

  // REL-2985 requires special handling of the times.
  // 2017A and earlier: intTimeParam will be None, and calculatedTimesParam will be defined with progTime.
  // If a blueprint exists - which it should - we can calculate the intTime from the progTime and then the rest
  // of the observation times from the intTime.
  val intTime: Option[TimeAmount] = timeParam.map {
    case -\/(intTimeParam)  => intTimeParam
    case \/-(progTimeParam) => (for {
      b <- blueprint
      c <- Overheads(b)
    } yield c.intTimeFromProgTime(progTimeParam)) | TimeAmount.empty
  }

  // 2017B and later: if the blueprint and the intTime are defined, recalculate the calculatedTimes.
  val calculatedTimes: Option[ObservationTimes] =  for {
    b <- blueprint
    c <- Overheads(b)
    t <- intTime
  } yield c.calculate(t)

  def progTime:  Option[TimeAmount] = calculatedTimes.map(_.progTime)
  def partTime:  Option[TimeAmount] = calculatedTimes.map(_.partTime)
  def totalTime: Option[TimeAmount] = calculatedTimes.map(_.totalTime)

  def copy(blueprint:Option[BlueprintBase] = blueprint,
           condition:Option[Condition] = condition,
           target:Option[Target] = target,
           intTime:Option[TimeAmount] = intTime,
           band:Band = band,
           meta:Option[ObservationMeta] = None, // metadata resets to None on copy, by default
           enabled:Boolean = enabled) =
    new Observation(blueprint, condition, target, intTime.map(_.left), band, meta, enabled)

  def this(m:M.Observation) = this (
      Option(m.getBlueprint).map(BlueprintBase(_)),
      Option(m.getCondition).map(Condition(_)),
      Option(m.getTarget).map(Target(_)),
      Option(m.getIntTime).map(TimeAmount(_).left[TimeAmount]).orElse(Option(m.getProgTime).map(TimeAmount(_).right[TimeAmount])),
      Option(m.getBand).getOrElse(M.Band.BAND_1_2),
      Option(m.getMeta).map(ObservationMeta(_)),
      m.isEnabled
  )

  def mutable(n:Namer) = {
    val m = Factory.createObservation
    m.setBlueprint(blueprint.map(_.mutable(n)).orNull)
    m.setCondition(condition.map(_.mutable(n)).orNull)
    m.setTarget(target.map(_.mutable(n)).orNull)
    m.setIntTime(intTime.map(_.mutable).orNull)
    m.setProgTime(progTime.map(_.mutable).orNull)
    m.setPartTime(partTime.map(_.mutable).orNull)
    m.setTotalTime(totalTime.map(_.mutable).orNull)
    m.setBand(band)
    m.setMeta(meta.map(_.mutable).orNull)
    m.setEnabled(enabled)
    m
  }

  def isEmpty = blueprint.isEmpty && condition.isEmpty && target.isEmpty && intTime.isEmpty

  override def equals(a:Any) = a match {
    case o:Observation => kernel == o.kernel
    case _             => false
  }

  private lazy val kernel = (blueprint, condition, target, intTime, band, meta)

  override lazy val hashCode = kernel.hashCode()

  def isPartialObservationOf(o:Observation) =
    intTime.isEmpty  && // If I have time defined, I might be incomplete but I'm not partial
    (band == o.band) && // must be the same band
    (blueprint.isEmpty || target.isEmpty || condition.isEmpty || intTime.isEmpty) && // Must have *some* empty component
    isPartial(blueprint, o.blueprint) &&
    isPartial(target, o.target) &&
    isPartial(condition, o.condition) &&
    isPartial(intTime, o.intTime)

  private def isPartial[A](a:Option[A], b:Option[A]) = a.isEmpty || a == b

  override def toString =
    "Observation(%s, %s, %s, %s, %s)".format(blueprint, condition, target, intTime, band)

}
