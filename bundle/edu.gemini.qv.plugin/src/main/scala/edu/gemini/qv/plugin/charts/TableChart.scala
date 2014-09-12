package edu.gemini.qv.plugin.charts

import edu.gemini.qv.plugin.data.CategorizedXYData
import edu.gemini.qv.plugin.filter.core.FilterAnd
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.QvContext.TableChartType
import edu.gemini.qv.plugin.table.renderer.{Cell, CellRenderer}
import edu.gemini.qv.plugin.ui.{SideBarPanel, SideBar}
import edu.gemini.qv.plugin.util.Exporter
import java.awt.Color
import java.awt.event.InputEvent
import javax.swing.table.AbstractTableModel
import scala.swing.GridBagPanel.Fill._
import scala.swing.ScrollPane.BarPolicy
import scala.swing._
import scala.swing.event.MouseClicked
import edu.gemini.qv.plugin.chart.ui.{CategorySubSelected, CategorySelected}

/**
 * Tabular view of categorized data. This is also known as the "Big Sheet".
 */
class TableChart(ctx: QvContext, data: CategorizedXYData, renderer: CellRenderer) extends GridBagPanel {

  private val dataGrid = new CategorizedTableGrid(data, renderer)
  private object scrollPane extends ScrollPane {
    verticalScrollBarPolicy = BarPolicy.AsNeeded
    horizontalScrollBarPolicy = BarPolicy.AsNeeded
    contents = dataGrid
  }

  private val exportPanel = SideBarPanel("Export", Exporter.print(dataGrid.peer), Exporter.printLandscape(dataGrid.peer), Exporter.exportXls(dataGrid.peer), Exporter.exportHtml(dataGrid.peer))
  private object theSideBar extends SideBar(exportPanel)


  layout(scrollPane) = new Constraints {
    gridx = 0
    gridy = 0
    weightx = 1
    weighty = 1
    fill = Both
  }
  layout(theSideBar) = new Constraints {
    gridx = 1
    gridy = 0
    weighty = 1
    fill = Vertical
  }

  // forward events from data grid
  deafTo(this)
  listenTo(dataGrid)
  reactions += {
    case e => publish(e)
  }

  def update(categorizedData: CategorizedXYData, cell: CellRenderer) = dataGrid.update(categorizedData, cell)

  /**
   * Actual table with categorized values.
   */
  class CategorizedTableGrid(var categorizedData: CategorizedXYData, var renderer: CellRenderer) extends Table {

    // set the renderer
    peer.setDefaultRenderer(classOf[Component], renderer)

    // set model to current observations to start with
    update(categorizedData, renderer)

    listenTo(mouse.clicks)
    reactions += {
      case m: MouseClicked => {
        val alt = (m.modifiers & InputEvent.ALT_DOWN_MASK) != 0
        val row = peer.rowAtPoint(m.point)
        val col = peer.columnAtPoint(m.point)
        if (col > 0) {
          // column 0 contains the headers, we are only interested in columns 1 and higher
          val f = new FilterAnd(categorizedData.activeXGroups(col-1), categorizedData.activeYGroups(row))
          if (!alt) publish(CategorySubSelected(f))
          else publish(CategorySelected(f))
        }
      }
    }


    def update(newCategorizedData: CategorizedXYData, renderer: CellRenderer) = {
      categorizedData = newCategorizedData
      model = new CategorizedTableModel(categorizedData, renderer)
      this.renderer = renderer
    }

  }

  /**
   * The table model.
   * Note that we add an artificial column 0 which holds the y-axis category names.
   */
  class CategorizedTableModel(categorizedData: CategorizedXYData, renderer: CellRenderer) extends AbstractTableModel {

    // give renderer a chance to reset itself, alternatively we could create new renderers?
    renderer.reset()

    // for performance reasons all components used to represent the row headers
    // and the cell contents are created once and then re-used by the table model
    // (the renderer will be responsible to change the components to reflect selections etc)
    private val labels: Seq[Label] =
      for {
        y <- Range(0, categorizedData.activeYGroups.size)
      } yield new Label(categorizedData.activeYGroups(y).name, null, Alignment.Center) {
        opaque = true
        foreground = Color.black
        background = Color.lightGray
      }
    private val components: Seq[Cell] =
      for {
        y <- Range(0, categorizedData.activeYGroups.size)
        x <- Range(0, categorizedData.activeXGroups.size)
      } yield renderer.createCell(categorizedData.observations(x, y))

    override val getColumnCount: Int = categorizedData.activeXGroups.size + 1

    override val getRowCount: Int = categorizedData.activeYGroups.size

    override def getColumnName(col: Int): String = col match {
      case 0 => ""
      case _ => categorizedData.activeXGroups(col-1) name
    }

    override def getValueAt(row: Int, col: Int): AnyRef = col match {
      case 0 => labels(row)
      case _ => components(row*categorizedData.activeXGroups.size + col - 1)
    }

  }

}