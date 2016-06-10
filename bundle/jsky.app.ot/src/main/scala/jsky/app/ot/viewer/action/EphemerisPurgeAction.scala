package jsky.app.ot.viewer.action

import edu.gemini.pot.sp.{ISPProgram, ISPObservation}
import edu.gemini.spModel.rich.pot.sp.obsWrapper
import edu.gemini.spModel.target.EphemerisPurge
import jsky.app.ot.OTOptions
import jsky.app.ot.viewer.SPViewer
import jsky.app.ot.viewer.action.EphemerisPurgeAction._

import java.awt.event.ActionEvent
import javax.swing.{JOptionPane, Action}
import javax.swing.JOptionPane.{INFORMATION_MESSAGE, QUESTION_MESSAGE, YES_NO_CANCEL_OPTION}

import scala.collection.JavaConverters._
import scalaz._, Scalaz._


/** Purge ephemeris data throughout the program. */
class EphemerisPurgeAction(viewer: SPViewer) extends AbstractViewerAction(viewer, "Purge Ephemeris Data ...") {
  val Title               = "Purge Ephemeris Data?"
  val ConfirmationMessage =
  """ Delete extra ephemeris data for all non-sidereal targets in this program that you
    | have permission to edit?  This will decrease the amount of memory required to edit
    | the program by keeping only target coordinates at the scheduling block time.
    |
    | Ephemeris data can always be re-fetched later for each observation in its Target
    | Environment component.
  """.stripMargin

  putValue(AbstractViewerAction.SHORT_NAME, "Purge Ephemeris")
  putValue(Action.SHORT_DESCRIPTION, "Purge ephemeris data in all non-sidereal observations.")
  setEnabled(true)

  override def computeEnabledState: Boolean = true

  override def actionPerformed(e: ActionEvent): Unit = {

    // Ask the user what to purge: only observed, all, or nothing.
    val userChoice: PurgeOption = {
      val staff = OTOptions.isStaffGlobally || Option(getProgram).exists(OTOptions.isStaff)
      val base  = List(All, Cancel)
      val opts  = staff ? (ObservedOnly :: base) | base
      val res   = JOptionPane.showOptionDialog(viewer, ConfirmationMessage, Title, YES_NO_CANCEL_OPTION, QUESTION_MESSAGE, null, opts.toArray, Cancel)
      if ((res < 0) || (res >= opts.length)) Cancel else opts(res)
    }

    if (userChoice != Cancel) {
      val updates = for {
        p  <- Option(getProgram).toList
        f   = userChoice.filter(p)
        o  <- p.getAllObservations.asScala.filter(f).toList
        io <- EphemerisPurge.purge(o)
      } yield io

      val msg = updates.size match {
        case 0 => "No observations were updated."
        case 1 => "One observation was updated."
        case n => s"$n observations updated."
      }

      updates.foreach(_.unsafePerformIO())

      JOptionPane.showMessageDialog(viewer, msg, "Purge Results", INFORMATION_MESSAGE)
    }
  }
}

object EphemerisPurgeAction {
  sealed trait PurgeOption {
    /** Computes the observation filter that corresponds to this purge option. */
    def filter(p: ISPProgram): ISPObservation => Boolean = {
      val staff = OTOptions.isStaffGlobally || OTOptions.isStaff(p)

      // We can only truncate ephemeris elements in observations that we can
      // legally edit. Otherwise, the merge will just reject the change on sync.
      // Staff, however, can edit anything without it being rejected on sync
      // because they are allowed to toggle the observation status to any value
      // anyway.
      def isEditable(o: ISPObservation): Boolean =
        staff || OTOptions.isObservationEditable(o)

      this match {
        case All          => isEditable
        case ObservedOnly => (o: ISPObservation) => isEditable(o) && o.isObserved
        case Cancel       => Function.const(false)
      }
    }

    override def toString: String = this match {
      case ObservedOnly => "Purge Observed Observations"
      case All          => "Purge All Observations"
      case Cancel       => "Cancel"
    }
  }

  case object All extends PurgeOption
  case object ObservedOnly extends PurgeOption
  case object Cancel extends PurgeOption
}
