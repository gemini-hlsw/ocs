package edu.gemini.dataman

import edu.gemini.dataman.core.{DmanAction, DmanFailure}

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import scalaz.\/

package object query {

  type GsaResponse[A] = GsaQueryError \/ A

  // Time format used in the GSA JSON. The parser must recognize arbitrary time
  // zone offsets in the input strings returned by the GSA server. The formatter
  // needs an explicit time zone in order to format the output string.  The
  // parser cannot have a time zone or else it ignores the time zone in the
  // input.  So the same DateTimeFormatter instance apparently cannot be used
  // for both parsing and formatting, which seems unfortunate.
  val TimeParse  = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSSSSSxxx")
  val TimeFormat = TimeParse.withZone(ZoneId.of("Z"))

  implicit class DmanOps[A](r: => GsaResponse[A]) {
    def liftDman: DmanAction[A] =
      r.leftMap(DmanFailure.QueryFailure).liftDman
  }
}
