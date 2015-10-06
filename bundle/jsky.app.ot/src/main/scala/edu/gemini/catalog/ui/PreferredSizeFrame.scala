package edu.gemini.catalog.ui

import java.awt.Dimension

import edu.gemini.shared.gui.SizePreference

import scala.swing.Window

/**
 * Mixin with a Scala Swing Window to get an initial size according to the screen
 */
trait PreferredSizeFrame { this: Window =>
  def adjustSize() {
    size = SizePreference.getDimension(this.getClass).getOrElse {
      // Set initial based on the desktop's size, the code below will work with multiple desktops
      val screenSize = java.awt.GraphicsEnvironment
        .getLocalGraphicsEnvironment
        .getDefaultScreenDevice
        .getDefaultConfiguration.getBounds
      new Dimension(screenSize.getWidth.intValue() * 3 / 4, (2f / 3f * screenSize.getHeight).intValue())
    }
  }
}
