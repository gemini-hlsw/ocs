package edu.gemini.qv.plugin.chart.ui

import edu.gemini.qv.plugin.chart.Axis
import edu.gemini.qv.plugin.filter.core._
import edu.gemini.qv.plugin.filter.ui.CategoriesFilter
import edu.gemini.qv.plugin.filter.ui.FilterElement.FilterElementChanged2
import edu.gemini.qv.plugin.{QvContext, QvStore}
import edu.gemini.qv.plugin.ui.QvGui
import java.awt.Color
import javax.swing.UIManager
import scala.Some
import scala.swing.GridBagPanel.Fill._
import scala.swing.Swing._
import scala.swing._
import scala.swing.event._

object AxisEditor {
  def apply(ctx: QvContext, initial: String, axis: Axis) = {
    new AxisEditor(ctx, initial, new AxisEditorPanel(ctx, axis))
  }
}

/**
 * Editor for axes of charts and categorized tables.
 */
class AxisEditor(ctx: QvContext, initial: String, panel: AxisEditorPanel) extends
  ElementEditor("Axis", initial, QvStore.DefaultAxes.map(_.label).toSet, QvStore.axes.map(_.label).toSet, Some(panel)) {

  def axis = panel.axis

  def delete() {
    QvStore.removeAxis(elementName)
  }

  def save() {
    val newAxis = new Axis(elementName, panel.axis.groups)
    QvStore.addAxis(newAxis)
  }

}

class AxisEditorPanel(ctx: QvContext, var axis: Axis) extends SplitPane(Orientation.Vertical) {
  var groupEditor = new GroupsPanel(0)
  var filterEditor = new CategoriesFilter(ctx, groupEditor.active.filter.elements) {
    border = EmptyBorder(5, 5, 5, 5)
  }

  leftComponent = filterEditor
  rightComponent = groupEditor

  listenTo(groupEditor, filterEditor)

  def replaceGroupEditor() {
    // remove old
    val storedLoc = dividerLocation
    deafTo(groupEditor)
    // create and insert new
    groupEditor = new GroupsPanel(groupEditor.activeIx)
    rightComponent = groupEditor
    listenTo(groupEditor)
    dividerLocation = storedLoc
  }

  def replaceFilterEditor() {
    // remove old
    deafTo(filterEditor)
    // create and insert new
    val storedLoc = dividerLocation
    val storedPanel = filterEditor.selection.index
    filterEditor = new CategoriesFilter(ctx, groupEditor.active.filter.elements) {
      border = EmptyBorder(5, 5, 5, 5)
    }
    filterEditor.selection.index = storedPanel
    leftComponent = filterEditor
    listenTo(filterEditor)
    dividerLocation = storedLoc
  }

  reactions += {
    case GroupSelected(g) => {
      replaceFilterEditor()
    }
    case FilterElementChanged2 => {
      groupEditor.active.filter = filterEditor.filter
    }
  }

  class GroupsPanel(var activeIx: Int) extends GridBagPanel {
    val editors = axis.groups.zipWithIndex.map{case (f, ix) => new GroupEditor(f, ix)}
    def active: GroupEditor = {
      activeIx = Math.min(activeIx, editors.size-1)
      activeIx = Math.max(activeIx, 0)
      editors(activeIx)
    }
    active.select()

    border = EmptyBorder(5, 5, 5, 5)
    editors.foreach(listenTo(_))
    deafTo(this)
    reactions += {
      case GroupSelected(g) =>
        active.deselect()
        activeIx = g.ix
        active.select()
        publish(new GroupSelected(g))
    }

    private var yPos = 0
    editors.foreach { e =>
      layout(e.groupButton)   = new Constraints() {gridx=0; gridy=yPos; weightx=1.0; fill=Horizontal}
      layout(e.deleteButton)  = new Constraints() {gridx=1; gridy=yPos}
      layout(e.addButton)     = new Constraints() {gridx=2; gridy=yPos}
      layout(e.upButton)      = new Constraints() {gridx=3; gridy=yPos}
      layout(e.downButton)    = new Constraints() {gridx=4; gridy=yPos}
      yPos += 1
    }

    layout(Button("Delete All") {deleteAll()}) = new Constraints() {gridx=1; gridy=yPos; gridwidth=4; fill=Horizontal}
    yPos += 1

    // filler
    layout(Swing.VGlue)       = new Constraints() {gridx=0; gridy=yPos; weighty=1.0; gridwidth=5; fill=Vertical}

    def deleteAll() {
      axis = new Axis(axis.label, Seq(new EmptyFilter("<<New>>")))
      replaceGroupEditor()
      replaceFilterEditor()
    }

  }

  case class GroupSelected(group: GroupEditor) extends Event

  class GroupEditor(private var _filter: Filter, val ix: Int) extends Publisher {

    val groupButton = groupButtonF(_filter)

    def select() = groupButton.background = Color.gray
    def deselect() = groupButton.background = UIManager.getColor("Button.background")

    listenTo(groupButton.mouse.clicks)
    reactions += {
      case _: MouseClicked => publish(new GroupSelected(GroupEditor.this))
    }

    def filter = _filter
    def filter_= (f: Filter): Unit = {
      _filter = f
      groupButton.text = f.name
      axis = new Axis(axis.label, axis.groups.updated(ix, f))
    }

    def groupButtonF(filter: Filter) = new Button {
      text = filter.name
    }

    // remove the group from the x or y axis
    def deleteButton = new Button {
      action = new Action("") {
        icon = QvGui.DelIcon
        def apply() = {
          axis = removeFromAxis(axis, ix)
          replaceGroupEditor()
          replaceFilterEditor()
        }
      }
    }
    // insert new empty group
    def addButton = new Button {
      action = new Action("") {
        icon = QvGui.AddIcon
        def apply() = {
          axis = insertIntoAxis(axis, ix+1)
          groupEditor.activeIx = ix+1
          replaceGroupEditor()
          replaceFilterEditor()
          revalidate()
        }
      }
    }
    def upButton = new Button {
      action = new Action("") {
        icon = QvGui.UpIcon
        def apply() = {
          if (ix > 0) {
            axis = swapOnAxis(axis, ix-1, ix)
            groupEditor.activeIx = ix-1
            replaceGroupEditor()
          }
        }
      }
    }
    def downButton = new Button {
      action = new Action("") {
        icon = QvGui.DownIcon
        def apply() = {
          if (ix < groupEditor.editors.size-1) {
            axis = swapOnAxis(axis, ix+1, ix)
            groupEditor.activeIx = ix+1
            replaceGroupEditor()
          }
        }
      }
    }

    def swapOnAxis(a: Axis, i: Int, j: Int) = new Axis(a.label, a.groups.updated(i, a.groups(j)).updated(j, a.groups(i)))
    def removeFromAxis(a: Axis, ix: Int) = {
      val shortenedGroups = a.groups.zipWithIndex.filter(_._2 != ix).unzip._1
      val newGroups = if (shortenedGroups.isEmpty) Seq(new EmptyFilter("<<New>>")) else shortenedGroups
      new Axis(a.label, newGroups)
    }
    def insertIntoAxis(a: Axis, ix: Int) = new Axis(a.label, (a.groups.slice(0, ix) :+ new EmptyFilter("<<New>>")) ++ a.groups.slice(ix, a.groups.length))
  }

}

