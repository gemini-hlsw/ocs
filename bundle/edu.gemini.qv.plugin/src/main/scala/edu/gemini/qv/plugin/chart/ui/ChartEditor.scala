package edu.gemini.qv.plugin.chart.ui

import edu.gemini.qv.plugin.chart.Axis
import edu.gemini.qv.plugin.chart.Chart.Calculation
import edu.gemini.qv.plugin.filter.core.FilterSet
import edu.gemini.qv.plugin.filter.ui.MainFilter
import edu.gemini.qv.plugin.QvStore
import edu.gemini.qv.plugin.QvStore.{BarChart, Histogram, Table}
import edu.gemini.qv.plugin.table.renderer.CellRenderer

class FilterEditor(initial: String, main: MainFilter) extends
  ElementEditor("Filter", initial, QvStore.DefaultFilters.map(_.label).toSet, QvStore.filters.map(_.label).toSet) {

  override def delete() {
    QvStore.removeFilter(elementName)
  }

  override def save() {
    val filter = new FilterSet(elementName, main.filterSet)
    QvStore.addFilter(filter)
  }
}

class ChartEditor(initial: String, xAxis: Axis, yAxis: Axis, func: Calculation) extends
  ElementEditor("Chart", initial, QvStore.DefaultHistograms.map(_.label).toSet, QvStore.histograms.map(_.label).toSet) {

  override def delete() {
    QvStore.removeChart(elementName)
  }

  override def save() {
    val chart = new Histogram(elementName, xAxis, yAxis, func)
    QvStore.addChart(chart)
  }
}

class TableEditor(initial: String, xAxis: Axis, yAxis: Axis, func: CellRenderer) extends
  ElementEditor("Table", initial, QvStore.DefaultTables.map(_.label).toSet, QvStore.tables.map(_.label).toSet) {

  override def delete() {
    QvStore.removeTable(elementName)
  }

  override def save() {
    val table = new Table(elementName, xAxis, yAxis, func)
    QvStore.addTable(table)
  }

}

class BarChartEditor2(initial: String, yAxis: Axis, colorCoding: Axis) extends
ElementEditor("Bar Chart", initial, QvStore.DefaultBarCharts.map(_.label).toSet, QvStore.visCharts.map(_.label).toSet) {

  override def delete() {
    QvStore.removeVisChart(elementName)
  }

  override def save() {
    val chart = new BarChart(elementName, yAxis, colorCoding)
    QvStore.addVisChart(chart)
  }

}