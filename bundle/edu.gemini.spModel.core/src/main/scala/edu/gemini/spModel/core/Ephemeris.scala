package edu.gemini.spModel.core

import java.util.TimerTask
import java.util.logging.Logger

import scalaz._, Scalaz._

final class Ephemeris(val site: Site, val compressedData: Deflated[List[(Long, Float, Float)]]) extends Serializable {

  // Internal state for managing the inflated ephemeris data.
  @transient @volatile private var _data: Long ==>> Coordinates = null
  @transient @volatile private var _task: TimerTask = null

  /**
   * A map from time to coordinates. This value is stored in compressed form and is deflated (and
   * cached for a while) on demand. The deflated value is transient, and is thus not serialized.
   */
  def data: Long ==>> Coordinates = synchronized {
    import Ephemeris.{ timer, logger }

    // Decompress if needed
    if (_data == null) {
      logger.info("Inflating compressed ephemeris.")
      _data = ==>>.fromList(compressedData.inflate.map { case (t, r, d) =>
        t -> Coordinates.fromDegrees(r, d).getOrElse(sys.error(s"corrupted ephemeris data: $t $r $d"))
      })
    }

    // Reset the cache timer to a point the future
    if (_task != null) _task.cancel()
    _task = new TimerTask {
      def run(): Unit = {
        logger.info("Discarding inflated ephemeris.")
        Ephemeris.this.synchronized(_data = null)
      }
    }
    timer.schedule(_task, 3000)

    // Done
    _data

  }
    
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

  /** Are there no elements? */
  def isEmpty: Boolean =
    data.isEmpty

  /** Is there at least one element? */
  def nonEmpty: Boolean =
    !isEmpty

  /** Find the closest matching element, if any. */
  def lookupClosestAssoc(k: Long): Option[(Long, Coordinates)] =
    data.lookupClosestAssoc(k)

  /** Find the closest matching Coordinates, if any. */
  def lookupClosest(k: Long): Option[Coordinates] =
    data.lookupClosest(k)

  /** Find the closest matching time, if any. */
  def lookupClosestKey(k: Long): Option[Long] =
    data.lookupClosestKey(k)

  /** Copy. */
  def copy(site: Site = site, data: (Long ==>> Coordinates) = data): Ephemeris =
    Ephemeris.apply(site, data)

  override def equals(a: Any): Boolean =
    a match { 
      case e: Ephemeris => e.site == site && e.compressedData == compressedData
      case _            => false
    }

  override def hashCode: Int =
    site.## ^ compressedData.##

}

object Ephemeris extends EphemerisInstances with EphemerisLenses {

  // A timer to discard inflated ephemerides after a while
  private val timer  = new java.util.Timer
  private val logger = Logger.getLogger(classOf[Ephemeris].getName)

  /** The empty ephemeris, with site arbitrarily chosen to be GN. */
  val empty: Ephemeris =
    apply(Site.GN, ==>>.empty)

  /** A single-point ephemeris. */
  def singleton(site: Site, time: Long, coordinates: Coordinates): Ephemeris =
    apply(site, ==>>.singleton(time, coordinates))

  /** Construct an ephemeris from a time/coordinate map. */
  def apply(site: Site, data: Long ==>> Coordinates): Ephemeris = {
    new Ephemeris(site, Deflated(data.toList.map { case (t, cs) =>
      (t, cs.ra.toDegrees.toFloat, cs.dec.toDegrees.toFloat)
    }))
  }

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

  val compressedData: Ephemeris @> Deflated[List[(Long, Float, Float)]] =
    Lens.lensu((a, b) => new Ephemeris(a.site, b), _.compressedData)

}
