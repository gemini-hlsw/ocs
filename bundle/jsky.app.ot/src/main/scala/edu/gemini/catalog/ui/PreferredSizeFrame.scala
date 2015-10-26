package edu.gemini.catalog.ui

import java.awt.Dimension

import edu.gemini.catalog.ui.QueryResultsFrame._
import edu.gemini.shared.gui.SizePreference

import scala.swing.{Point, Window}
import scala.swing.event.{UIElementMoved, UIElementResized}

/**
 * Mixin with a Scala Swing Window to get an initial size according to the screen
 */
trait PreferredSizeFrame { this: Window =>
  def adjustSize(pack: Boolean) {
    if (pack) {
      this.pack()
    } else {
      // Update size according to the last save position
      size = SizePreference.getDimension(this.getClass).getOrElse {
        // Set initial based on the desktop's size, the code below will work with multiple desktops
        val screenSize = java.awt.GraphicsEnvironment
          .getLocalGraphicsEnvironment
          .getDefaultScreenDevice
          .getDefaultConfiguration.getBounds
        new Dimension(screenSize.getWidth.intValue() * 3 / 4, (2f / 3f * screenSize.getHeight).intValue())
      }
    }

    // Update location according to the last save position
    println(SizePreference.getPosition(this.getClass))
    SizePreference.getPosition(this.getClass).fold(this.centerOnScreen())((d:Point) => location = d)

    // Save position and dimensions
    listenTo(this)
    reactions += {
      case _: UIElementResized =>
        SizePreference.setDimension(getClass, Some(this.size))
      case _: UIElementMoved =>
        SizePreference.setPosition(getClass, Some(this.location))
    }
  }
}
