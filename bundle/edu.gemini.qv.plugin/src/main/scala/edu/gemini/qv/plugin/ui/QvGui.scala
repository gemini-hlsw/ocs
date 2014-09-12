package edu.gemini.qv.plugin.ui

import java.awt.Color
import javax.swing.{UIManager, BorderFactory, Icon, ImageIcon}
import scala.Some
import scala.swing.Dialog._
import scala.swing._

/**
 * A collection of silly little UI related tools, definitions and thingies that did not fit anywhere else.
 * Parts of the stuff in here should probably live in places like edu.gemini.shared.gui and could be
 * migrated there at some point.
 */
object QvGui {

  // =====
  // ICONS
  // =====

  private def load(name: String): ImageIcon =
    new ImageIcon(this.getClass.getResource(name))

  val ErrorIcon = load("/resources/images/error_tsk.gif")
  val InfoIcon = load("/resources/images/info_tsk.gif")
  val DownIcon = load("/resources/images/DownArrow16.gif")
  val UpIcon = load("/resources/images/UpArrow16.gif")
  val AddIcon = load("/resources/images/add.gif")
  val DelIcon = load("/resources/images/remove.gif")
  val EditIcon = load("/resources/images/edit.gif")
  val CheckIcon = load("/resources/images/check.png")
  val Spinner16Icon = load("/resources/images/spinner16.gif")
  val CalendarIcon = load("/resources/images/dates.gif")
  val QvIcon = load("/resources/images/qvicon.png")

  // =====


  // =====
  // COLORS
  // =====

  // some QV specific colors; these colors are used in different places in order to keep the number
  // of different colors used in the UI low and somewhat streamlined
  val Text = UIManager.getDefaults.get("Label.foreground").asInstanceOf[java.awt.Color] // get the pretty green for OT labels
  val Green = new Color(51, 160, 44)
  val Blue = new Color(31,120,180)
  val Red = new Color(227,26,28)
  val MoonColor = new Color(225,225,225)

  // Note: The colors below are copies of colors defined in OtColor in jsky.app.ot.util.
  // I did not want to make this plugin depend on the jsky.app.ot.util bundle just for those colors
  // so I repeat them here. These colors should live in edu.gemini.shared.gui in order to be reusable.
  val LightOrange = new Color(255, 225, 172)
  val VeryLightGray = new Color(247, 243, 239)
  val DarkGreen = new Color(51, 102, 51)
  // ====



  // =====
  // ACTION BUTTONS
  // =====

  // Shortcuts to create non-focusable buttons with label, icon, tooltip and a simple action.
  // Depending on the executable function that is passed along, the button itself will be passed into the
  // executable function, this is useful if we have to show a dialog and need the button as a position reference.

  object ActionButton {

    // action buttons with callback without parameters
    def apply(label: String, tip: String, executable: () => Unit) =
      new ActionButton(label, tip, Some(executable), None, None)
    def apply(label: String, tip: String, executable: () => Unit, pic: Icon) =
      new ActionButton(label, tip, Some(executable), None, Some(pic))

    // action buttons with callback that expects the button as parameter
    def apply(label: String, tip: String, executable: (Button) => Unit) =
      new ActionButton(label, tip, None, Some(executable), None)
    def apply(label: String, tip: String, executable: (Button) => Unit, pic: Icon) =
      new ActionButton(label, tip, None, Some(executable), Some(pic))
  }

  sealed class ActionButton(label: String, tip: String, executable: Option[() => Unit], executableWithBtn: Option[(Button) => Unit], pic: Option[Icon] = None)
    extends AbstractActionButton(label, tip, executable, executableWithBtn, pic)

  abstract class AbstractActionButton(label: String, tip: String, executable: Option[() => Unit], executableWithBtn: Option[(Button) => Unit], pic: Option[Icon])
    extends Button() {
      focusable = false
      action = new Action(label) {
        toolTip = tip
        pic.foreach(icon = _)
        def apply(): Unit = {
          executable.foreach(_())                                 // execute function without param (if available)
          executableWithBtn.foreach(_(AbstractActionButton.this)) // execute function with param (if available)
        }
      }
    }


  // =====
  // DIALOGS
  // =====

  def showInfo(title: String, message: String) = showMessage(Message.Info, title, message)
  def showWarning(title: String, message: String) = showMessage(Message.Warning, title, message)
  def showError(title: String, message: String) = showMessage(Message.Error, title, message)
  def showError(title: String, message: String, t: Throwable) = showMessage(Message.Error, title, s"$message\n${t.getMessage}")

  // Dialogs are sometimes brought up to indicate errors caused by background worker tasks,
  // therefore we want to make sure here that they are always executed on the Swing event thread
  def showMessage(messageType: Message.Value, title: String, message: String) =
    Swing.onEDT {
      Dialog.showMessage(
        messageType=messageType,
        title=title,
        message=message)
    }

  /**
   * Shows a simple busy dialog to give some feedback to the users in case they start an operation which does not
   * immediately provide a visual feedback. This helps for example to avoid users double clicking observations wildly
   * if opening an observation in the OT does not happen immediately..
   * Creation of the dialog is done on the EDT just to be sure (in case this is ever used in a future).
   * Use the helper method done() to close this dialog on the EDT.
   * @param title
   * @param message
   * @return
   */
  def showBusy(title: String, message: String): BusyDialog =  {
    val dialog = new BusyDialog(title, message)
    Swing.onEDT {
      dialog.centerOnScreen()
      dialog.open()
    }
    dialog
  }

  /**
   * Simple dialog that shows a message and a spinner icon to provide a visual feedback that something
   * is happening in the background.
   */
  class BusyDialog(label: String, message: String) extends Dialog {
    modal = false
    title = label
    contents = new Label(message) {
      border = BorderFactory.createEmptyBorder(10, 15, 10, 15)
      icon = Spinner16Icon
    }

    /**
     * Done will dispose of the dialog on the EDT.
     * If you use close() or dispose() on the dialog directly, make sure it is done on the EDT.
     */
    def done(): Unit = Swing.onEDT { dispose() }
  }

  // Areas for explanations.
  class Instructions extends TextArea {
    opaque = false
    editable = false
    foreground = Color.DARK_GRAY
  }

}
