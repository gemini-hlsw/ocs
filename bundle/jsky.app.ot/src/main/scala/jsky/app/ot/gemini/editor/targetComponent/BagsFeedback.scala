package jsky.app.ot.gemini.editor.targetComponent

import java.awt.Color
import javax.swing.Icon

import jsky.app.ot.ags.BagsState
import jsky.app.ot.gemini.editor.targetComponent.TargetFeedback.Row
import jsky.app.ot.util.OtColor._
import jsky.app.ot.util.Resources

import scala.swing.GridBagPanel.Fill
import scala.swing.{Alignment, Label}

object BagsFeedback {
  private val spinner = Resources.getIcon("spinner16-transparent.png")

  private class BagsStateRow(bgColor: Color, message: String, icon: Icon) extends Row {

    object feedbackLabel extends Label {
      border = labelBorder
      foreground = Color.DARK_GRAY
      background = bgColor
      text = message
      icon = icon
      opaque = true
      horizontalAlignment = Alignment.Left
    }

    layout(feedbackLabel) = new Constraints {
      weightx = 1.0
      fill = Fill.Both
    }
  }
}
