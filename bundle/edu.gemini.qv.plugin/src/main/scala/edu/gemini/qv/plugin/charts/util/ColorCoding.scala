package edu.gemini.qv.plugin.charts.util

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.filter.core.{EmptyFilter, Filter}
import java.awt.{Color, Paint, Stroke}

import edu.gemini.qv.plugin.QvContext
import org.jfree.chart.labels.StandardXYToolTipGenerator
import org.jfree.chart.renderer.category.{GanttRenderer, StandardBarPainter}
import org.jfree.chart.renderer.xy.{XYLineAndShapeRenderer, XYSplineRenderer}
import org.jfree.chart.{LegendItem, LegendItemCollection}
import org.jfree.data.gantt.{Task, TaskSeries, TaskSeriesCollection}
import org.jfree.data.time.SimpleTimePeriod
import org.jfree.data.xy.XYDataset

import scala.swing.Color

/**
 * Defines a color for a given filter.
 */
case class ColorCode(filter: Filter, color: Color) {
  def name = filter.name
  def label = filter.label
}

/**
 * A helper class that defines a color coded tasks that can be used for bar charts.
 * @param filter
 * @param color
 * @param label
 * @param start
 * @param end
 */
class ColorCodedTask(val filter: Filter, val color: Color, label: String, start: Long, end: Long) extends Task(label, new SimpleTimePeriod(start, end))

/**
 * A helper class that renders color coded tasks in a JFreeChart gantt chart.
 * @param data
 */
class ColorCodedGanttRenderer(data: TaskSeriesCollection) extends GanttRenderer {

  setBarPainter(new StandardBarPainter())
  setDrawBarOutline(true)
  setShadowVisible(false)

  override def getItemPaint(row: Int, col: Int): Paint = {
    data.getRowKeys.get(row) match {
      case taskSeries: TaskSeries =>
        taskSeries.getTasks.get(col) match {
          case task: ColorCodedTask => task.color
          case _ => throw new RuntimeException("Use ColorCodedTask objects in conjunction with ColorCodedGanttRenderer!")
        }
      case _ => throw new RuntimeException("Use TaskSeries objects in conjunction with ColorCodedGanttRenderer!")
    }
  }
}

/**
 * Assign colors to observations using the given filter and color pairs.
 */
case class ColorCoding(codes: Set[ColorCode])  {

  import ColorCoding._

  def color(o: Obs, ctx: QvContext): Color = code(o, ctx).color

  def color(obs: Set[Obs], ctx: QvContext): Color = code(obs, ctx).color

  def code(group: Filter): ColorCode = {
    val matching = codes.find(c => c.filter == group)
    matching.getOrElse(OtherCode)
  }

  def code(o: Obs, ctx: QvContext): ColorCode = {
    val matching = codes.filter(cc => cc.filter.predicate(o, ctx))
    matching.size match {
      case 0 => OtherCode
      case 1 => matching.head
      case _ => AmbiguousCode
    }
  }

  def code(obs: Set[Obs], ctx: QvContext): ColorCode = {
    val codes = obs.map(code(_, ctx))
    codes.size match {
      case 0 => OtherCode
      case 1 => codes.head
      case _ => AmbiguousCode
    }
  }

  // == helpers for creating custom renderer based on given configuration

  /** Gets a spline renderer with colors according to the given color coding. */
  def splineRenderer(ctx: QvContext, obs: Seq[Obs], stroke: Stroke): XYLineAndShapeRenderer = renderer(ctx, new XYSplineRenderer(), obs, stroke)

  /** Gets a line renderer with colors according to the given color coding. */
  def lineRenderer(ctx: QvContext, obs: Seq[Obs], stroke: Stroke, color: Option[Color] = None): XYLineAndShapeRenderer = renderer(ctx, new XYLineAndShapeRenderer(), obs, stroke, color)

  /** Inits a renderer for the given sequence of observations. Note that the sequence is relevant! */
  private def renderer(ctx: QvContext, renderer: XYLineAndShapeRenderer, obs: Seq[Obs], stroke: Stroke, colorOverride: Option[Color] = None): XYLineAndShapeRenderer = {
    renderer.setBaseShapesVisible(false)
    obs.zipWithIndex.map({case (o, ix) => {
      renderer.setSeriesPaint(ix, colorOverride.getOrElse(color(o, ctx)))
      renderer.setSeriesStroke(ix, stroke)
      renderer.setSeriesToolTipGenerator(ix, new StandardXYToolTipGenerator() {
        override def generateToolTip(dataset: XYDataset, series: Int, item: Int): String = {
          o.getObsId
        }
      })
    }})
    renderer
  }

  // == helpers for creating custom chart legends based on given color coding

  /**
   * Creates a chart legend with legend items that cover all given observations.
   * @param observations
   * @return
   */
  def legend(ctx: QvContext, observations: Set[Obs]): LegendItemCollection = {
    val obsCodes = observations.map(code(_, ctx))
    legendForCodes(obsCodes)
  }

  /**
   * Creates a chart legend with legend items that cover all given observations.
   * @param observations
   * @param groups
   * @return
   */
  def legend(ctx: QvContext, observations: Set[Obs], groups: Set[Filter]): LegendItemCollection = {
    val obsCodes = groups.map(f => observations.filter(f.predicate(_, ctx))).map(code(_, ctx)) // group observations and get color code for those groups
    legendForCodes(obsCodes)
  }

  private def legendForCodes(codes: Set[ColorCode]): LegendItemCollection = {
    val legend = new LegendItemCollection()
    if (codes.size <= MaxLegendItems) {
      val items = codes.toSeq.sortBy(_.name).map(legendItem)
      items.foreach(legend.add)
    } else {
      legend.add(new LegendItem("Too many items for legend.", InactiveColor))
    }
    legend
  }

  private def legendItem(code: ColorCode) = {
    val l = new LegendItem(code.name, code.color)
    l.setSeriesKey(code.filter)
    l
  }

}

object ColorCoding {

  val MaxLegendItems = 25
  val InactiveColor = Color.lightGray
  val OtherColor = Color.darkGray
  val AmbiguousColor = Color.orange

  val OtherCode = ColorCode(EmptyFilter("Other"), OtherColor)
  val AmbiguousCode = ColorCode(EmptyFilter("Ambiguous"), AmbiguousColor)

  def apply(activeGroups: Set[Filter], inactiveGroups: Set[Filter] = Set(), colorFunc: Int => Color = qualitativeColor): ColorCoding = {

    // Order the groups by their names before assigning colors. This will make sure that every time the program
    // is run (and we have the same set of groups, e.g. one for each instrument) the same colors are used for the
    // same groups (e.g. "GNIRS" is always green) which makes it easier for the users to work with QV.
    // Note that we use the name (e.g. "GNIRS") as opposed to the filter label (e.g. "Instrument" for all instruments).
    val orderedGroups: Seq[Filter] = (activeGroups ++ inactiveGroups).toSeq.sortBy(_.name)

    ColorCoding(
      orderedGroups.zipWithIndex.map({
        {case (g, ix) => ColorCode(g, if (activeGroups.contains(g)) colorFunc(ix) else ColorCoding.InactiveColor)}
      }).toSet
    )

  }

  /** Gets a qualitative color. */
  def qualitativeColor(i: Int): Color = qualitativeColors(i % qualitativeColors.length)

  // Color palettes created with: http://colorbrewer2.org/
  // 11 class paired (Leaving out the light yellow from the 12 class paired schema
  // because the yellow is poorly visible).
  def qualitativeColors: Seq[Color] = Seq(

    // === ideally we only need up to 11 colors, but often more are needed ..
    new Color(166,206,227),
    new Color(31,120,180),
    new Color(178,223,138),
    new Color(51,160,44),
    new Color(251,154,153),
    new Color(227,26,28),
    new Color(253,191,111),
    new Color(255,127,0),
    new Color(202,178,214),
    new Color(106,61,154),
    new Color(177,89,40),

    // === .. that's why we repeat the same colors here, but make them a tad bit darker ..
    new Color(166,206,227).darker(),
    new Color(31,120,180).darker(),
    new Color(178,223,138).darker(),
    new Color(51,160,44).darker(),
    new Color(251,154,153).darker(),
    new Color(227,26,28).darker(),
    new Color(253,191,111).darker(),
    new Color(255,127,0).darker(),
    new Color(202,178,214).darker(),
    new Color(106,61,154).darker(),
    new Color(177,89,40).darker()

    // === .. if more than 22 colors are needed the same colors are recycled over and over again.
    // (To be honest, more than 22 colors can not be reliably differentiated anyway.. probably
    // a meaningful limit is 5 to 10, that's why color brewer only goes up to 12 different colors.)
  )

  /** Gets a sequential color for a given value and maximum. */
  def sequentialColor(value: Double, max: Double): Color = {
    require(value >= 0)
    require(max >= 0)
    require(value <= max)
    sequentialColors(Math.round(value / max * (sequentialColors.size - 1)).toInt)
  }

  def sequentialColors: Seq[Color] = Seq(
    new Color(0xf7, 0xfc, 0xfd),
    new Color(0xe5, 0xf5, 0xf9),
    new Color(0xcc, 0xec, 0xe6),
    new Color(0x99, 0xd8, 0xc9),
    new Color(0x66, 0xc2, 0xa4),
    new Color(0x41, 0xae, 0x76),
    new Color(0x23, 0x8b, 0x45),
    new Color(0x00, 0x6d, 0x2c),
    new Color(0x00, 0x44, 0x1b)
  )


}


