package jsky.app.ot.gemini.editor.targetComponent.details

import java.util.Date

import edu.gemini.horizons.api.HorizonsQuery.ObjectType
import edu.gemini.horizons.api.{ OrbitalElements, EphemerisEntry, HorizonsReply }
import edu.gemini.spModel.target.system.CoordinateParam.Units
import edu.gemini.spModel.target.system.{ NamedTarget, ConicTarget }
import edu.gemini.spModel.target.system.ITarget.Tag
import jsky.app.ot.gemini.editor.horizons.HorizonsService

import scalaz.{ Tag => _, _ }, Scalaz._
import scalaz.concurrent.Task

/** Pure functional interface to Horizons. */
object Horizons {

  /** The type of failures produced by HorizonsIO programs. */
  sealed abstract class HorizonsFailure(val message: String)
  case class  UnknownError(err: Throwable) extends HorizonsFailure(err.getMessage)
  case object EmptyName         extends HorizonsFailure("Name must be non-empty.")
  case object NoOrbitalElements extends HorizonsFailure("Cannot resolve orbital elements for named targets.")
  case object NoService         extends HorizonsFailure("No local Horizons service is available.")
  case object CancelOrError     extends HorizonsFailure("User canceled or there was a error, which was already reported to the user.")
  case object NoResults         extends HorizonsFailure("No results were found.")
  case object NoMinorBody       extends HorizonsFailure("Can't resolve the given ID to any minor body")
  case object Spacecraft        extends HorizonsFailure("Horizons suggests this is a spacecraft. Sorry, but OT can't use spacecrafts")

  /** The type of programs that perform Horizons lookups. */
  type HorizonsIO[A] = EitherT[Task, HorizonsFailure, A]
  object HorizonsIO {
    def either[A](a: => HorizonsFailure \/ A): HorizonsIO[A] = EitherT(Task.delay(a))
    def delay[A](a: => A): HorizonsIO[A] = either(a.right)
  }

  /** Syntax for HorizonsIO */
  implicit class HorizonsIOOps[A](hio: HorizonsIO[A]) {
    def runAsyncAndReportErrors: Unit =
      hio.run.runAsync { e =>
        e.leftMap[Horizons.HorizonsFailure](Horizons.UnknownError).join match {
          case -\/(CancelOrError) => // do nothing
          case -\/(e)      => println(e) // TODO
          case _ => // done
        }
      }
  }

  /** Alias for underlying Ephemeris representation. */
  type Ephemeris = java.util.List[EphemerisEntry]
  object Ephemeris {
    val empty: Ephemeris = java.util.Collections.emptyList[EphemerisEntry]
  }

  /**
   * The most general form of lookupConicTarget; construct a program to look up and construct a
   * conic target, given the name to assign, the Horizons object id to search for, an object type
   * hint, and a cache directive.
   */
  def lookupConicTargetById(
    name:     String,
    hObjId:   String,
    hObjType: ObjectType,
    date:     Date,
    useCache: Boolean
  ): HorizonsIO[(ConicTarget, Ephemeris)] =
    for {
      _ <- validateName(hObjId)
      s <- getService
      r <- lookup(s, hObjId, hObjType, date, useCache)
      t <- extractConicTarget(r, name)
      e <- extractEphemeris(r)
    } yield (t, e)

  /**
   * Construct a program to look up and construct a new conic target, given a name and an object
   * type hint.
   */
  def lookupConicTargetByName(
    name: String,
    hObjTypeHint: ObjectType,
    date: Date
  ): HorizonsIO[(ConicTarget, Ephemeris)] =
    lookupConicTargetById(name, name, hObjTypeHint, date, false) // use name as id

  /**
   * Construct a program to look up and construct the requested solar object.
   */
  def lookupSolarObject(name: String, obj: NamedTarget.SolarObject, date: Date): HorizonsIO[(NamedTarget, Ephemeris)] =
    for {
      s <- getService
      r <- lookup(s, obj.getHorizonsId, obj.objectType, date, true)
      t <- extractNamedTarget(name, r, obj)
      e <- extractEphemeris(r)
    } yield (t, e)

  /**
   * Constuct a program that extracts a named target from the given reply, with the supplied solar
   * object.
   */
  def extractNamedTarget(name: String, r: HorizonsReply, obj: NamedTarget.SolarObject): Horizons.HorizonsIO[NamedTarget] =
    HorizonsIO.either {

      // Should have the exact ID and obj
      if (r.hasObjectIdAndType &&
          r.getObjectId   == obj.expectId &&
          r.getObjectType == obj.objectType &&
          r.hasEphemeris) {

        val e = r.getEphemeris.get(0)
        val nt = new NamedTarget

        nt.setSolarObject(obj)
        nt.setName(name)
        nt.getRa .setAs(e.getCoordinates.getRaDeg,  Units.DEGREES)
        nt.getDec.setAs(e.getCoordinates.getDecDeg, Units.DEGREES)
        nt.setDateForPosition(e.getDate)
        nt.right

      } else {

        // We did not get back what we asked for
        UnknownError(new Exception("hmm " + r)).left

      }

    }

  /**
   * Constuct a program that extracts a conic target from the given reply, with the supplied name.
   */
  def extractConicTarget(r: HorizonsReply, name: String): Horizons.HorizonsIO[ConicTarget] =
    HorizonsIO.either {

      // Initialize a conic target from the name and reply in scope
      def init(ct: ConicTarget): Unit = {

        // Set the identifying information
        ct.setName(name)
        if (r.hasObjectIdAndType) {
          ct.setHorizonsObjectId(r.getObjectId)
          ct.setHorizonsObjectTypeOrdinal(r.getObjectType.ordinal())
        }

        // Set the orbital elements, if any
        if (r.hasOrbitalElements) {
          import OrbitalElements.Name._
          val es = r.getOrbitalElements
          ct.getAQ         .setValue(es.getValue(A))
          ct.getEpoch      .setValue(es.getValue(EPOCH))
          ct.getEpochOfPeri.setValue(es.getValue(TP))
          ct.getANode      .setValue(es.getValue(OM))
          ct.getPerihelion .setValue(es.getValue(W))
          ct.getInclination.setValue(es.getValue(IN))
          ct.getLM         .setValue(es.getValue(MA))
          ct.setE(es.getValue(EC)) // just a raw double for some reason
        }

        // Set the date/time and coordinates to correspond with the first ephemeris element, if any
        if (r.hasEphemeris) {
          val e = r.getEphemeris.get(0)
          ct.getRa .setAs(e.getCoordinates.getRaDeg,  Units.DEGREES)
          ct.getDec.setAs(e.getCoordinates.getDecDeg, Units.DEGREES)
          ct.setDateForPosition(e.getDate)
        }
      }

      // Construct and initialize a conic target of the appropriate type
      r.getObjectType match {
        case ObjectType.COMET      => (new ConicTarget(Tag.JPL_MINOR_BODY)   <| init).right
        case ObjectType.MINOR_BODY => (new ConicTarget(Tag.MPC_MINOR_PLANET) <| init).right
        case _                     => NoMinorBody.left
      }

    }

  /** Construct a program that extracts an ephemeris (possibly empty) from the given reply. */
  def extractEphemeris(r: HorizonsReply): HorizonsIO[Ephemeris] =
    HorizonsIO.delay(if (r.hasEphemeris) r.getEphemeris else Ephemeris.empty)

  /** Construct a program that ensures the given name is valid (non-empty). */
  def validateName(name: String): HorizonsIO[Unit] =
    HorizonsIO.either(name.isEmpty either EmptyName or (()))

  /** A program that retrieves the Horizons service, if available. */
  val getService: HorizonsIO[HorizonsService] =
    HorizonsIO.either(Option(HorizonsService.getInstance) \/> NoService)

  /** Returns previous results if they match the requested objectId, type, and date. */
  def getCachedResult(
    service: HorizonsService,
    hObjId: String,
    hObjType: ObjectType,
    date: Date
  ): Option[HorizonsReply] =
    Option(service.getLastResult)
      .filter(_.getObjectId == hObjId)
      .filter(_.getObjectType == hObjType)
      .filter(_.hasEphemeris)
      .filter(_.getEphemeris.get(0).getDate == date)

  /**
   * Construct a program to look up a target on the provided service and return the Horizons reply.
   */
  def lookup(
    service: HorizonsService,
    hObjId: String,
    hObjType: ObjectType,
    date: Date,
    useCache: Boolean
  ): HorizonsIO[HorizonsReply] =
    HorizonsIO.either {

      // Use the cached result if possible
      getCachedResult(service, hObjId, hObjType, date).map(_.right).filter(_ => useCache).getOrElse {

        // New query
        service.setInitialDate(date)
        service.setObjectId(hObjId)
        service.setObjectType(hObjType)

        // Here is the blocking call. If we get null back then it means the user cancelled or there
        // was an error, which will have been reported to the user already. This call also
        // internally handles multiple answer disambiguation. This should probably all get up into
        // this code but we'll leave it for now.
        Option(service.execute)
          .fold[HorizonsFailure \/ HorizonsReply](CancelOrError.left) { reply =>
          import HorizonsReply.ReplyType._

          reply.getReplyType match {

            // Some error conditions
            case null            => CancelOrError.left
            case NO_RESULTS      => NoResults    .left
            case SPACECRAFT      => Spacecraft   .left
            case INVALID_QUERY   => ??? // impl error, should never happen
            case MUTLIPLE_ANSWER => ??? // impl error, this should have been handled already

            // Usable results!
            case otherwise => reply.right

          }
        }

      }

    }

}


