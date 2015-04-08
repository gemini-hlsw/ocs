package jsky.app.ot.gemini.editor.targetComponent.details

import java.util.Date

import edu.gemini.horizons.api.{HorizonsQuery, HorizonsReply}
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.target.system.{NonSiderealTarget, ConicTarget, NamedTarget, ITarget}
import edu.gemini.spModel.target.system.ITarget.Tag
import jsky.app.ot.OTOptions
import jsky.app.ot.gemini.editor.horizons.HorizonsService

import scalaz._, Scalaz._
import scalaz.concurrent.Task


object Horizons {

  private sealed trait HorizonsActionType
  private case object GET_ORBITAL_ELEMENTS extends HorizonsActionType
  private case object UPDATE_POSITION      extends HorizonsActionType
  private case object PLOT_EPHEMERIS       extends HorizonsActionType

  sealed abstract class HorizonsFailure(val message: String)
  case object EmptyName         extends HorizonsFailure("Name must be non-empty.")
  case object NoOrbitalElements extends HorizonsFailure("Cannot resolve orbital elements for named targets.")
  case object NoService         extends HorizonsFailure("No local Horizons service is available.")
  case object CancelOrError     extends HorizonsFailure("User canceled or there was a error, which was already reported to the user.")
  case object NoResults         extends HorizonsFailure("No results were found.")
  case object NoMinorBody       extends HorizonsFailure("Can't resolve the given ID to any minor body")
  case object Spacecraft        extends HorizonsFailure("Horizons suggests this is a spacecraft. Sorry, but OT can't use spacecrafts")

  type HorizonsIO[A] = EitherT[Task, HorizonsFailure, A]

  def getOrbitalElements(
    pid: Option[SPProgramID],
    target: ConicTarget,
    date: Date
  ): HorizonsIO[(HorizonsReply, String)] =
    query(pid, target, GET_ORBITAL_ELEMENTS, date)

  def getUpdatedPosition(
    pid: Option[SPProgramID],
    target: NonSiderealTarget,
    date: Date
  ): HorizonsIO[(HorizonsReply, String)] =
    query(pid, target, UPDATE_POSITION, date)

  def getEphemeris(
    pid: Option[SPProgramID],
    target: NonSiderealTarget,
    date: Date
  ): HorizonsIO[(HorizonsReply, String)] =
    query(pid, target, PLOT_EPHEMERIS, date)

  private def query(
    pid: Option[SPProgramID],
    target: ITarget,
    operationType: HorizonsActionType,
    date: Date
  ): HorizonsIO[(HorizonsReply, String)] =
    for {
      _ <- validateName(target.getName)
      s <- getService(pid)
      p <- scheduleHorizonsLookup(target.getName, target, s, operationType, date)
    } yield p

  private def validateName(name: String): HorizonsIO[Unit] =
    EitherT(Task(name.isEmpty either EmptyName or ()))

  // get the horizons service, with the site set based on the passed program id, if any
  private def getService(pid: Option[SPProgramID]): HorizonsIO[HorizonsService] =
    EitherT(Task {
      val op = Option(HorizonsService.getInstance)
      for {
        s <- op
        p <- pid.filterNot(OTOptions.isStaff)
      } s.setSite(p)
      op \/> NoService
    })

  private def getCachedResult(
    service: HorizonsService,
    name: String,
    operationType: HorizonsActionType,
    date: Date
  ): Option[(HorizonsReply, String)] =
    Option(service.getLastResult)
      .filter(_.getObjectId == name)
      .filter(_.hasOrbitalElements || operationType != GET_ORBITAL_ELEMENTS)
      .filter(_.hasEphemeris)
      .filter(_.getEphemeris.get(0).getDate == date)
      .strengthR(service.getObjectId)

  private def scheduleHorizonsLookup(
    name: String,
    target: ITarget,
    service: HorizonsService,
    operationType: HorizonsActionType,
    date: Date
  ): HorizonsIO[(HorizonsReply, String)] =
    EitherT(Task {

      // Use the cached result if possible
      getCachedResult(service, name, operationType, date).map(_.right).getOrElse {

        // New query
        service.setInitialDate(date)
        service.setObjectId(name)
        service.setObjectType(target.getTag match {
          case Tag.JPL_MINOR_BODY   => HorizonsQuery.ObjectType.COMET
          case Tag.MPC_MINOR_PLANET => HorizonsQuery.ObjectType.MINOR_BODY
          case Tag.NAMED            => HorizonsQuery.ObjectType.MAJOR_BODY
        })

        // This is a blocking call. If we get null back then it means the user cancelled or there
        // was an error, which will have been reported to the user already (eek).
        Option(service.execute)
          .fold[HorizonsFailure \/ (HorizonsReply, String)](CancelOrError.left) { reply =>

          reply.getReplyType match {

            // Some error conditions
            case null => CancelOrError.left
            case HorizonsReply.ReplyType.NO_RESULTS => NoResults.left
            case HorizonsReply.ReplyType.MAJOR_PLANET if target.getTag != Tag.NAMED => NoMinorBody.left
            case HorizonsReply.ReplyType.SPACECRAFT => Spacecraft.left

            // Usable results!
            case otherwise =>

              // If current target is PLUTO then retroactively change the reply type to MAJOR_PLANET
              // Why do we do this? Who knows!?!!
              target match {
                case nt: NamedTarget if nt.getSolarObject == NamedTarget.SolarObject.PLUTO =>
                  reply.setReplyType(HorizonsReply.ReplyType.MAJOR_PLANET);
                case _ => // do nothing
              }

              // Done
              (reply, service.getObjectId).right

          }
        }
      }
    })

}


