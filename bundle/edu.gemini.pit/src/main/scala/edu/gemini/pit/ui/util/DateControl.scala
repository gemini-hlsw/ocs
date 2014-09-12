package edu.gemini.pit.ui.util

import scala.swing._

class DateControl(ms: Long) extends Panel {
  override lazy val peer = new CalendarPanel(ms, ms)
  def value = peer.getStartDate
}