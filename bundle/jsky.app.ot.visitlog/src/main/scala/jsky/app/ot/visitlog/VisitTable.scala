package jsky.app.ot.visitlog

import edu.gemini.spModel.obsrecord.ObsVisit

import VisitTableModel._

import java.util.{TimeZone, Date}
import java.text.SimpleDateFormat
import javax.swing.{JLabel, JTable}
import javax.swing.table.TableCellRenderer

import scala.swing.{Component, Alignment, Table, Label}
import scala.swing.Alignment._
import scala.swing.Swing._

object VisitTable {
  private class LabelCellRenderer[T](conf: (Label, T) => Unit) extends Table.AbstractRenderer[T, Label](new Label) {
    override def configure(t: Table, sel: Boolean, foc: Boolean, value: T, row: Int, col: Int): Unit = {
      conf(component, value)
    }
  }

  private class DateCellRenderer(val tz: TimeZone) extends Table.AbstractRenderer[Date, Label](new Label) {
    val dateFormatter = new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss")
    dateFormatter.setTimeZone(tz)

    override def configure(t: Table, sel: Boolean, foc: Boolean, value: Date, row: Int, col: Int): Unit = {
      component.text = dateFormatter.format(value)
    }
  }

  private val alignmentRenderer = (Map.empty[Alignment.Value, LabelCellRenderer[String]]/:Alignment.values) { (m, a) =>
    m + (a -> new LabelCellRenderer[String]((lab, s) => {
        lab.horizontalAlignment = a
        lab.text = s
    }))
  }

  val colWidths = Map(
    IdColumn.name  -> 210,
    Datasets.name  -> 100,
    StartTime.name -> 160,
    Duration.name  -> 100
  )
}

import VisitTable._

class VisitTable extends Table {
  private val visitTableModel = new VisitTableModel
  model = visitTableModel
  autoResizeMode = Table.AutoResizeMode.LastColumn
  selection.intervalMode = Table.IntervalMode.Single
  peer.getTableHeader.setReorderingAllowed(false)

  // Awful, but no other clear way to change the table column header ?
  peer.getTableHeader.setDefaultRenderer(new TableCellRenderer() {
    val hr = peer.getTableHeader.getDefaultRenderer
    override def getTableCellRendererComponent(t: JTable, v: Object, sel: Boolean, foc: Boolean, row: Int, col: Int): java.awt.Component = {
      val res = hr.getTableCellRendererComponent(t, v, sel, foc, row, col).asInstanceOf[JLabel]
      if (columns.indexOf(StartTime) == col) {
        res.setText(s"${res.getText} (${dateRenderer.tz.getID})")
      }
      res
    }
  })

  private var dateRenderer = new DateCellRenderer(TimeZone.getTimeZone("UTC"))

  def timeZone: TimeZone = dateRenderer.tz
  def timeZone_=(tz: TimeZone): Unit = {
    this.dateRenderer = new DateCellRenderer(tz)
    peer.getTableHeader.repaint()
    repaint()
  }

  override def rendererComponent(sel: Boolean, foc: Boolean, row: Int, col: Int): Component = {
    val res = (columns(col), model.getValueAt(row, col)) match {
      case (c, s: String) if c == Datasets =>
        alignmentRenderer(Center).componentFor(this, sel, foc, s, row, col)
      case (c, d: Date) =>
        dateRenderer.componentFor(this, sel, foc, d, row, col)
      case (c, s: String) if c == Duration =>
        alignmentRenderer(Right).componentFor(this, sel, foc, s, row, col)
      case _ => super.rendererComponent(sel, foc, row, col)
    }
    res.border = EmptyBorder(5, 5, 5, 5)
    res
  }

  def visits: List[ObsVisit] = visitTableModel.visits
  def visits_=(visitList: List[ObsVisit]): Unit = {
    visitTableModel.visits = visitList

    // Set the column widths to somewhat reasonable values
    VisitTableModel.columns.zipWithIndex.foreach { case (col, index) =>
      val tc = peer.getColumnModel.getColumn(index)
      val w  = colWidths(col.name)
      tc.setMinWidth(w)
      tc.setPreferredWidth(w)
    }
  }
}
