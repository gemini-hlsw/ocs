package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.mutable.{HmsDmsCoordinates, DegDegCoordinates}
import edu.gemini.model.p1.{ mutable => M }
import edu.gemini.spModel.core.{ProperMotion => _, _}

import scala.collection.JavaConverters._
import scalaz.{Band => SBand, _}
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

  implicit class DegDeg(val c: DegDegCoordinates) extends AnyVal {
    def toCoordinates:NumberFormatException \/ Coordinates = for {
      dec <- Declination.fromAngle(Angle.fromDegrees(c.getDec.doubleValue())) \/> new NumberFormatException()
    } yield Coordinates(RightAscension.fromDegrees(c.getRa.doubleValue()), dec)
  }

  implicit class HmsDms(val c: HmsDmsCoordinates) extends AnyVal {
    def toCoordinates:NumberFormatException \/ Coordinates = for {
      raHms  <- Angle.parseHMS(c.getRa)
      decDms <- Angle.parseDMS(c.getDec)
      dec    <- Declination.fromAngle(decDms) \/> new NumberFormatException()
    } yield Coordinates(RightAscension.fromAngle(raHms), dec)
  }

  def apply(m: M.Target, referenceCoordinates: Option[Coordinates]): Target = m match {
    case m: M.SiderealTarget    => SiderealTarget(m)
    case m: M.NonSiderealTarget => NonSiderealTarget(m, referenceCoordinates)
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
  import Target._

  def empty = apply(UUID.randomUUID(), "Untitled", Coordinates.zero, CoordinatesEpoch.J_2000, None, List.empty)

  def apply(m: M.SiderealTarget): SiderealTarget =
    new SiderealTarget(
    uuid(m),
    m.getName,
    Option(m.getDegDeg).map(_.toCoordinates).getOrElse(m.getHmsDms.toCoordinates).getOrElse(Coordinates.zero),
    m.getEpoch,
    Option(m.getProperMotion).map(ProperMotion(_)),
    ~Option(m.getMagnitudes).map(_.getMagnitude.asScala.map(m => new Magnitude(m.getValue.doubleValue(), m.getBand.toBand)).toList))
}

case class SiderealTarget (
  uuid:UUID,
  name: String,
  coords: Coordinates,
  epoch: CoordinatesEpoch,
  properMotion: Option[ProperMotion],
  magnitudes: List[Magnitude]) extends Target {

  def isEmpty = coords == Coordinates.zero && properMotion.isEmpty && magnitudes.isEmpty

  def mutable(n:Namer) = {
    val m = Factory.createSiderealTarget
    m.setId(n.nameOf(this))
    m.setName(name)
    val degDeg = Factory.createDegDegCoordinates()
    degDeg.setRa(new java.math.BigDecimal(coords.ra.toAngle.toDegrees))
    degDeg.setDec(new java.math.BigDecimal(coords.dec.toDegrees))
    m.setDegDeg(degDeg)
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

  def empty = apply(UUID.randomUUID(), "Untitled", List.empty, CoordinatesEpoch.J_2000, None, None, None)

  /**
   * @param referenceCoordinates optional coordinates that will be used instead of the ephemeris
   *   for ITAC bucket-filling. This value is used only by ITAC and does not exist in Phase 2.
   */
  def apply(m: M.NonSiderealTarget, referenceCoordinates: Option[Coordinates]): NonSiderealTarget = new NonSiderealTarget(
    uuid(m),
    m.getName,
    m.getEphemeris.asScala.map(EphemerisElement(_)).toList,
    m.getEpoch,
    Option(m.getHorizonsDesignation),
    Option(m.getHorizonsQuery()),
    referenceCoordinates
  )

}

  /**
   * @param referenceCoordinates optional coordinates that will be used instead of the ephemeris
   *   for ITAC bucket-filling. This value is used only by ITAC and does not exist in Phase 2.
   */
case class NonSiderealTarget(
  uuid:UUID,
  name: String,
  ephemeris: List[EphemerisElement],
  epoch: CoordinatesEpoch,
  horizonsDesignation: Option[String],
  horizonsQuery: Option[String],
  referenceCoordinates: Option[Coordinates] = None
) extends Target {

  def isEmpty = ephemeris.isEmpty

  def mutable(n:Namer) = {
    val m = Factory.createNonSiderealTarget
    m.setId(n.nameOf(this))
    m.setName(name)
    m.getEphemeris.addAll(ephemeris.map(_.mutable).asJava)
    m.setEpoch(epoch)
    m.setHorizonsDesignation(horizonsDesignation.orNull)
    m.setHorizonsQuery(horizonsQuery.orNull)
    m
  }

  def coords(date: Long): Option[Coordinates] = for {
    (a, b, f) <- find(date, ephemeris)
    cA        = a.coords
    cB        = b.coords
    dec       <- Declination.fromAngle(Angle.fromDegrees(f(cA.dec.toDegrees, cB.dec.toDegrees)))
  } yield Coordinates(RightAscension.fromAngle(Angle.fromDegrees(f(cA.ra.toAngle.toDegrees, cB.ra.toAngle.toDegrees))), dec)

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
