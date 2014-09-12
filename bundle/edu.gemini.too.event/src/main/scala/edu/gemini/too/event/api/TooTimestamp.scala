package edu.gemini.too.event.api

/**
 *
 */
case class TooTimestamp(value: Long) extends Ordered[TooTimestamp] {
  def compare(that: TooTimestamp): Int = value.compareTo(that.value)
  def less(ago: Long): TooTimestamp = TooTimestamp(value - ago)
}

private[event] object TooTimestamp {
  def now: TooTimestamp = TooTimestamp(System.currentTimeMillis())
}
