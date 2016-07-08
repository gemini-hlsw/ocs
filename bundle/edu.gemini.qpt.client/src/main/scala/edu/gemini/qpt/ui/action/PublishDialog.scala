package edu.gemini.qpt.ui.action

import edu.gemini.qpt.ui.util.SharedIcons

import javax.swing.{BorderFactory, JOptionPane}

import scala.swing.GridBagPanel.{Fill, Anchor}
import scala.swing.ListView.Renderer
import scala.swing._
import scala.swing.event.SelectionChanged

/** Implements the dialog used when publishing QPT plans. It prompts the user
  * for the publish destination, password (if necessary), and the time zone.
  */
object PublishDialog {

  // The publish destination, or type.
  sealed trait PublishType
  case object PublishPreview            extends PublishType
  case object PublishPreviewWithMarkers extends PublishType
  case object PublishWeb                extends PublishType

  // Time zone to use for the display.
  sealed trait TimeOption
  case object Local extends TimeOption
  case object Utc   extends TimeOption

  case class PublishOptions(publishType: PublishType, password: String, time: TimeOption)

  val defaultOptions = PublishOptions(PublishWeb, "", Utc)


  object panel extends GridBagPanel {

    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

    // Set the panel size explicitly.  Otherwise, when the password field
    // appears the panel can be cropped because the dialog size doesn't
    // adjust.
    preferredSize = new Dimension(345, 105)

    def addRow(text: String, widget: Component, row: Int): Unit =
      addRow(new Label(text), widget, row)

    def addRow(lab: Label, widget: Component, row: Int): Unit = {
      add(lab, new Constraints {
        gridx  = 1
        gridy  = row
        anchor = Anchor.West
        insets = new Insets(3, 0, 0, 5)
      })

      add(widget, new Constraints {
        gridx  = 2
        gridy  = row
        fill   = Fill.Horizontal
        insets = new Insets(3, 0, 0, 0)
      })
    }

    // Left column (0) icon.
    val icon = new Label
    icon.icon = SharedIcons.ICON_PUBLISH

    add(icon, new Constraints {
      gridx      = 0
      gridy      = 0
      gridheight = 4
      anchor     = Anchor.Center
      insets     = new Insets(0, 0, 0, 10)
    })


    // Row 0, cols 1 & 2: instructions.
    add(new Label("Select publish options."), new Constraints {
      gridx      = 1
      gridy      = 0
      gridwidth  = 2
      anchor     = Anchor.West
    })

    def updatePasswordVisibility(pt: PublishType): Unit = {
      passPrompt.visible = pt == PublishWeb
      passField.visible  = pt == PublishWeb
    }


    // Row 1: publish destination
    object publishType extends ComboBox[PublishType](List(PublishPreview, PublishPreviewWithMarkers, PublishWeb)) {
      renderer = Renderer {
        case PublishPreview            => "Preview Locally"
        case PublishPreviewWithMarkers => "Preview Locally w/ QC Markers"
        case PublishWeb                => "Publish to the Web"
      }
    }
    addRow("Destination", publishType, 1)

    listenTo(publishType.selection)
    reactions += {
      case SelectionChanged(`publishType`) =>
        updatePasswordVisibility(publishType.selection.item)
    }


    // Row 2: (invisible except for when publishing to the web) password prompt
    val passPrompt = new Label("Password")
    object passField extends PasswordField
    addRow(passPrompt, passField, 2)


    // Row 3: time zone
    val timeLocal = new ToggleButton("Local Time")
    val timeUtc   = new ToggleButton("UTC")
    object timeGroup extends ButtonGroup(timeLocal, timeUtc)

    object timePanel extends GridPanel(1, 2) {
      contents += timeLocal
      contents += timeUtc
    }
    addRow("Timezone", timePanel, 3)


    /** Extract PublishOptions from the widgets in this panel. */
    def options: PublishOptions = {
      val pub  = publishType.selection.item
      val pass = String.valueOf(passField.password)
      val time = if (timeLocal.selected) Local else Utc
      PublishOptions(pub, pass, time)
    }

    /** Configure the widgets to match the provided options. */
    def options_=(o: PublishOptions) = {
      publishType.selection.item = o.publishType
      passField.text             = o.password
      timeGroup.select(if (options.time == Local) timeLocal else timeUtc)

      updatePasswordVisibility(o.publishType)
    }
  }

  /** Displays the publish options and allows the user to confirm the publish
    * or cancel.
    *
    * @return selected PublishOptions, if any (None if cancelled)
    */
  def prompt(p: java.awt.Component, o: PublishOptions): Option[PublishOptions] = {
    val choices   = Array[AnyRef]("Publish", "Cancel")
    panel.options = o
    val res       = JOptionPane.showOptionDialog(p, panel.peer, "Publish Plan",
                      JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                      null, choices, choices(0))
    if (res == 0) Some(panel.options) else None
  }
}
