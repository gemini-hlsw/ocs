package edu.gemini.qv.plugin.util

import java.util.Date

import edu.gemini.horizons.server.backend.HorizonsService2
import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.skycalc.TimeUtils
import edu.gemini.spModel.core
import edu.gemini.spModel.core.{HorizonsDesignation, Site, Peer}
import edu.gemini.util.skycalc.calc.Interval
import edu.gemini.util.skycalc.{Night, Ephemeris, NonSiderealTarget}
import java.util.logging.{Level, Logger}
import jsky.coords.WorldCoords
import scala.collection.concurrent
import scalaz.{\/-, -\/}

/**
 * Cache for non-sidereal targets and their positions according to JPL Horizons.
 * The positions are queried once and then cached indefinitely.
 */
object NonSiderealCache {

  private val LOG = Logger.getLogger(NonSiderealCache.getClass.getName)

  case class NonSiderealKey(horizonsId: HorizonsDesignation)

  private val targetMap: concurrent.Map[NonSiderealKey, NonSiderealTarget] = concurrent.TrieMap()

  def get(site: Site, t: Long, obs: Obs): WorldCoords = {
    require(isHorizonsTarget(obs))

    val night = Night(site, t)
    val nonSid = get(Seq(night), obs)
    nonSid.positionAt(t)
  }

  def get(peer: Peer, nights: Seq[Night], obs: Obs): NonSiderealTarget =
    horizonsNameFor(obs).
      map(id => get(peer, nights, id, new WorldCoords(obs.getRa, obs.getDec))).
      getOrElse(NonSiderealTarget(obs.getCoords))

  /**
   * Checks if the positions of the base target of this observation can be looked up using Horizon.
   * In order we can do a lookup for a non-sidereal target it needs to be defined (i.e. target environment,
   * base position and target are not null) and the target is a NonSiderealTarget.
 *
   * @param obs
   * @return
   */
  def isHorizonsTarget(obs: Obs): Boolean =
    // QV only deals with "valid" observations, i.e. target environment, base and target all have to be set
    obs.getTargetEnvironment.getAsterism.isNonSidereal

  def get(nights: Seq[Night], obs: Obs): NonSiderealTarget = {
    require(isHorizonsTarget(obs))
    horizonsNameFor(obs).
      map(name => {
          val res = targetMap.get(NonSiderealKey(name))
          if (res.isDefined) res.get else NonSiderealTarget(obs.getCoords)
        }).
      getOrElse(NonSiderealTarget(obs.getCoords))
  }

  private def get(peer: Peer, nights: Seq[Night], horizonsId: HorizonsDesignation, default: WorldCoords): NonSiderealTarget = {
    val key = NonSiderealKey(horizonsId)
    val trg = targetMap.get(key)
    if (trg.isDefined) trg.get
    else {
      val ephemerides = executeHorizonsQuery(peer, nights, horizonsId, default)
      val target = NonSiderealTarget(ephemerides)
      targetMap.put(key, target)
      target
    }
  }

  /**
   * Gets the Horizons name from the observation which can be used as the object's ID for position look-ups.
   * Note that this name can be empty if no one has yet done a Horizons lookup for a new non-sidereal target
   * in the OT (magnifying glass).
 *
   * @param obs
   * @return
   */
  def horizonsNameFor(obs: Obs): Option[HorizonsDesignation] =
    obs.getTargetEnvironment.getAsterism.getNonSiderealSpTarget.map(_.getTarget).flatMap {
      case n: core.NonSiderealTarget => n.horizonsDesignation
      case _ =>
        LOG.warning(s"Don't know how to get Horizons name for this target ${obs.getObsId}.")
        None
    }

  /**
   * Executes a horizons query for the a semester and waits for the result.
   * Doing this as a blocking operation is ok here, because this is only executed as part of the constraints
   * calculations which are all done in the background and in parallel, so this will not block the main UI but
   * will wait for the positions to be available to do the constraints calculations that need them.
 *
   * @param peer
   * @param nights
   * @param horizonsName
   * @param default
   * @return
   */
  private def executeHorizonsQuery(peer: Peer, nights: Seq[Night], horizonsName: HorizonsDesignation, default: WorldCoords): Seq[Ephemeris] = {
    // Horizons service only allows fixed intervals, which means we can not get the positions precisely at middle night times for each night
    // of a whole semester and getting a precise position for every single night is too slow. As a compromise we calculate an "average"
    // middle night time for the semester and use that (e.g. if for the first night middle night time is 00:40 and for the 90st night
    // (middle of semester) the middle night time is 00:20 we use 00:30 as the middle night time for all nights of this semester for
    // the Horizons query). This leaves us with positions that are on average a few minutes before or after actual middle night
    // time but for all practical purposes this should not really matter.
    // TODO: adapt this to work with arbitrary number of nights... this is still tailored for single semesters..
    require(nights.size >= 91) // sanity check, semesters always have 180+ nights
    val avgMidNight = nights.head.middleNightTime + (((nights(90).middleNightTime - nights.head.middleNightTime) - TimeUtils.days(90)) / 2)
    val dummyResult = Seq(Ephemeris(System.currentTimeMillis(), default))
    val interval = Interval(avgMidNight, nights.last.end)

    LOG.fine(s"Querying positions for non-sidereal target id=$horizonsName for time=${TimeUtils.print(avgMidNight, peer.site.timezone)}}")
    LOG.fine(s"Mid night times are: head: time=${TimeUtils.print(nights.head.middleNightTime, peer.site.timezone)}}")
    LOG.fine(s"Mid night times are: last: time=${TimeUtils.print(nights.last.middleNightTime, peer.site.timezone)}}")

    def toSkycalcEphemeris(e: core.Ephemeris): Seq[Ephemeris] =
      e.toList.map { case (t, c) =>
        Ephemeris(t, new WorldCoords(c.ra.toDegrees, c.dec.toDegrees))
      }

    // One element per day
    HorizonsService2.lookupEphemeris(horizonsName, peer.site, new Date(interval.start), new Date(interval.end), interval.asDays.toInt)
      .map(toSkycalcEphemeris).run.unsafePerformIO match {

      case -\/(e) =>
          LOG.log(Level.WARNING, s"Horizons lookup failed for id $horizonsName, using default position; error was $e")
          dummyResult

      case \/-(e) =>
          LOG.fine(s"Received ${e.size} positions for id $horizonsName from Horizons service")
          e
    }

  }


}
