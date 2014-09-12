package edu.gemini.pit.ui.editor

import java.awt.event.MouseAdapter
import java.beans.{PropertyChangeListener, PropertyChangeEvent}

import scala.swing.BorderPanel.Position
import scala.swing.{UIElement, TextField, ScrollPane, Label, BorderPanel, Alignment}

import edu.gemini.model.p1.immutable._
import edu.gemini.pit.ui.util.{SharedIcons, StdModalEditor}
import edu.gemini.ui.workspace.util.ElementFactory
import javax.swing
import java.awt
import awt.Color
import scala.swing.event.ValueChanged
import edu.gemini.ui.gface._
import swing.JScrollPane

object KeywordsEditor {
  def selectKeywords(kws: List[Keyword], editable:Boolean, parent: UIElement) = (new KeywordsEditor(kws, editable)).open(parent)
}

class KeywordsEditor(selection: List[Keyword], editable:Boolean) extends StdModalEditor[List[Keyword]]("Select Keywords") {dialog =>

  // Disable ok if read-only
  Contents.Footer.OkButton.enabled = editable

  // Construct our header
  override def header = new BorderPanel {
    peer.setLayout(new awt.BorderLayout(4, 4))
    add(new Label("Select from the list below, or filter on matching text:") {
      horizontalAlignment = Alignment.Left
    }, Position.North)
    add(new TextField {
      reactions += {
        case ValueChanged(_) => Viewer.setFilter(new KeywordFilter(text))
      }
    }, Position.Center)
  }

  // Construct our editor
  def editor = new ScrollPane {
    override lazy val peer = new JScrollPane(Viewer.getList)
  }

  // Construct a new value
  def value = Viewer.getModel

  object Viewer extends GListViewer[List[Keyword], Keyword](new KeywordController, ElementFactory.createList()) {
    getList.setBorder(swing.BorderFactory.createEmptyBorder(0, 3, 0, 0));
    getList.addMouseListener(Listener)
    getList.setVisibleRowCount(20) // essential; scroll bar doesn't work otherwise
    getList.setFocusable(false)
    getList.setSelectionBackground(getList.getBackground) // make selection invisible
    setDecorator(new KeywordDecorator)
    setModel(selection)
    setSelection(new GSelection(selection.toSeq: _*))

    // this forces selected elements to be shown
    def getKeywordAt(p: awt.Point) = getElementAt(p) match {
      case kw: Keyword => Some(kw)
      case _ => None
    }
  }

  object Listener extends MouseAdapter {
    override def mouseClicked(e: awt.event.MouseEvent) {
      for {
        kw <- Viewer.getKeywordAt(e.getPoint)
        m <- Option(Viewer.getModel)
        if editable // otherwise do nothing
      } Viewer.setModel(if (m contains kw) m.filterNot(_ == kw) else kw :: m)
    }
  }

  object Status extends Label {
    Viewer.addPropertyChangeListener(new PropertyChangeListener {
      def propertyChange(e: PropertyChangeEvent) {
        refresh()
      }
    })

    def refresh() {
      text = Option(Viewer.getModel).map(_.size).getOrElse(0) + " selected"
    }

    refresh()
  }

  trait ModelAgnostic[M, E] {
    def modelChanged(viewer: GViewer[M, E], oldModel: M, newModel: M) {}
  }

  class KeywordFilter(pattern: String) extends GFilter[List[Keyword], Keyword] with ModelAgnostic[List[Keyword], Keyword] {
    def accept(kw: Keyword): Boolean = kw.value.toUpperCase.contains(pattern.toUpperCase)
  }

  class KeywordDecorator extends GElementDecorator[List[Keyword], Keyword] {
    var model: Option[List[Keyword]] = None

    def decorate(label: swing.JLabel, k: Keyword) {
      label.setText(k.value)
      model foreach {
        case m if (m.contains(k)) => label.setIcon(SharedIcons.CHECK_SELECTED)
        case m =>                    label.setIcon(SharedIcons.CHECK_UNSELECTED)
      }
      // REL-636
      // For some reason the label text color changes to white when selected on Windows
      // Here we reset it to black
      label.setForeground(Color.black)
    }

    def modelChanged(v: GViewer[List[Keyword], Keyword], old: List[Keyword], m: List[Keyword]) {
      model = Option(m)
    }
  }

  class KeywordController extends GListController[List[Keyword], Keyword] {
    var model: Option[List[Keyword]] = None
    val kws = Keyword.values

    def getElementCount = kws.length

    def getElementAt(i: Int) = kws(i)

    def modelChanged(v: GViewer[List[Keyword], Keyword], old: List[Keyword], m: List[Keyword]) {
      model = Option(m)
    }
  }

}



