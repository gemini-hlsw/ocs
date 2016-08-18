package edu.gemini.qv.plugin

import java.io.File
import java.util.logging.Logger
import javax.swing.{JOptionPane, BorderFactory}

import edu.gemini.qv.plugin.chart.Axis
import edu.gemini.qv.plugin.chart.Chart.Calculation
import edu.gemini.qv.plugin.data.DataChanged
import edu.gemini.qv.plugin.filter.core.Filter.{IsActive, IsCompleted}
import edu.gemini.qv.plugin.filter.core._
import edu.gemini.qv.plugin.table.renderer.CellRenderer
import edu.gemini.qv.plugin.ui.QvGui.ActionButton

import scala.swing.ListView.Renderer
import scala.swing._
import scala.swing.event.Event
import scala.util.{Failure, Success, Try}
import scala.xml.{Node, XML}

/**
 * Helper object that deals with making defaults and user elements persistent.
 * When adding or removing user elements the change is made persistent immediately and a change event is
 * published to allow UI elements to reflect the change (e.g. combo boxes offering axes to choose from etc).
 */
object QvStore extends Publisher {

  private val LOG = Logger.getLogger(QvStore.getClass.getName)

  val DefaultFilterName = "All"
  val DefaultHistogramName = "Instruments by RA"
  val DefaultTableName = "Instruments by RA"
  val DefaultBarChartName = "Instruments by Band"

  object AxesChanged extends Event
  object ChartsChanged extends Event
  object TablesChanged extends Event

  /** Some defaults which are always available. */
  val DefaultFilters = Seq(
    FilterSet(DefaultFilterName, Set()),
    FilterSet("Active & Not Completed", Set(IsActive(Some(true)), IsCompleted(Some(false))))
  )

  val DefaultAxes = Seq(
    Axis.RA1, Axis.RA025, Axis.RA05, Axis.RA2,
    Axis.Instruments, Axis.Partners,
    Axis.GmosNDispersers, Axis.GmosNIfuXDispersers, Axis.GmosNMosXDispersers,
    Axis.GmosSDispersers, Axis.GmosSIfuXDispersers, Axis.GmosSMosXDispersers,
    Axis.IQs, Axis.CCs, Axis.WVs, Axis.SBs,
    Axis.Priorities, Axis.Bands, Axis.BigSheet
  )
  val DefaultHistograms: Seq[Histogram] = Seq(
    Histogram(DefaultHistogramName, Axis.RA1, Axis.Instruments, edu.gemini.qv.plugin.chart.Chart.ObservationCount)
  )
  val DefaultTables: Seq[Table] = Seq(
    Table(DefaultTableName, Axis.Instruments, Axis.RA1, CellRenderer.Renderers(0)),
    Table("Big Sheet", Axis.BigSheet, Axis.RA05, CellRenderer.Renderers(7))
  )
  val DefaultBarCharts: Seq[BarChart] = Seq(
    BarChart(DefaultBarChartName, Axis.Instruments, Axis.Bands)
  )

  private var filtersMap: Map[String, FilterSet] = DefaultFilters.map(a => (a.label, a)).toMap
  private var axesMap: Map[String, Axis] = DefaultAxes.map(a => (a.label, a)).toMap
  private var histogramsMap: Map[String, Histogram] = DefaultHistograms.map(c => (c.label, c)).toMap
  private var tablesMap: Map[String, Table] = DefaultTables.map(t => (t.label, t)).toMap
  private var visChartMap: Map[String, BarChart] = DefaultBarCharts.map(c => (c.label, c)).toMap

  /** Adds a filter and notifies listeners. */
  def addFilter(a: FilterSet) { filtersMap += a.label -> a; update(DataChanged) }
  /** Removes a filter and notifies listeners. */
  def removeFilter(label: String) { filtersMap -= label; update(DataChanged) }

  /** Adds an axis and notifies listeners. */
  def addAxis(a: Axis) { axesMap += a.label -> a; update(AxesChanged) }
  /** Removes an axis and notifies listeners. */
  def removeAxis(label: String) { axesMap -= label; update(AxesChanged) }

  /** Adds a chart and notifies listeners. */
  def addChart(c: Histogram) { histogramsMap += c.label -> c; update(ChartsChanged) }
  /** Removes a chart and notifies listeners. */
  def removeChart(label: String) { histogramsMap -= label; update(ChartsChanged) }

  /** Adds a table and notifies listeners. */
  def addTable(t: Table) { tablesMap += t.label -> t; update(TablesChanged) }
  /** Removes a table and notifies listeners. */
  def removeTable(label: String) { tablesMap -= label; update(TablesChanged) }

  /** Adds a table and notifies listeners. */
  def addVisChart(c: BarChart) { visChartMap += c.label -> c; update(ChartsChanged) }
  /** Removes a table and notifies listeners. */
  def removeVisChart(label: String) { visChartMap -= label; update(ChartsChanged) }

  private def update(e: Event) {
    saveDefaults()
    publish(e)
  }

  def filters = filtersMap.values.toSeq.sortBy(_.label)
  def filter(label: String) = filtersMap.getOrElse(label, filtersMap.values.head)
  def filterIndex(label: String) = filters.indexOf(filter(label))
  def defaultFilter = filter(DefaultFilterName)

  def axes = axesMap.values.toSeq.sortBy(_.label)
  def axis(label: String) = axesMap.getOrElse(label, axesMap.values.head)
  def axisIndex(label: String) = axes.indexOf(axis(label))

  def histograms = histogramsMap.values.toSeq.sortBy(_.label)
  def histogram(label: String) = histogramsMap(label)
  def histogramIndex(label: String) = histograms.indexOf(histogram(label))

  def tables = tablesMap.values.toSeq.sortBy(_.label)
  def table(label: String) = tablesMap(label)
  def tableIndex(label: String) = tables.indexOf(table(label))

  def visCharts = visChartMap.values.toSeq.sortBy(_.label)
  def visChart(label: String) = visChartMap(label)
  def visChartIndex(label: String) = visCharts.indexOf(table(label))

  // these values can not change for now:
  val functionsMap: Map[String, Calculation] = edu.gemini.qv.plugin.chart.Chart.Calculations.map(c => (c.label, c)).toMap
  val renderersMap: Map[String, CellRenderer] = CellRenderer.Renderers.map(r => (r.label, r)).toMap
  val functions = functionsMap.values.toSeq.sortBy(_.label)
  val renderers = renderersMap.values.toSeq.sortBy(_.label)

  def loadDefaults() {
    if (QvTool.defaultsFile.forall(_.exists())) {
      // set defaults to whatever was read from disk in case of success or show error dialog otherwise
      QvTool.defaultsFile.foreach(f => loadFromFile(f) match {
        case Success((fMap, aMap, cMap, tMap, vcMap)) =>
          filtersMap = fMap
          axesMap = aMap
          histogramsMap = cMap
          tablesMap = tMap
          visChartMap = vcMap
        case Failure(t) =>
          showError("Error while loading QV defaults: " + t.getMessage);
      })
    }
  }

  def addDefaults(file: File) {
    loadFromFile(file) match {
      case Success((fMap, aMap, cMap, tMap, vcMap)) =>
        filtersMap ++= fMap
        axesMap ++= aMap
        histogramsMap ++= cMap
        tablesMap ++= tMap
        visChartMap ++= vcMap
        // make new stuff locally persistent
        saveDefaults()
      case Failure(t) =>
        showError("Error while importing QV defaults: " + t.getMessage)
    }
  }

  def saveDefaults() {
    try {
      QvTool.defaultsFile.foreach(f => XML.save(f.getAbsolutePath, FilterXMLFormatter.formatAll))
    } catch {
      case t: Throwable =>
        showError("Error while storing QV defaults: " + t.getMessage)
    }
  }

  private def showError(msg: String) {
    LOG.warning(msg)
    JOptionPane.showMessageDialog(null, msg, "QV Defaults Error", JOptionPane.ERROR_MESSAGE)
  }

  // TODO: On import we should make sure that default elements are not overridden.
  def loadFromFile(file: File) = {
    for {
      xml <- Try(XML.load(file.getAbsolutePath))
      savedFilters <- FilterXMLParser.parseFilters(xml \ "filters" \ "filter")
      filtersMap = (DefaultFilters ++ savedFilters).map(a => a.label -> a).toMap
      savedAxes <- FilterXMLParser.parseAxes(xml \ "axes" \ "axis")
      // TODO: remove this; a permanent proper solution is to make sure that no default/dynamic axes are imported
      // TODO: unfortunately this is needed right now because there are xml files in the wild with "Observations" axes
      fixedAxes = savedAxes.filter(_.label != "Observations").filter(_.label != "Programs")
      axesMap = (DefaultAxes ++ fixedAxes).map(a => a.label -> a).toMap.withDefaultValue(Axis.RA1)
      savedCharts <- FilterXMLParser.parseHistograms(xml \ "histograms" \ "histogram", axesMap)
      savedTables <- FilterXMLParser.parseTables(xml \ "tables" \ "table", axesMap)
      barchartAxesMap = axesMap ++ Axis.Dynamics.map(a=>a.label -> a).toMap // include dynamic axes for lookup when loading stored bar charts
      savedVisCharts <- FilterXMLParser.parseVisCharts(xml \ "barcharts" \ "barchart", barchartAxesMap)
      chartsMap = (DefaultHistograms ++ savedCharts).map(c => c.label -> c).toMap
      tablesMap = (DefaultTables ++ savedTables).map(t => t.label -> t).toMap
      visChartMap = (DefaultBarCharts ++ savedVisCharts).map(c => c.label -> c).toMap
    } yield (filtersMap, axesMap, chartsMap, tablesMap, visChartMap)
  }

  def histogramFromXml(n: Node, axes: Map[String, Axis]): Histogram =
    Histogram(n \ "label" text, axes(n \ "xAxis" text), axes(n \ "yAxis" text), QvStore.functionsMap(n \ "function" text))

  def tableFromXml(n: Node, axes: Map[String, Axis]): Table =
    Table(n \ "label" text, axes(n \ "xAxis" text), axes(n \ "yAxis" text), QvStore.renderersMap(n \ "renderer" text))

  def barChartFromXml(n: Node, axes: Map[String, Axis]): BarChart =
    BarChart(n \ "label" text, axes(n \ "yAxis" text), axes(n \ "colorCoding" text))

  trait NamedElement {
    def isEditable: Boolean = true
    def label: String
  }

  case class Histogram(label: String, xAxis: Axis, yAxis: Axis, function: Calculation) extends NamedElement {
    def toXml =
      <histogram>
        <label>{label}</label>
        <xAxis>{xAxis.label}</xAxis>
        <yAxis>{yAxis.label}</yAxis>
        <function>{function.label}</function>
      </histogram>
  }

  case class Table(label: String, xAxis: Axis, yAxis: Axis, function: CellRenderer) extends NamedElement {
    def toXml =
      <table>
        <label>{label}</label>
        <xAxis>{xAxis.label}</xAxis>
        <yAxis>{yAxis.label}</yAxis>
        <renderer>{function.label}</renderer>
      </table>
  }

  case class BarChart(label: String, yAxis: Axis, colorCoding: Axis) extends NamedElement {
    def toXml =
      <barchart>
        <label>{label}</label>
        <yAxis>{yAxis.label}</yAxis>
        <colorCoding>{colorCoding.label}</colorCoding>
      </barchart>
  }


  /// IMPORT / EXPORT


}

class StoreImporter(parent: Component) extends FileChooser {
  if (showOpenDialog(parent) == FileChooser.Result.Approve) {
    QvStore.addDefaults(selectedFile)
    QvStore.publish(QvStore.AxesChanged)
    QvStore.publish(QvStore.ChartsChanged)
    QvStore.publish(QvStore.TablesChanged)
    QvStore.publish(DataChanged)
  }
}

class StoreExporter(parent: Component, elements: Seq[QvStore.NamedElement]) extends Dialog {

  object choices extends ComboBox[QvStore.NamedElement](elements) {
    renderer = Renderer(_.label)
  }

  object fileChooser extends FileChooser() {
    title = "Exporting a QV Element"
  }

  object buttons extends BoxPanel(Orientation.Horizontal) {
    contents += ActionButton("Save to File...", "Saves this element to a file.", () => saveToFile(this))
    contents += ActionButton("Cancel", "Cancel this operation.", () => cancel())
  }

  contents = new GridBagPanel {
    border = BorderFactory.createEmptyBorder(10,10,10,10)

    layout(new Label("Select element to export:")) = new Constraints {
      gridx = 0
      gridy = 0
      anchor = GridBagPanel.Anchor.West
    }
    layout(Swing.VStrut(10)) = (0, 1)
    layout(choices) = new Constraints {
      gridx = 0
      gridy = 2
      weightx = 1
      fill=GridBagPanel.Fill.Horizontal
    }
    layout(Swing.VStrut(10)) = (0, 3)
    layout(buttons) = new Constraints {
      gridx = 0
      gridy = 4
      anchor = GridBagPanel.Anchor.East
    }
  }

  modal = true
  preferredSize = new Dimension(350, 100)
  centerOnScreen()
  open()

  private def cancel() = dispose()

  private def saveToFile(parent: Component) = {
    close()
    fileChooser.selectedFile = new File(choices.selection.item.label+".xml")
    if (fileChooser.showSaveDialog(parent) == FileChooser.Result.Approve) {
      val path = fileChooser.selectedFile.getAbsolutePath
      choices.selection.item match {
        case fs: FilterSet => XML.save(path, FilterXMLFormatter.formatSome(filters = Seq(fs)))
        case a: Axis => XML.save(path, FilterXMLFormatter.formatSome(axes = Seq(a)))
        case c: QvStore.Histogram => XML.save(path, FilterXMLFormatter.formatSome(
          axes = Seq(c.xAxis, c.yAxis),
          histograms = Seq(c)
        ))
        case t: QvStore.Table => XML.save(path, FilterXMLFormatter.formatSome(
          axes = Seq(t.xAxis, t.yAxis),
          tables = Seq(t)
        ))
        case b: QvStore.BarChart => XML.save(path, FilterXMLFormatter.formatSome(
          axes = Seq(b.colorCoding, b.yAxis),
          barCharts = Seq(b)
        ))
      }
    }
    dispose()
  }

}



