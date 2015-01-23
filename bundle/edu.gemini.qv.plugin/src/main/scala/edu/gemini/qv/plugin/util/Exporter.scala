package edu.gemini.qv.plugin.util

import java.awt.Desktop
import java.awt.print.{PageFormat, PrinterException, PrinterJob}
import java.io.{File, PrintWriter}
import javax.swing.JTable
import javax.swing.JTable.PrintMode
import javax.swing.table.TableColumn

import edu.gemini.qv.plugin.table.ObservationTableModel.{DecValue, RaValue, TimeValue}
import edu.gemini.qv.plugin.table.renderer.EncodedObservationsRenderer.TextPane
import edu.gemini.qv.plugin.ui.QvGui
import edu.gemini.qv.plugin.ui.QvGui.ActionButton

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.{Button, Label, TextArea}

/**
 */
object Exporter {

  def print(table: JTable, hiddenHeaders: Option[Seq[TableColumn]] = None): Button = ActionButton(
    "Print...",
    "Prints this table scaling its size to fit the width of a page.",
    () => doWithHiddenHeaders(table, hiddenHeaders) {
      Exporter.print(table, PageFormat.PORTRAIT)
    }
  )

  def printLandscape(table: JTable, hiddenHeaders: Option[Seq[TableColumn]] = None): Button = ActionButton(
    "Print (Landscape)...",
    "Prints this table using paper landscape orientation, scaling its size to fit the width of a page.",
    () => doWithHiddenHeaders(table, hiddenHeaders) {
      Exporter.print(table, PageFormat.LANDSCAPE)
    }
  )

  def exportXls(table: JTable, hiddenHeaders: Option[Seq[TableColumn]] = None): Button = ActionButton(
    "Open in Spreadsheet...",
    "Opens this table with the application that is configured for xls files.",
    () => doWithHiddenHeaders(table, hiddenHeaders) {
      Exporter.openAsXls(table)
    }
  )

  def exportHtml(table: JTable, hiddenHeaders: Option[Seq[TableColumn]] = None): Button = ActionButton(
    "Open in Browser...",
    "Opens this table with the application that is configured for html files.",
    () => doWithHiddenHeaders(table, hiddenHeaders) {
      Exporter.openAsHtml(table)
    }
  )

  // === Deal with hidden headers that need to be printed..

  /**
   * In case there are hidden header columns which need to be printed we must add them before printing the
   * table and then remove them again. That is a bit cumbersome, but it allows to use the Swing table
   * printing even for the setup where we have the table row headers (Observation ID) in a separate table.
   * @param table
   * @param hiddenHeaders
   * @param fn
   * @tparam T
   * @return
   */
  private def doWithHiddenHeaders[T](table: JTable, hiddenHeaders: Option[Seq[TableColumn]])(fn: => T) {
    hiddenHeaders.foreach(showHeaders(table, _))
    fn
    hiddenHeaders.foreach(hideHeaders(table, _))
  }

  private def showHeaders(table: JTable, hidden: Seq[TableColumn]) =
    hidden.zipWithIndex.foreach { case (c, ix) =>
      table.addColumn(c)
      table.moveColumn(table.getColumnCount - 1, ix)
    }

  private def hideHeaders(table: JTable, hidden: Seq[TableColumn]) =
    hidden.foreach(c => table.removeColumn(c))

  // ===========

  private def print(table: JTable, orientation: Int): Unit = {
    val job = PrinterJob.getPrinterJob
    // on MacOSx the native print dialog allows not to set the paper orientation (??)
    val pageFormat = job.defaultPage()
    pageFormat.setOrientation(orientation)
    job.setPrintable(table.getPrintable(PrintMode.FIT_WIDTH, null, null), pageFormat)
    // use the native print dialog, this allows users to print PDFs which is often useful
    val ok = job.printDialog()
    if (ok) {
      try {
        job.print()
      } catch {
        case e: PrinterException => QvGui.showError("Printing Failed", "Could not print data.", e)
      }
    }
  }


  private def openAsXls(table: JTable) = openAs(table, "xls")

  private def openAsHtml(table: JTable) = openAs(table, "html")

  private def openAs(table: JTable, format: String): Unit = {
    val busy = QvGui.showBusy("Opening Data", "Opening table in external application...")
    val header = headers(table)
    val data = values(table)
    val file = toTable(header, data, format)
    Future {
      Desktop.getDesktop.open(file)
    } andThen {
      case _ => busy.done()
    } onFailure {
      case t: Throwable => QvGui.showError("Open Data Failed", s"Could not open data as $format.", t)
    }
  }

  private def toTable(header: Vector[String], data: Vector[Vector[AnyRef]], format: String): File = {
    val file = File.createTempFile("openAsFile", s".${format}")
    val out = new PrintWriter(file)

//    out.append("<html>\n<head>\n<style type=\"text/css\">\n<!--\nbr {mso-data-placement:same-cell}\n//-->\n</style>\n</head>\n<body>")
    out.append("<html>\n<body>\n")

    out.append("<table>\n")
    out.append("<thead>\n")
    out.append("  <tr>\n")
    for (h <- 0 to header.size - 1) {
      out.append("    <td>")
      out.append(header(h))
      out.append("</td>")
    }
    out.append("  </tr>")
    out.append("</thead>\n")

    out.append("<tbody>")
    out.append("  <tr>\n")
    for (r <- 0 to data.size - 1) {
      out.append("  <tr>\n")
      for (c <- 0 to data(r).size - 1) {
        out.append("    <td>")
        val string = toString(data(r)(c))
        out.append(string.replaceAllLiterally("\n", "<br>")) //"<br style=\"mso-data-placement:same-cell;\">"))
        out.append("</td>\n")
      }
      out.append("  </tr>\n")
    }
    out.append("</tbody>")
    out.append("</table>\n")

    out.append("</body>\n</html>")

    out.close
    file
  }

  private def headers(table: JTable): Vector[String] =
    (for (h <- 0 to table.getColumnCount - 1) yield table.getColumnName(h)).toVector

  private def values(table: JTable): Vector[Vector[AnyRef]] =
    (for (r <- 0 to table.getRowCount - 1) yield colValues(table, r)).toVector

  private def colValues(table: JTable, r: Int): Vector[AnyRef] =
    (for (c <- 0 to table.getColumnCount - 1) yield table.getValueAt(r, c)).toVector


  private def toString(v: AnyRef): String = v match {
    case s: String            => s
    case d: java.lang.Double  => f"$d%.2f"
    case l: Label             => l.text
    case t: TextArea          => t.text
    case t: TextPane          => t.styledDocument.getText(0, t.styledDocument.getLength)
    case r: RaValue           => r.prettyString
    case d: DecValue          => d.prettyString
    case t: TimeValue         => t.prettyString
    case x                    => x.toString
  }


}
