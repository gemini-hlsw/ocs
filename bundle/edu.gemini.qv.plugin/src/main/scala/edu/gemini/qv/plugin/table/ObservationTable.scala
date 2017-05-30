package edu.gemini.qv.plugin.table

import java.awt.event.{AdjustmentEvent, AdjustmentListener}
import java.util.regex.PatternSyntaxException
import javax.swing.table.{DefaultTableCellRenderer, TableColumn, TableRowSorter}
import javax.swing.{BorderFactory, RowFilter, SwingConstants}

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.qv.plugin.data._
import edu.gemini.qv.plugin.filter.core.Filter
import edu.gemini.qv.plugin.filter.ui.PopupMenu
import edu.gemini.qv.plugin.table.ObservationTableModel.{Column, DecValue, RaValue, TimeValue}
import edu.gemini.qv.plugin.ui.{QvGui, SideBar, SideBarPanel}
import edu.gemini.qv.plugin.util.Exporter
import edu.gemini.qv.plugin.{ConstraintsChanged, QvContext, QvTool, ReferenceDateChanged}
import edu.gemini.shared.gui.SortableTable

import scala.None
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.GridBagPanel.Fill._
import scala.swing.ScrollPane.BarPolicy._
import scala.swing._
import scala.swing.event._


class ObservationTable(ctx: QvContext) extends GridBagPanel {

  private val dataModel = new ObservationTableModel(ctx)
  private val headGrid = new HeaderTableGrid
  private val dataGrid = new DataTableGrid
  private val columnSidePanel = new ColumnSidePanel

  // share selection model and sort model between the two tables to keep them in sync
  private val selectionModel = headGrid.peer.getSelectionModel
  dataGrid.peer.setSelectionModel(selectionModel)
  headGrid.peer.setRowSorter(dataModel.rowSorter)
  dataGrid.peer.setRowSorter(dataModel.rowSorter)
  // !IMPORTANT! : Let only one of the tables update the selection when the sort order changes. Otherwise they
  // will cancel each others changes out, which will result in a faulty selection when the sort order changes!
  headGrid.peer.setUpdateSelectionOnSort(false)

  private val exportSidePanel = SideBarPanel(
    "Export",
    Exporter.print(dataGrid.peer, Some(dataGrid.headerColumns)),
    Exporter.printLandscape(dataGrid.peer, Some(dataGrid.headerColumns)),
    Exporter.exportXls(dataGrid.peer, Some(dataGrid.headerColumns)),
    Exporter.exportHtml(dataGrid.peer, Some(dataGrid.headerColumns)))
  private val theSideBar = new SideBar(columnSidePanel, exportSidePanel)

  private val dataDetails = new ObservationTableDetails(ctx, dataGrid)

//  NOTE: In theory it seems the code below is how you get a table with one or more fixed columns on the left side
//  which scroll in sync vertically with the rest of the table. *But* I could not get this to work, the left table
//  always got out of sync after a while. The solution here with having the two tables on two separate scroll panes
//  makes it necessary to sync the two scroll panes manually, but at least I got that to work reliably.
//  --- Why does this not work?
//  private object scrollPane extends ScrollPane(dataGrid)
//  val viewport = new JViewport
//  viewport.setView(headGrid.peer)
//  scrollPane.peer.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, headGrid.peer.getTableHeader)
//  scrollPane.peer.setRowHeaderView(viewport)

  // scroll pane for the column header table on the left; vertical scroll position has to be synced with data table
  private object fixedScrollPane extends ScrollPane(headGrid) with AdjustmentListener {
    minimumSize = new Dimension(150, 0)
    preferredSize = new Dimension (150, 0)
    horizontalScrollBarPolicy = Always
    verticalScrollBar.peer.addAdjustmentListener(this)
    def adjustmentValueChanged(e: AdjustmentEvent): Unit = {
      detailsScrollPane.verticalScrollBar.value = verticalScrollBar.value
    }
  }
  // scroll pane for the actual data table; vertical scroll position has to be synced with column header table
  private object detailsScrollPane extends ScrollPane(dataGrid) with AdjustmentListener {
    verticalScrollBarPolicy = AsNeeded
    horizontalScrollBarPolicy = Always
    verticalScrollBar.peer.addAdjustmentListener(this)
    def adjustmentValueChanged(e: AdjustmentEvent): Unit = {
      fixedScrollPane.verticalScrollBar.value = verticalScrollBar.value
    }
  }

  layout(fixedScrollPane) = new Constraints {
    gridx = 0
    gridy = 0
    weighty = 1
    fill=GridBagPanel.Fill.Vertical
  }
  layout(detailsScrollPane) = new Constraints {
    gridx = 1
    gridy = 0
    weightx = 1
    weighty = 1
    fill=GridBagPanel.Fill.Both
  }
  layout(theSideBar) = new Constraints {
    gridx = 2
    gridy = 0
    weighty = 1
    fill = GridBagPanel.Fill.Vertical
  }
  layout(dataDetails) = new Constraints {
    gridx = 0
    gridy = 1
    gridwidth = 3
    weightx = 1
    fill=GridBagPanel.Fill.Horizontal
  }

  class DataTableGrid extends ObservationTableGrid {
    // store a copy of the all columns of the table's column model, do this before we make any changes to it by
    // removing currently not visible columns; Java enumeration results in a stream: force stream evaluation now!
    // (or we will get whichever columns are left when we call this method later on)

    import scala.collection.JavaConversions._

    val allColumns: Seq[TableColumn] = peer.getColumnModel.getColumns.toStream.force
    val headerColumns = allColumns.take(1)
    val dataColumns = allColumns.drop(1)

    headerColumns.foreach { c => c.setPreferredWidth(150) }

    // in the data table we want to hide all columns which are not meant to be normally visible at startup
    dataModel.columns.foreach { c =>
      if (!c.visibleAtStart) {
        val ix = peer.getColumnModel.getColumnIndex(c.name)
        peer.removeColumn(peer.getColumnModel.getColumn(ix))
      }
    }
  }
  
  class HeaderTableGrid extends ObservationTableGrid {

    // store a copy of the all columns of the table's column model, do this before we make any changes to it by
    // removing currently not visible columns; Java enumeration results in a stream: force stream evaluation now!
    // (or we will get whichever columns are left when we call this method later on)

    import scala.collection.JavaConversions._

    val allColumns: Seq[TableColumn] = peer.getColumnModel.getColumns.toStream.force
    val headerColumns = allColumns.take(1)
    val dataColumns = allColumns.drop(1)

    // in the header table we only want to see the observation ID (column 0)
    while (peer.getColumnCount > 1) {
      peer.removeColumn(peer.getColumnModel.getColumn(peer.getColumnCount-1))
    }
  }

  /**
   * A table that displays a set of observations and implements some interactions with them, like opening
   * them in the OT and other stuff.
   */
  abstract class ObservationTableGrid extends Table with SortableTable {

    model = dataModel

    private val popup = createPopup()

    peer.setDefaultRenderer(classOf[java.lang.Double], DoubleValueRenderer)
    peer.setDefaultRenderer(classOf[RaValue], RaValueRenderer)
    peer.setDefaultRenderer(classOf[DecValue], DecValueRenderer)
    peer.setDefaultRenderer(classOf[TimeValue], TimeValueRenderer)
    peer.setComponentPopupMenu(popup.peer)
    peer.getTableHeader.setComponentPopupMenu(popup.peer)

    listenTo(ctx.mainFilterProvider, ctx.tableFilterProvider, selection, ctx, mouse.clicks)
    reactions += {
      case DataChanged                                 =>
        updateData()
      case FilterChanged(_, _, _)                      =>
        updateData()
      case ConstraintsChanged                          =>
        updateData()
      case ReferenceDateChanged                        =>
        updateData()

      case TableRowsSelected(source, range, adjusting) =>
        if (!peer.getSelectionModel.getValueIsAdjusting) {
          // get selected rows and convert them to the model indices (compensate sorting etc)
          val selIxs = selection.rows.map(viewToModelRow)
          // get from the current data filter the observations with the given indices (get sequence of observations from table model)
          val selObs = model.asInstanceOf[ObservationTableModel].observations.zipWithIndex.filter({case (o, ix) => selIxs.contains(ix)}).unzip._1
          // and create a selection filter that matches those observations
          ctx.selectionFilter = Filter.ObservationSet(selObs.toSet)
        }

      case m: MouseClicked if m.clicks == 2            =>
        val viewRow = peer.rowAtPoint(m.point)
        val modelRow = viewToModelRow(viewRow)
        val obs = model.asInstanceOf[ObservationTableModel].observations(modelRow)
        val obsId = new SPObservationID(obs.getProg.getProgramId, obs.getObsNumber)
        val busy = QvGui.showBusy("Opening Observation", s"Opening observation ${obs.getObsId}...")
        Future {
          QvTool.viewerService.map(_.loadAndView(obsId))
        } andThen {
          case _ => busy.done()
        } onFailure {
          case t => QvGui.showError("Could Not Open Observation", s"Could not open observation ${obs.getObsId} in OT.", t)
        }
    }

    def updateSearchFilter(text: String) {
      try {
        val searchFilter: RowFilter[ObservationTableModel, Object] = RowFilter.regexFilter(text)
        peer.getRowSorter.asInstanceOf[TableRowSorter[ObservationTableModel]].setRowFilter(searchFilter)
      } catch {
        case _: PatternSyntaxException => // ignore
      }
    }

    private def updateData() {

      // update model with new data
      val selected = ctx.selectionFilter                  // save current selection
      dataModel.observations = ctx.filtered               // update observations, this will destroy selection
      dataModel.rowSorter.allRowsChanged()                // resets the rows but keeps all sort keys in sort model
      selectObservations(selected)                        // restore the selection

      // make sure tables get repainted
      headGrid.revalidate()
      dataGrid.revalidate()

      // hack: once we got a first load of data resize columns and turn auto resize off for good
      if (ctx.filtered.nonEmpty && dataGrid.autoResizeMode != Table.AutoResizeMode.Off) {
        dataGrid.autoResizeMode = Table.AutoResizeMode.Off
        resizeColumns()
      }

    }

    private def selectObservations(selected: Option[Filter]) = {
      peer.getSelectionModel.setValueIsAdjusting(true)
      selected.foreach (f => {
        dataModel.observations.zipWithIndex.foreach({ case (o, ix) =>
          if (f.predicate(o, ctx)) {
            val j = modelToViewRow(ix)
            peer.getSelectionModel.addSelectionInterval(j, j)
          }
        })
      })
      peer.getSelectionModel.setValueIsAdjusting(false)
    }

    private def createPopup() = {
      val popup = new PopupMenu
      popup.add(newWindowFromTable())
      popup.add(newWindowFromSelection())
      popup
    }

    private def newWindowFromTable(): MenuItem = new MenuItem(Action("Open new window with observations from table..."){
      val obs = FilterProvider(ctx, ctx.source)
      obs.filter = (ctx.mainFilter, ctx.tableFilter) match {
        case (None, None) => None
        case (Some(f), None) => Some(f)
        case (_, Some(f)) => Some(f)
      }
      // open a new qv tool with its own context (i.e. it can have its own ref date, time range etc)
      QvTool(QvContext(ctx.peer, ctx.dataSource, obs))
    })

    private def newWindowFromSelection(): MenuItem = new MenuItem(Action("Open new window with selected observations..."){
      val obs = FilterProvider(ctx, ctx.source)
      obs.filter = ctx.selectionFilter
      // open a new qv tool with its own context (i.e. it can have its own ref date, time range etc)
      QvTool(QvContext(ctx.peer, ctx.dataSource, obs))
    })


    /** Resizes all columns according to their widest content. */
    def resizeColumns() {
      Range(0, peer.getColumnCount-1).foreach(col => {
        val width = Math.min(preferredColumnWidth(col) + 25, 300)
        peer.getColumnModel.getColumn(col).setPreferredWidth(width)
      })
    }

    /** Finds the maximum preferred width of a column. */
    private def preferredColumnWidth(col: Int) =
      Range(0, peer.getRowCount-1).map(row => {
        val renderer = peer.getCellRenderer(row, col)             // get preferred size of column for all rows
        val comp = peer.prepareRenderer(renderer, row, col)       // (if any, note that rows can be empty)
        comp.getPreferredSize.width
      }).reduceOption(_ max _).getOrElse(0)                       // get maximum preferred size of all rows (on potentially empty collection)


    // === SPECIAL PURPOSE RENDERERS
    // if column is right aligned add some padding on the right side in order to separate content from next column
    private object RaValueRenderer extends DefaultTableCellRenderer() {
      setHorizontalAlignment(SwingConstants.RIGHT)
      override def setValue(value: Object) = value match {
        case v: RaValue =>
          setText(v.prettyString)
          setBorder(BorderFactory.createEmptyBorder(0,0,0,6))
      }
    }
    private object DecValueRenderer extends DefaultTableCellRenderer() {
      setHorizontalAlignment(SwingConstants.RIGHT)
      override def setValue(value: Object) = value match {
        case v: DecValue =>
          setText(v.prettyString)
          setBorder(BorderFactory.createEmptyBorder(0,0,0,6))
      }
    }
    private object DoubleValueRenderer extends DefaultTableCellRenderer() {
      setHorizontalAlignment(SwingConstants.RIGHT)
      override def setValue(value: Object) = value match {
        case v: java.lang.Double =>
          setText(f"$v%.2f")
          setBorder(BorderFactory.createEmptyBorder(0,0,0,5))
      }
    }
    private object TimeValueRenderer extends DefaultTableCellRenderer {
      setHorizontalAlignment(SwingConstants.RIGHT)
      override def setValue(value: Object) = value match {
        case v: TimeValue =>
          setText(v.prettyString)
          setBorder(BorderFactory.createEmptyBorder(0,0,0,5))
      }
    }

  }

  class ObservationTableDetails(ctx: QvContext, dataGrid: ObservationTableGrid) extends GridBagPanel {
    private val status = new Label(statusText)
    private val search = new TextField()
    private val clear = Button("Clear Subselection") {ctx.tableFilter = None}

    layout(clear) = new Constraints{ gridx=0 }
    layout(Swing.HStrut(5)) = new Constraints { gridx=1 }
    layout(new Label("Regexp Search (in all columns): ")) = new Constraints{ gridx=2 }
    layout(search) = new Constraints{ gridx=3; weightx=1.0; fill=GridBagPanel.Fill.Horizontal }
    layout(Swing.HStrut(5)) = new Constraints { gridx=4 }
    layout(status) = new Constraints{ gridx=5 }

    listenTo(ctx, ctx.tableFilterProvider, ctx.selectionFilterProvider, search)
    reactions += {
      case _: ValueChanged => dataGrid.updateSearchFilter(search.text)
      case FilterChanged(_, _, _) => updateStatus()
      case DataChanged => updateStatus()
    }

    private def updateStatus() {
      status.text = statusText
      clear.enabled = ctx.tableFilter.isDefined
      revalidate() // redo layout
    }

    private def statusText =
      s"${ctx.filtered.size} Observations, Selected: ${ctx.selected.size}"
  }

  class ColumnSidePanel extends SideBarPanel("Columns") {


    val latestColIndex = mutable.Map[TableColumn, Int]()

    object resizeBtn extends Button(new Action("Resize All Columns") {
      toolTip = "Adapt column widths to fit size needed by displayed data."
      def apply() = dataGrid.resizeColumns()
    })

    layout(resizeBtn) = new Constraints {
      gridx = 0
      gridy = 0
      weightx = 1
      fill = Horizontal
    }

    dataModel.dataColumns.zipWithIndex.foreach({ case (c, y) =>
      layout(checkbox(dataGrid.dataColumns(y), c, y)) = new Constraints {
        gridx = 0
        gridy = y + 1
        weightx = 1
        fill = Horizontal
      }
    })
    layout(Swing.VGlue) = new Constraints {
      gridx = 0
      gridy = dataModel.dataColumns.size + 1
      weighty = 1
      fill = Vertical
    }

    private def checkbox(column: TableColumn, rawColumn: Column[_], pos: Int) = new CheckBox {
      latestColIndex.put(column, pos)
      selected = rawColumn.visibleAtStart
      action = new Action(column.getHeaderValue.toString) {
        toolTip = rawColumn.tip
        def apply() = if (selected) addColumn(column) else removeColumn(column)
      }
    }

    private def removeColumn(column: TableColumn) = {
      latestColIndex.put(column, viewIndex(column))
      dataGrid.peer.removeColumn(column)
    }

    private def addColumn(column: TableColumn) = {
      val latestIndex = latestColIndex(column) min dataGrid.peer.getColumnCount
      dataGrid.peer.addColumn(column)
      dataGrid.peer.moveColumn(viewIndex(column), latestIndex)
    }

    private def viewIndex(column: TableColumn) =
      dataGrid.peer.convertColumnIndexToView(column.getModelIndex)

  }
}
