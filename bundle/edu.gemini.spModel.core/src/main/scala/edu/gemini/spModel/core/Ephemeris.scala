package edu.gemini.spModel.core

import scalaz._, Scalaz._

final case class Ephemeris(site: Site, data: Long ==>> Coordinates) {

  /** Perform an exact or interpolated lookup. */
  def iLookup(k: Long): Option[Coordinates] =
    data.iLookup(k)

  /** Construct an exact or interpolated slice. */
  def iSlice(lo: Long, hi: Long): Option[Ephemeris] =
    data.iSlice(lo, hi).map(Ephemeris(site, _))

  /** Construct a table of (Long, Coordinates) values on the given interval. */
  def iTable(lo: Long, hi: Long, step: Long): Option[List[(Long, Coordinates)]] =
    data.iTable(lo, hi, step)

  /** Number of elements in the ephemeris. */
  def size: Int =
    data.size

  /** Ephemeris elements as an association list. */
  def toList: List[(Long, Coordinates)] =
    data.toList

  /** Is the ephemeris empty? */
  def isEmpty: Boolean =
    data.isEmpty

  /** Find the closest matching element, if any. */
  def lookupClosestAssoc(k: Long): Option[(Long, Coordinates)] =
    data.lookupClosestAssoc(k)

  /** Find the closest matching Coordinates, if any. */
  def lookupClosest(k: Long): Option[Coordinates] =
    data.lookupClosest(k)

  /** Find the closest matching time, if any. */
  def lookupClosestKey(k: Long): Option[Long] =
    data.lookupClosestKey(k)

}

object Ephemeris extends EphemerisInstances with EphemerisLenses {

  /** The empty ephemeris, with site arbitrarily chosen to be GN. */
  val empty: Ephemeris =
    apply(Site.GN, ==>>.empty)

  /** A single-point ephemeris. */
  def singleton(site: Site, time: Long, coordinates: Coordinates): Ephemeris =
    apply(site, ==>>.singleton(time, coordinates))

}

trait EphemerisInstances {

  implicit val EqualEphemeris: Equal[Ephemeris] =
    Equal.equalBy(e => (e.site, e.data))

}

trait EphemerisLenses {

  val site: Ephemeris @> Site =
    Lens.lensu((a, b) => a.copy(site = b), _.site)

  val data: Ephemeris @> (Long ==>> Coordinates) =
    Lens.lensu((a, b) => a.copy(data = b), _.data)

  val ephemerisElements: Ephemeris @> List[(Long, Coordinates)] =
    data.xmapB(_.toList)(==>>.fromList(_))

}