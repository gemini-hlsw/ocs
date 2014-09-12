package edu.gemini.pit.ui.util.gface

import edu.gemini.ui.gface.GTableViewer
import edu.gemini.ui.gface.GSelectionBroker
import javax.swing.ListSelectionModel
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import scala.collection.JavaConversions._

trait MultiSelectViewerHelpers[A] {
  self: GTableViewer[_, A, _] =>

  // Multi interval, for now
  setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)

  def selection: List[A] = getSelection.iterator.toList

  def onDoubleClick(f: List[A] => Unit) {
    getTable.addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent) {
        if (e.getClickCount == 2)
          f(selection)
      }
    })
  }

  def onSelectionChanged(f: List[A] => Unit) {
    addPropertyChangeListener(GSelectionBroker.PROP_SELECTION, new PropertyChangeListener {
      def propertyChange(e: PropertyChangeEvent) {
        f(selection)
      }
    })
  }

}
