package edu.gemini.shared.gui.monthview

import org.jdesktop.swingx.calendar.DateSelectionModel

sealed trait DateSelectionMode {
  private[monthview] def correspondingType: DateSelectionModel.SelectionMode
}

object DateSelectionMode {
  case object Single extends DateSelectionMode {
    override private[monthview] def correspondingType =
      DateSelectionModel.SelectionMode.SINGLE_SELECTION
  }

  case object Interval extends DateSelectionMode {
    private[monthview] def correspondingType =
      DateSelectionModel.SelectionMode.SINGLE_INTERVAL_SELECTION
  }
}