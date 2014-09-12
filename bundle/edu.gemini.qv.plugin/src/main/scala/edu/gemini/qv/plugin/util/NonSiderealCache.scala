package edu.gemini.qv.plugin.util

import edu.gemini.horizons.api.{HorizonsQuery, HorizonsService}
import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.QvTool
import edu.gemini.skycalc.TimeUtils
import edu.gemini.spModel.core.{Site, Peer}
import edu.gemini.spModel.target.system.IHorizonsTarget
import edu.gemini.util.skycalc.calc.Interval
import edu.gemini.util.skycalc.{Night, Ephemeris, NonSiderealTarget}
import java.util.logging.{Level, Logger}
import jsky.coords.WorldCoords
import scala.collection.concurrent

/**
 * Cache for non-sidereal targets and their positions according to JPL Horizons.
 * The positions are queried once and then cached indefinitely.
 */
object NonSiderealCache {

  private val LOG = Logger.getLogger(NonSiderealCache.getClass.getName)

  case class NonSiderealKey(horizonsId: String)

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
   * base position and target are not null) and the target implements the IHorizonsTarget interface.
   * @param obs
   * @return
   */
  def isHorizonsTarget(obs: Obs): Boolean =
    // QV only deals with "valid" observations, i.e. target environment, base and target all have to be set
    obs.getTargetEnvironment.getBase.getTarget match {
      case t: IHorizonsTarget => true
      case _ => false
    }

  def get(nights: Seq[Night], obs: Obs): NonSiderealTarget = {
    require(isHorizonsTarget(obs))
    horizonsNameFor(obs).
      map(name => {
          val res = targetMap.get(NonSiderealKey(name))
          if (res.isDefined) res.get else NonSiderealTarget(obs.getCoords)
        }).
      getOrElse(NonSiderealTarget(obs.getCoords))
  }

  private def get(peer: Peer, nights: Seq[Night], horizonsId: String, default: WorldCoords): NonSiderealTarget = {
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
   * @param obs
   * @return
   */
  def horizonsNameFor(obs: Obs): Option[String] =
    obs.getTargetEnvironment.getBase.getTarget match {

      case t: edu.gemini.spModel.target.system.IHorizonsTarget =>
        if (t.getName == null || t.getName.isEmpty) None
        else Option(t.getName)

      case _ =>
        LOG.warning(s"Don't know how to get Horizons name for this target ${obs.getObsId}.")
        None
    }


  /**
   * Executes a horizons query for the a semester and waits for the result.
   * Doing this as a blocking operation is ok here, because this is only executed as part of the constraints
   * calculations which are all done in the background and in parallel, so this will not block the main UI but
   * will wait for the positions to be available to do the constraints calculations that need them.
   * @param peer
   * @param nights
   * @param horizonsName
   * @param default
   * @return
   */
  private def executeHorizonsQuery(peer: Peer, nights: Seq[Night], horizonsName: String, default: WorldCoords): Seq[Ephemeris] = {
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

    // execute and wait, this will block; we need the horizons result before we can continue..
    val tryResult = HorizonsService.
      executeAndWait(QvTool.authClient, peer, horizonsName,  interval, 24*60, HorizonsQuery.StepUnits.TIME_MINUTES)

    // in case of an error log a warning
    tryResult.leftMap(t => {
      LOG.log(Level.WARNING, s"Horizons lookup failed for id $horizonsName, using default position", t)
    })

    // in case of an empty result also return dummy result, otherwise return positions returned by Horizons service
    tryResult.map(result => {
      if (result.getEphemeris.isEmpty) {
        LOG.warning(s"Reply from Horizons service was empty for id $horizonsName: ${result.getReplyType}; using default position")
        dummyResult
      } else {
        LOG.fine(s"Received ${result.getEphemeris.size} positions for id $horizonsName from Horizons service")
        val eph = Seq(scala.collection.JavaConversions.asScalaBuffer(result.getEphemeris):_*)
        eph.map(e => Ephemeris(e.getDate.getTime, e.getCoordinates))
      }
    }).getOrElse(dummyResult)

  }


}
