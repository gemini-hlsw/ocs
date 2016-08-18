package jsky.app.ot.vcs

import edu.gemini.shared.util.VersionComparison
import edu.gemini.shared.util.VersionComparison.{Conflicting, Newer, Older, Same}
import jsky.app.ot.util.OtColor._

import scala.swing.{Alignment, BorderPanel, Label}
import scala.swing.Swing.EmptyBorder
import java.awt.Color
import javax.swing.Icon

import jsky.util.gui.Resources

object VcsStatusPanel {
  private val alertIcon = Resources.getIcon("eclipse/alert.gif")
  private val blankIcon = Resources.getIcon("eclipse/blank.gif")
  private val errorIcon = Resources.getIcon("eclipse/error.gif")

  val ConflictsMessage     = "The database contains conflicting changes to your program which you must resolve before storing your updates."
  val StatusUnknownMessage = "Cannot contact the Observing Database.  Check your network connection."
  val PendingStoreMessage  = "You have made changes to your program that haven't been sent to the database.  Click 'Sync' in the toolbar to store your updates."
  val PendingFetchMessage  = "There are changes to your program in the database that haven't been applied to your copy.  Click 'Sync' in the toolbar to retrieve the updates."
  val PendingSyncMessage   = "Your version of the program and the version in the database have both been updated.  Click 'Sync' in the toolbar to merge the updates."
  val UnknownPeerMessage   = "The database in which this program should be stored has not been specified.  Click 'Sync' in the toolbar to assign the database to use."
}

import VcsStatusPanel._

class VcsStatusPanel(tracker: VcsStateTracker) extends BorderPanel {
  border = EmptyBorder(3, 3, 3, 3)

  object messagePanel extends BorderPanel {
    border = EmptyBorder(1, 1, 1, 1)

    object statusLabel extends Label {
      foreground = Color.darkGray
      horizontalAlignment = Alignment.Left
    }

    add(statusLabel, BorderPanel.Position.Center)
  }

  add(messagePanel, BorderPanel.Position.Center)

  listenTo(tracker)
  reactions += {
    case evt: VcsStateEvent => handle(evt)
  }

  def handle(evt: VcsStateEvent): Unit = evt match {
    case VcsStateEvent(None, _, _, _)       => show(BG_GREY, blankIcon, " ")
    case VcsStateEvent(_, None, _, _)       => show(BANANA, alertIcon, UnknownPeerMessage)
    case VcsStateEvent(_, _, _, h :: _)     => show(LIGHT_SALMON, errorIcon, ConflictsMessage)
    case VcsStateEvent(_, _, Some(Same), _) => show(BG_GREY, blankIcon, " ")
    case VcsStateEvent(_, _, s, _)          =>
      val (color, icon, text) = status(s)
      show(color, icon, text)
  }

  def status(s: Option[VersionComparison]): (Color, Icon, String) =
    s match {
      case None              => (LIGHT_SALMON, alertIcon, StatusUnknownMessage)
      case Some(Conflicting) => (CANTALOUPE,   alertIcon, PendingSyncMessage)
      case Some(Older)       => (BANANA,       alertIcon, PendingFetchMessage)
      case Some(Newer)       => (BANANA,       alertIcon, PendingStoreMessage)
      case _                 => (LIGHT_SALMON, errorIcon, "An unexpected error has occurred.")
    }

  def show(color: Color, icon: Icon, message: String): Unit = {
    messagePanel.background = color
    messagePanel.statusLabel.icon = icon
    messagePanel.statusLabel.text = message
    visible = true
  }

  handle(tracker.currentState)
}
