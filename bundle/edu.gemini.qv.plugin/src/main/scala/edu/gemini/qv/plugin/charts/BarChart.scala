package edu.gemini.qv.plugin.charts

import edu.gemini.qv.plugin.charts.util.{ColorCodedTask, ColorCoding, QvChartFactory}
import edu.gemini.qv.plugin.data.CategorizedYData
import edu.gemini.qv.plugin.filter.core.Filter
import edu.gemini.qv.plugin.selector.OptionsSelector.IgnoreDaytime
import edu.gemini.qv.plugin.selector.{ConstraintsSelector, OptionsSelector}
import edu.gemini.qv.plugin.util._
import edu.gemini.spModel.core.Site
import edu.gemini.util.skycalc.Night
import edu.gemini.util.skycalc.calc.Solution
import java.util.TimeZone

import edu.gemini.qv.plugin.QvContext
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.DateAxis
import org.jfree.data.gantt.{TaskSeries, TaskSeriesCollection}


/**
 * Bar charts are essentially one form of visibility plots which show when an observation (or a group of observations)
 * is observable (visible) taking all restrictions into account like elevation limits, timing windows etc.
 */
case class BarChart(ctx: QvContext, site: Site, categorizedData: CategorizedYData, nights: Seq[Night], timeZone: TimeZone, constraints: ConstraintsSelector, details: OptionsSelector, colorCodingAxis: edu.gemini.qv.plugin.chart.Axis, first: Int, max: Int) extends VisibilityCategoryChart {

  def createChart: JFreeChart = {

    val colorCoding = ColorCoding(colorCodingAxis.groups.toSet)

    val data = visibilities(colorCoding, details.selected.contains(IgnoreDaytime))
    val chart = QvChartFactory.createTimelineBarChart(title("Visibility for"), data)

    val plot = chart.getCategoryPlot
    val dateAxis = plot.getRangeAxis.asInstanceOf[DateAxis]
    dateAxis.setRange(start, end)
    initDateAxis(dateAxis, nights, timeZone)

    addDetails(plot, categorizedData.observations)

    val legend = colorCoding.legend(ctx, categorizedData.observations, categorizedData.activeYGroups.toSet)
    plot.setFixedLegendItems(legend)

    chart
  }

  private def visibilities(colorCoding: ColorCoding, daysOnly: Boolean): TaskSeriesCollection = {
    val taskSeries = new TaskSeries("Visibilities")
    val nonEmptyCategories = categorizedData.activeYGroups.filter(categorizedData.observationsFor(_).size > 0)
    visibleCategories(nonEmptyCategories).foreach(yFilter => {
      val obs = categorizedData.observationsFor(yFilter)
      val colorCode = colorCoding.color(obs, ctx)
      val solution = if (nights.isEmpty) Solution() else SolutionProvider(site).solution(nights, constraints.selected, obs)
      val visibility = if (daysOnly) solution.allDay(site.timezone) else solution
      val earliest = solution.earliest.getOrElse(0L)
      val latest = solution.latest.getOrElse(0L)
      val task = new ColorCodedTask(yFilter, colorCode, yFilter.name + " (" + obs.size + ")", earliest, latest)
      visibility.intervals.foreach(i => {
        task.addSubtask(new ColorCodedTask(yFilter, colorCode, "", i.start, i.end))
      })
      taskSeries.add(task)
    })

    val collection = new TaskSeriesCollection
    collection.add(taskSeries)

    collection
  }

  private def visibleCategories(categories: Seq[Filter]) = categories.size match {
    case i if i < max => categories
    case i if i-first < max => categories.takeRight(max)
    case _ => categories.drop(first).take(max)
  }

}
