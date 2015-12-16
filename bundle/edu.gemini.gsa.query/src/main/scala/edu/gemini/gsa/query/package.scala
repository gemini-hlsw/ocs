package edu.gemini.gsa

import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import scalaz.\/

package object query {

  protected [query] val DetailLevelRef = new AtomicReference(Level.INFO)
  protected [query] val JsonLevelRef   = new AtomicReference(Level.FINE)

  def DetailLevel: Level                = DetailLevelRef.get
  def DetailLevel_=(level: Level): Unit = DetailLevelRef.set(level)

  def JsonLevel: Level                = JsonLevelRef.get
  def JsonLevel_=(level: Level): Unit = JsonLevelRef.set(level)

  type GsaResponse[A] = GsaQueryError \/ A

  // Time format used in the GSA JSON. The parser must recognize arbitrary time
  // zone offsets in the input strings returned by the GSA server. The formatter
  // needs an explicit time zone in order to format the output string.  The
  // parser cannot have a time zone or else it ignores the time zone in the
  // input.  So the same DateTimeFormatter instance apparently cannot be used
  // for both parsing and formatting, which seems unfortunate.
  val TimeParse  = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSSSSSxxx")
  val TimeFormat = TimeParse.withZone(ZoneId.of("Z"))

}
