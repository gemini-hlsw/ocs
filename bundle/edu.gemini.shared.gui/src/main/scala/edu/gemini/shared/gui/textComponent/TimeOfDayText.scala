package edu.gemini.shared.gui.textComponent

import scala.swing.TextComponent
import java.util.{Calendar, TimeZone}
import java.util.Calendar.{HOUR_OF_DAY, MINUTE, SECOND}

object TimeOfDayText {
  val TimePattern = """^(\d?\d):(\d\d):(\d\d)$""".r

  case class Hms(h: Int, m: Int, s: Int) {
    require(h >= 0 && h < 24)
    require(m >= 0 && m < 60)
    require(s >= 0 && s < 60)

    /**
     * Calculates the total number of milliseconds since midnight represented by
     * the Hms.
     */
    def milliSec: Long = h * 3600000l + m * 60000l + s * 1000l

    override def toString: String = f"$h%02d:$m%02d:$s%02d"
  }

  object Hms {
    val Zero = Hms(0, 0, 0)

    def parse(timeString: String): Option[Hms] = timeString match {
      case TimePattern(hs, ms, ss) =>
        val h = hs.toInt
        val m = ms.toInt
        val s = ss.toInt
        if ((h > 23) || (m > 59) || (s > 59)) None
        else Some(Hms(h, m, s))
      case _                       => None
    }

    /**
     * Gets the Hms associated with the given timestamp in the default time
     * zone.
     */
    def apply(time: Long): Hms = apply(time, TimeZone.getDefault)

    /**
     * Gets the Hms associated with the given timestamp in the given time zone.
     */
    def apply(time: Long, tz: TimeZone): Hms = {
      val cal = Calendar.getInstance(tz)
      cal.setTimeInMillis(time)
      Hms(cal.get(HOUR_OF_DAY), cal.get(MINUTE), cal.get(SECOND))
    }
  }
}

import TimeOfDayText._

trait TimeOfDayText extends NonEmptyText { this: TextComponent =>
  override def valid = Hms.parse(text).isDefined

  def hms: Hms = Hms.parse(text).getOrElse(Hms.Zero)
  def hms_=(h: Hms): Unit = {
    text = h.toString
  }
}
