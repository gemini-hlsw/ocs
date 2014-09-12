package edu.gemini.qv.plugin.chart.ui

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.charts.ChartItem
import edu.gemini.qv.plugin.charts.util.{ColorCoding, ColorCodedTask}
import edu.gemini.qv.plugin.filter.core.{EmptyFilter, Filter, FilterAnd}
import java.awt.Color
import java.awt.event.InputEvent
import javax.swing.border.EmptyBorder
import org.jfree.chart._
import org.jfree.chart.entity.{LegendItemEntity, CategoryItemEntity, XYItemEntity}
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.data.gantt.TaskSeriesCollection
import scala.Some
import scala.swing.Component
import scala.swing.event.Event


case class ObservationSelected(o: Obs) extends Event
case class ObservationsSelected(f: Filter) extends Event
case class CategorySelected(filter: Filter) extends Event
case class CategorySubSelected(filter: Filter) extends Event

object JFChartComponent {

  // an arbitrary empty chart we use as a stand in while we are waiting for data
  val EmptyChart = ChartFactory.createTimeSeriesChart(
      "",
      null,
      null,
      null,
      true,
      true,
      false
    )

}

/**
 * Scala Swing wrapper for JFreeChart chart components.
 */
class JFChartComponent extends Component {
  override lazy val peer = new ChartPanel(JFChartComponent.EmptyChart)

  peer.setBackground(Color.white)
  peer.setBorder(new EmptyBorder(10,10,10,10)) // add some padding

  // don't limit drawing resolution
  peer.setMaximumDrawHeight(Int.MaxValue)
  peer.setMaximumDrawWidth(Int.MaxValue)

  // install event listener
  peer.addChartMouseListener(new MouseListener)

  /**
   * Updates a chart by fully recreating and redrawing it.
   * This will update the chart in the ChartPanel.
   * @param chart
   */
  def updateChart(chart: JFreeChart) = peer.setChart(chart)


  private class MouseListener extends ChartMouseListener {

    // react to mouse clicks
    def chartMouseClicked(event: ChartMouseEvent): Unit = {

      if (event.getEntity == null) return

      event.getEntity match {

        // legend item was clicked
        case l: LegendItemEntity => l.getSeriesKey match {
          case f: Filter => f match {
            // TODO: It would be nice to support clicks on "Other" and "Ambiguous" legend items, however we would
            // TODO: have to create filters covering the corresponding observations dynamically, which is a bit tricky.
            case EmptyFilter("Other") =>      // For now: Ignore clicks on Other group in legend
            case EmptyFilter("Ambiguous") =>  // For now: Ignore clicks on Ambiguous group in legend
            case _ => publish(ObservationsSelected(f))
          }
          case _ => // Ignore
        }

        // a curve was clicked
        case e: XYItemEntity =>
          e.getDataset.getSeriesKey(e.getSeriesIndex) match {
            case o: Obs => publish(ObservationSelected(o))
            case f: Filter => publish(ObservationsSelected(f))
            case _ => // Ignore
          }

        // a category was clicked
        case e: CategoryItemEntity => {

          val (rowFilter, colFilter) = e.getDataset match {
            case d: TaskSeriesCollection =>
              // we are dealing with the bar chart
              val t = d.getSeries(e.getRowKey).get(e.getColumnKey.toString)
              (None, Some(t.asInstanceOf[ColorCodedTask].filter))
            case d: DefaultCategoryDataset =>
              // this is the histogram
              val row = e.getRowKey.asInstanceOf[ChartItem].filter
              val col = e.getColumnKey.asInstanceOf[ChartItem].filter
              (Some(row), Some(col))

            case _ => (None, None)  // don't know what this is
          }

          val alt = (event.getTrigger.getModifiers & InputEvent.ALT_MASK) != 0
          val f = (rowFilter, colFilter) match {
            case (Some(r), Some(c)) => Some(FilterAnd(r, c))
            case (Some(r), None) => Some(r)
            case (None, Some(c)) => Some(c)
            case _ => None
          }

          if (f.isDefined) {
            if (!alt) publish(CategorySubSelected(f.get))
            else publish(CategorySelected(f.get))
          }

        }

        // everything else can be ignored
        case _ => // ignore
      }

    }

    // not interested in mouse movements for now
    def chartMouseMoved(event: ChartMouseEvent): Unit = {

    }
  }
}

