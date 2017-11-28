package edu.gemini.qv.plugin.charts

import edu.gemini.qv.plugin.chart.Chart._
import edu.gemini.qv.plugin.chart._
import edu.gemini.qv.plugin.charts.util.{ColorCoding, QvChartFactory}
import edu.gemini.qv.plugin.data.{CategorizedXYValues, FilterProvider}
import edu.gemini.qv.plugin.filter.core.Filter
import edu.gemini.qv.plugin.filter.core.Filter.{HasDummyTarget, RA}
import edu.gemini.qv.plugin.QvContext
import edu.gemini.qv.plugin.selector.OptionsSelector
import edu.gemini.qv.plugin.selector.OptionsSelector.{AvailableHours, DarkHours, EmptyCategories, RaAsLst}
import edu.gemini.qv.plugin.util.SemesterData
import edu.gemini.util.skycalc.calc.Interval
import java.awt.Color
import java.time.Instant
import java.time.format.DateTimeFormatter

import edu.gemini.shared.util.DateTimeUtils
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.CategoryLabelPositions
import org.jfree.chart.plot.DatasetRenderingOrder
import org.jfree.chart.renderer.category.{BarRenderer, LineAndShapeRenderer}
import org.jfree.data.category.DefaultCategoryDataset


object HistogramChart {
  def apply(ctx: QvContext, xAxis: Axis, yAxis: Axis, calculation: Calculation, data: FilterProvider, range: Interval, details: OptionsSelector) = {
    val categorizedData = CategorizedXYValues(ctx, xAxis.groups, yAxis.groups, data.observations, calculation.value)
    new HistogramChart(ctx, xAxis, yAxis, calculation, categorizedData, data, range, details)
  }
}

/**
 */
class HistogramChart(ctx: QvContext, xAxis: Axis, yAxis: Axis, calculation: Calculation, categorizedData: CategorizedXYValues, data: FilterProvider, range: Interval, details: OptionsSelector) {

  def createChart: JFreeChart = {

    val chart = QvChartFactory.createHistogramChart(dataset)
    val plot = chart.getCategoryPlot

    // -- heuristic that rotates x axis labels in case we seem not to have enough space
    // (is there a better way to do this?)
    if (visibleXGroups.size > 10) {
      val domainAxis = plot.getDomainAxis
      domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45)
    }

    // -- add datasets for available and dark hours (if needed and if result of calculation is a number of hours)
    if (details.isSelected(AvailableHours) && xAxis.isTime && calculation.isHours) {
      plot.setDataset(1, scienceTimeDataSet)
      plot.setRenderer(1, new LineAndShapeRenderer())
    }

    if (details.isSelected(DarkHours) && xAxis.isTime && calculation.isHours) {
      plot.setDataset(2, darkTimeDataSet)
      plot.setRenderer(2, new LineAndShapeRenderer())
    }

    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD)

    // replace default legend with a custom one
    val colorCoding = ColorCoding(yAxis.groups.toSet)
    val legend = colorCoding.legend(ctx, categorizedData.observations)
    plot.setFixedLegendItems(legend)

    // -- do some renderer magic
    val renderer = plot.getRenderer.asInstanceOf[BarRenderer]

    // try to use the same color for each group even if some groups are added/removed
    // (use the indices of the visible groups in the seq of all groups on the y-axis)
    visibleYGroups.zipWithIndex.foreach({case (f, ix) =>
      val code = colorCoding.code(f)
      renderer.setSeriesPaint(ix, code.color)
      renderer.setSeriesOutlinePaint(ix, Color.darkGray)
    })

    chart
  }


  private val dataset = {
    val data = new DefaultCategoryDataset()

    val empty = Map[Filter, Double]()
    val values: Seq[(Filter, Filter, Double)] = for {
      xGroup <- visibleXGroups
      yGroup <- visibleYGroups
    } yield (xGroup, yGroup, categorizedData.data.getOrElse(xGroup, empty).getOrElse(yGroup, 0.toDouble))

    // add values in correct order
    values.foreach({v => data.addValue(
      v._3,
      ChartItem(yName(v._2), v._2),
      ChartItem(xName(v._1), v._1))}
    )

    data
  }

  /** Show all categories along x if we show time (RA) or if configured to do so, otherwise skip empty ones. */
  private def visibleXGroups =
    if (xAxis.isTime)
      // for time axis show "dummy" group only if it is not empty (i.e. part of active groups)
      if (categorizedData.activeXGroups.exists(dummyRaFilter)) categorizedData.xCategories
      else categorizedData.xCategories.filterNot(dummyRaFilter)
    else if (details.isSelected(EmptyCategories)) categorizedData.xCategories
    else categorizedData.activeXGroups

  /** Show all categories along y if configured to do so, otherwise skip empty ones. */
  private def visibleYGroups =
    if (details.isSelected(EmptyCategories)) categorizedData.yCategories
    else categorizedData.activeYGroups

  private val MMMddFormatter = DateTimeFormatter.ofPattern("MMM/dd").withZone(ctx.site.timezone.toZoneId)
  private def xName(f: Filter) = f match {
    case f: RA if details.isSelected(RaAsLst) =>
      val optLst = SemesterData.lst(ctx.site, range, f)
      optLst.map(l => MMMddFormatter.format(Instant.ofEpochMilli(l))).getOrElse(f.name)
    case _ =>
      f.name
  }

  private def yName(f: Filter) = f.name

  private def dummyRaFilter(f: Filter) = f match {
    case HasDummyTarget(_) => true
    case _ => false
  }

  private def scienceTimeDataSet = {
    val d = new DefaultCategoryDataset
    categorizedData.xCategories.foreach {
      case ra: RA =>
        val seconds = SemesterData.scienceTime(ctx.site, range, ra) / DateTimeUtils.MillisecondsPerSecond
        d.addValue(seconds.toDouble / DateTimeUtils.SecondsPerHour, "Available", ChartItem(xName(ra), ra))

      case other =>
        d.addValue(null, "Available", ChartItem(xName(other), other))
    }
    d
  }

  private def darkTimeDataSet = {
    val d = new DefaultCategoryDataset
    categorizedData.xCategories.foreach {
      case ra: RA =>
        val seconds = SemesterData.darkTime(ctx.site, range, ra) / DateTimeUtils.MillisecondsPerSecond
        d.addValue(seconds.toDouble/DateTimeUtils.SecondsPerHour, "Dark", ChartItem(xName(ra), ra))

      case other =>
        d.addValue(null, "Dark", ChartItem(xName(other), other))
    }
    d
  }

}

// helper class
case class ChartItem(name: String, filter: Filter) extends Comparable[ChartItem] {
  override def toString: String = name
  override def compareTo(other: ChartItem) = name.compareTo(other.name)
}

