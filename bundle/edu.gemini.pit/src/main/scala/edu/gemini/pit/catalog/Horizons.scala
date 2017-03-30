package edu.gemini.pit.catalog

import edu.gemini.model.p1.{ immutable => I }
import edu.gemini.horizons.api._
import edu.gemini.horizons.api.HorizonsReply.ReplyType._
import edu.gemini.horizons.server.backend.CgiQueryExecutor
import scala.actors.Actor._
import scala.collection.JavaConverters._
import java.io.IOException
import java.util.logging.Level
import java.util.{UUID, Date}
import edu.gemini.spModel.core._

object Horizons {

  implicit class pimpLong(val n: Long) extends AnyVal {
    def ms = n
    def secs = ms * 1000
    def mins = secs * 60
    def hours = mins * 60
    def days = hours * 24
  }

  def apply(sem: I.Semester) = new Horizons(Site.GN, new Date(sem.firstDay - 1.days), new Date(sem.lastDay + 1.days))

}

class Horizons private (site: Site, start: Date, end: Date) extends Catalog { cat =>

  def find(id: String)(callback: Result => Unit): Unit = {
    actor {

      val f = callback.safe
      val qe: IQueryExecutor = CgiQueryExecutor.instance

      val hq = new HorizonsQuery(site)
      hq.setStartDate(start)
      hq.setEndDate(end)
      hq.setSteps(60 * 24, HorizonsQuery.StepUnits.TIME_MINUTES) // once a day, for now. N.B. the arcsecond step doesn't seem to work
      hq.setObjectId(id)

      try {
        val hr = qe.execute(hq)

        hr.getReplyType match {

          // Not Found
          case NO_RESULTS    => f(NotFound(id))

          // Error
          case INVALID_QUERY => throw new IllegalArgumentException("Invalid query: " + id)

          // Ambiguous
          case MUTLIPLE_ANSWER =>
            val table = hr.getResultsTable
            val header = table.getHeader.asScala

            val options = for {
              row <- table.getResults.asScala.map(_.asScala.toSeq)
              map = Map(header.zip(row): _*)

              // The first column can be named either ID# or Record #, depending on the object type.
              id <- map.get("ID#").orElse(map.get("Record #"))

              name <- (for {
                epoch <- map.get("Epoch-yr")
                name  <- map.get("Name")
              } yield s"$name epoch:$epoch").orElse(map.get("Name"))
            } yield new Choice(cat, name, id)

            f(if (options.isEmpty) NotFound(id) else Success(Nil, options.toList))

          // Success!
          case _ => decode(id, hr) match {
            case None    => f(NotFound(id))
            case Some(t) => f(Success(List(t), Nil))
          }

        }

      } catch {

        case t: Throwable => try {

          def unwrap(t: Throwable): Nothing = Option(t.getCause).map(unwrap).getOrElse(throw t)
          unwrap(t)

        } catch {

          case e: IOException =>
            Log.warning("%s(%s)".format(e.getClass.getSimpleName, e.getMessage))
            f(Offline)

          case t: Throwable =>
            Log.log(Level.WARNING, "Unexpected trouble looking up %s on Horizons.".format(id), t)
            f(Error(t))
        }
      }

    }
  }

  def decode(name: String, hr: HorizonsReply): Option[I.Target] = if (hr.hasEphemeris) {
    val ephem = hr.getEphemeris.asScala.toList.flatMap { e =>
      val coords = e.getCoordinates
      val mag    = Option(e.getMagnitude).filter(_ > 0)
      val ra     = RightAscension.fromAngle(Angle.fromDegrees(coords.getRaDeg))
      val dec    = Declination.fromAngle(Angle.fromDegrees(coords.getDecDeg))
      dec.map(d => I.EphemerisElement(Coordinates(ra, d), mag, e.getDate.getTime))
    }
    println("Got " + ephem.length + " ephemeris entries.")
    Some(I.NonSiderealTarget(UUID.randomUUID(), name, ephem, I.CoordinatesEpoch.J_2000))
  } else None

}
