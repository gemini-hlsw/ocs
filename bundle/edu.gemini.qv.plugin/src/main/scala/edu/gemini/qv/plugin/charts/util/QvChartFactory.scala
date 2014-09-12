package edu.gemini.qv.plugin.charts.util

import java.awt.Color
import org.jfree.chart.axis.AxisLocation
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.renderer.category.{StandardBarPainter, BarRenderer}
import org.jfree.chart.{JFreeChart, ChartFactory}
import org.jfree.data.category.CategoryDataset
import org.jfree.data.gantt.TaskSeriesCollection
import scala.swing._

/**
 * A tiny factory that allows to create some JFreeChart charts which are already configured the way we want
 * them for QV.
 */
object QvChartFactory {

  /**
   * Creates a time line chart; i.e. a XY-Plot for arbitrary functions which has a time line as the domain axis.
   * @param title
   * @return
   */
  def createTimelineChart(title: String): JFreeChart = {
    
    val chart = ChartFactory.createTimeSeriesChart(
      title,
      null,
      null,
      null,
      true,
      true,
      false
    )

    // this is an xy-plot; configure it as needed
    val plot = chart.getXYPlot
    plot.setOutlineVisible(false)
    plot.setBackgroundPaint(Color.white)
    plot.setRangeTickBandPaint(new Color(248, 248, 255))
    plot.setDomainGridlinePaint(Color.gray)
    plot.setRangeGridlinePaint(Color.gray)

    chart
  }

  /**
   * Creates a time line bar chart.
   * @param title
   * @param data
   * @return
   */
  def createTimelineBarChart(title: String, data: TaskSeriesCollection): JFreeChart = {

    val chart = ChartFactory.createGanttChart(
      title, // the title
      null,  // no x-axis text
      null,  // no y-axis text
      data,
      true,
      true,
      false
    )

    // this is a category plot, configure it as needed
    val plot = chart.getCategoryPlot
    plot.setOutlineVisible(false)
    plot.setBackgroundPaint(Color.white)
    plot.setDomainGridlinePaint(Color.gray)
    plot.setRangeGridlinePaint(Color.gray)
    plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT)
    // jfreechart uses a color coding for different bars according to the task series they belong to,
    // this is not what we want here, therefore we supply our own renderer
    plot.setRenderer(new ColorCodedGanttRenderer(data))

    chart
  }

  /**
   * Creates a histogram chart, i.e. a stacked bar chart.
   * @param data
   * @return
   */
  def createHistogramChart(data: CategoryDataset): JFreeChart = {

    val chart = ChartFactory.createStackedBarChart(
      null, // no title
      null, // no x-axis text
      null, // no y-axis text
      data,
      PlotOrientation.VERTICAL,
      true,
      true,
      false
    )

    // this is a category plot, configure it as needed
    val plot = chart.getCategoryPlot
    plot.setOutlineVisible(false)
    plot.setBackgroundPaint(Color.white)
    plot.setDomainGridlinePaint(Color.gray)
    plot.setRangeGridlinePaint(Color.gray)
    // replace default renderer - which does some pseudo 3D gradient stuff - with a basic one
    plot.getRenderer match {
      case renderer: BarRenderer =>
        renderer.setBarPainter(new StandardBarPainter())
        renderer.setDrawBarOutline(true)
        renderer.setShadowVisible(false)
    }

    chart
  }


}
