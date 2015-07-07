package edu.gemini.shared.gui

import java.awt.{Point, Dimension}
import java.util.prefs.Preferences

/**
 * Class to store sizing preferences for windows / other components.
 * User: sraaphor
 */

object SizePreference {
  // The default value if the preferences are not set.
  private val DEFAULT_WIDTH = -1
  private val DEFAULT_HEIGHT = -1
  private val DEFAULT_POSX = -1
  private val DEFAULT_POSY = -1

  private def getPair(classObj: Class[_], keys: (String, String), defaults: (Int, Int)): Option[(Int, Int)] = {
    // The keys for the values in the preferences file.
    val xKey = classObj.getTypeName + keys._1
    val yKey = classObj.getTypeName + keys._2

    // Get the preferences for this class and obtain the width and height.
    val prefs = Preferences.userNodeForPackage(classObj)
    // If a preference has been set, return it; otherwise None
    Option((prefs.getInt(xKey, defaults._1), prefs.getInt(yKey, defaults._2))).filter(p => p._1 != defaults._1 && p._2 != defaults._2)
  }

  private def setPair(classObj: Class[_], keys: (String, String), values: Option[(Int, Int)]) {
    // The keys for the values in the preferences file.
    val xKey = classObj.getTypeName + keys._1
    val yKey = classObj.getTypeName + keys._2

    // Store the preferences for this class
    val prefs = Preferences.userNodeForPackage(classObj)
    values.fold {
      prefs.remove(xKey)
      prefs.remove(yKey)
    } { dim =>
      prefs.putInt(xKey, dim._1)
      prefs.putInt(yKey, dim._2)
    }
  }

  /**
   * Pass in the Class.getClass for the component for which we want to find the size preference.
   * Returns None if there is no preference.
   */
  def getDimension(classObj: Class[_]): Option[Dimension] =
    getPair(classObj, (".width", ".height"), (DEFAULT_WIDTH, DEFAULT_HEIGHT)).map(d => new Dimension(d._1, d._2))

  /**
   * Pass in the Class.getClass for the component for which we want to find the position preference.
   * Returns None if there is no preference.
   */
  def getPosition(classObj: Class[_]): Option[Point] =
    getPair(classObj, (".posX", ".posY"), (DEFAULT_POSX, DEFAULT_POSY)).map(p => new Point(p._1, p._2))

  /**
   * Pass in the Class.getClass for the component for which we want to set the size preference.
   * If there is no preference, or to delete the existing preference, can pass in None.
   */
  def setDimension(classObj: Class[_], d: Option[Dimension]) {
    setPair(classObj, (".width", ".height"), d.map(i => (i.width, i.height)))
  }

  /**
   * Pass in the Class.getClass for the component for which we want to set the size preference.
   * If there is no preference, or to delete the existing preference, can pass in null.
   */
  def setPosition(classObj: Class[_], d: Option[Point]) {
    setPair(classObj, (".posX", ".posY"), d.map(i => (i.x, i.y)))
  }
}
