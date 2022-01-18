package edu.gemini.pit.ui.util.gface

import edu.gemini.ui.gface.GTableViewer
import edu.gemini.ui.gface.GSelectionBroker
import javax.swing.ListSelectionModel
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

trait SingleSelectViewerHelpers[A] {
  self: GTableViewer[_, A, _] =>

  // This is probably unnecessary
  setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

  def selection = Option(getSelection).filter(!_.isEmpty).map(_.first)

  def onDoubleClick(f: A => Unit) {
    getTable.addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent) {
        for {
          z <- selection if e.getClickCount == 2
        } f(z)
      }
    })
  }

  def onClick(f: A => Unit) {
    getTable.addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent) {
        for {
          z <- selection if e.getClickCount == 1
        } f(z)
      }
    })
  }

  def onSelectionChanged(f: Option[A] => Unit) {
    addPropertyChangeListener(GSelectionBroker.PROP_SELECTION, new PropertyChangeListener {
      def propertyChange(e: PropertyChangeEvent) {
        f(selection)
      }
    })
  }

}
