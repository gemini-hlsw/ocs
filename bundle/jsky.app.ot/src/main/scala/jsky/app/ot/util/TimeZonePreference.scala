package jsky.app.ot.util

import java.util.TimeZone
import java.util.prefs.Preferences

/**
 * A shared timezone preference for time-dependent displays that offer a choice
 * of timezone.
 */
object TimeZonePreference {
  val pref = Preferences.userNodeForPackage(TimeZonePreference.getClass)

  def get: TimeZone = TimeZone.getTimeZone(pref.get("TimeZone", "UTC"))

  def set(tz: TimeZone): Unit = pref.put("TimeZone", tz.getID)
}
