package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._
import java.util.UUID

sealed trait Target {

  /**
   * Each Target has a UUID that can be used as a reference. This value is preserved on copy (unless a new one
   * is specified). Targets constructed from the same mutable instance will have the same uuid.
   */
  def uuid:UUID

  def name: String
  def mutable(n:Namer): M.Target
  def coords(time: Long): Option[Coordinates]
  def epoch: CoordinatesEpoch
  def isEmpty: Boolean

  def withUuid(uuid:UUID):Target

  final lazy val ref = TargetRef(this)

}

object Target {

  def apply(m: M.Target): Target = m match {
    case m: M.SiderealTarget    => SiderealTarget(m)
    case m: M.NonSiderealTarget => NonSiderealTarget(m)
    case m: M.TooTarget         => TooTarget(m)
  }

  def empty = SiderealTarget.empty

}

object TooTarget extends UuidCache[M.TooTarget] {
  def empty = apply(UUID.randomUUID(), "Untitled")
  def apply(m: M.TooTarget):TooTarget = TooTarget(uuid(m), m.getName)
}

case class TooTarget private (uuid:UUID, name: String) extends Target {

  def mutable(n:Namer) = {
    val m = Factory.createTooTarget()
    m.setId(n.nameOf(this))
    m.setName(name)
    m
  }

  def isEmpty = false
  def epoch = M.CoordinatesEpoch.J_2000
  def coords(time: Long): Option[Coordinates] = None
  def withUuid(uuid:UUID):Target = copy(uuid = uuid)
}

object SiderealTarget extends UuidCache[M.SiderealTarget] {

  def empty = apply(UUID.randomUUID(), "Untitled", Coordinates.empty, CoordinatesEpoch.J_2000, None, List.empty)

  def apply(m: M.SiderealTarget): SiderealTarget =
    new SiderealTarget(
    uuid(m),
    m.getName,
    Coordinates(Option(m.getDegDeg).getOrElse(m.getHmsDms)),
    m.getEpoch,
    Option(m.getProperMotion).map(ProperMotion(_)),
    ~Option(m.getMagnitudes).map(_.getMagnitude.asScala.map(Magnitude(_)).toList))
}

case class SiderealTarget (
  uuid:UUID,
  name: String,
  coords: Coordinates,
  epoch: CoordinatesEpoch,
  properMotion: Option[ProperMotion],
  magnitudes: List[Magnitude]) extends Target {

  def isEmpty = coords == Coordinates.empty && properMotion.isEmpty && magnitudes.isEmpty

  def mutable(n:Namer) = {
    val m = Factory.createSiderealTarget
    m.setId(n.nameOf(this))
    m.setName(name)
    m.setDegDeg(coords.toDegDeg.mutable)
    m.setEpoch(epoch)
    m.setProperMotion(properMotion.map(_.mutable).orNull)
    m.setMagnitudes {
      val ms = Factory.createMagnitudes()
      ms.getMagnitude.addAll(magnitudes.map(_.mutable).asJavaCollection)
      ms
    }
    m
  }

  def coords(date: Long): Option[Coordinates] = properMotion.map { pm =>
    //    println("WARNING: not interpolating proper motion")
    coords
  }.orElse(Some(coords))

  def withUuid(uuid:UUID):Target = copy(uuid = uuid)

}

object NonSiderealTarget extends UuidCache[M.NonSiderealTarget] {

  def empty = apply(UUID.randomUUID(), "Untitled", List.empty, CoordinatesEpoch.J_2000)

  def apply(m: M.NonSiderealTarget): NonSiderealTarget = new NonSiderealTarget(
    uuid(m),
    m.getName,
    m.getEphemeris.asScala.map(EphemerisElement(_)).toList,
    m.getEpoch)

}

case class NonSiderealTarget(
  uuid:UUID,
  name: String,
  ephemeris: List[EphemerisElement],
  epoch: CoordinatesEpoch) extends Target {

  def isEmpty = ephemeris.isEmpty

  def mutable(n:Namer) = {
    val m = Factory.createNonSiderealTarget
    m.setId(n.nameOf(this))
    m.setName(name)
    m.getEphemeris.addAll(ephemeris.map(_.mutable).asJava)
    m.setEpoch(epoch)
    m
  }

  def coords(date: Long) = for {
    (a, b, f) <- find(date, ephemeris)
    cA = a.coords.toDegDeg
    cB = b.coords.toDegDeg
  } yield DegDeg(f(cA.ra.doubleValue(), cB.ra.doubleValue()), f(cA.dec.doubleValue(), cB.dec.doubleValue()))

  def magnitude(date: Long) = for {
    (a, b, f) <- find(date, ephemeris)
    mA <- a.magnitude
    mB <- b.magnitude
  } yield f(mA, mB)

  private def find(date: Long, es: List[EphemerisElement]): Option[(EphemerisElement, EphemerisElement, (Double, Double) => Double)] = es match {
    case a :: b :: _ if a.validAt <= date && b.validAt >= date =>
      val factor = (date.doubleValue - a.validAt) / (b.validAt - a.validAt) // between 0 and 1
      def interp(a: Double, b: Double) = a + (b - a) * factor
      Some((a, b, interp))
    case _ :: ep => find(date, ep)
    case _       => None
  }

  def withUuid(uuid:UUID):Target = copy(uuid = uuid)

}

