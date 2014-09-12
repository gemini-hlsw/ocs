package edu.gemini.shared.gui

import java.awt.Dimension
import java.util.prefs.Preferences

/**
 * Class to store sizing preferences for windows / other components.
 * User: sraaphor
 */

object SizePreference {
  // The default value if the preferences are not set.
  private val DEFAULT_WIDTH = -1
  private val DEFAULT_HEIGHT = -1

  /**
   * Pass in the Class.getClass for the component for which we want to find the size preference.
   * Returns null if there is no preference.
   */
  def get(classObj: Class[_]): Option[Dimension] = {
    // The keys for the width and height in the preferences file.
    val widthKey = classObj.getSimpleName + ".width"
    val heightKey = classObj.getSimpleName + ".height"

    // Get the preferences for this class and obtain the width and height.
    val prefs = Preferences.userNodeForPackage(classObj)
    val width =  prefs.getInt(widthKey, DEFAULT_WIDTH)
    val height = prefs.getInt(heightKey, DEFAULT_HEIGHT)

    // If a size preference has been set, return it; otherwise null.
    if (width == DEFAULT_WIDTH || height == DEFAULT_HEIGHT) None
    else Some(new Dimension(width, height))
  }

  /**
   * Pass in the Class.getClass for the component for which we want to set the size preference.
   * If there is no preference, or to delete the existing preference, can pass in null.
   */
  def set(classObj: Class[_], d: Option[Dimension]) {
    // The keys for the width and height in the preferences file.
    val widthKey = classObj.getSimpleName + ".width"
    val heightKey = classObj.getSimpleName + ".height"

    val prefs = Preferences.userNodeForPackage(classObj)
    d.fold {
      prefs.remove(widthKey)
      prefs.remove(heightKey)
    } { dim =>
      prefs.putInt(widthKey, dim.getWidth.toInt)
      prefs.putInt(heightKey, dim.getHeight.toInt)
    }
  }
}
