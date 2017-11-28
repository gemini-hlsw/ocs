package edu.gemini.qv.plugin.charts

import edu.gemini.qpt.shared.sp.{Conds, Obs}
import edu.gemini.qv.plugin.charts.util.{ColorCoding, XYAxes, XYPlotter}
import edu.gemini.qv.plugin.selector.OptionsSelector._
import edu.gemini.qv.plugin.selector.{ConstraintsSelector, OptionsSelector}
import edu.gemini.qv.plugin.ui.QvGui
import edu.gemini.qv.plugin.util.ScheduleCache._
import edu.gemini.qv.plugin.util.SolutionProvider
import edu.gemini.spModel.core.ProgramType.Classical
import edu.gemini.spModel.core.Site
import edu.gemini.util.skycalc._
import edu.gemini.util.skycalc.calc._
import java.awt.image.BufferedImage
import java.awt.{BasicStroke, Color, GradientPaint, Paint}
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZonedDateTime}
import java.util.TimeZone
import javax.swing.JPanel

import edu.gemini.qv.plugin.QvContext
import org.jfree.chart.LegendItem
import org.jfree.chart.annotations.{XYAnnotation, XYImageAnnotation, XYTextAnnotation}
import org.jfree.chart.axis._
import org.jfree.chart.plot._
import org.jfree.data.xy.{XYSeries, XYSeriesCollection}
import org.jfree.ui.{Layer, RectangleAnchor, RectangleInsets, TextAnchor}

import scala.concurrent.duration._


trait VisibilityXYChart extends VisibilityChart with XYAxes {

  val observations: Set[Obs]

  val grays = ColorCoding.qualitativeColors.map(c => Color.lightGray)

  val moonRenderer = XYPlotter.splineRenderer(QvGui.MoonColor, SolidVeryThickStroke)

  private def addMoon(plot: XYPlot): Unit = {
    val sampling = if (duration.asDays < 8) regularSampling() else midNightTimeSampling()
    val ix = plot.getDatasetCount
    plot.setDataset(ix, moonDataset(sampling))
    plot.setRenderer(ix, moonRenderer)
    if (MainElevationAxis != plot.getRangeAxis && MainMidNightElevationAxis != plot.getRangeAxis) {
      plot.setRangeAxis(ix, ElevationAxis)
      plot.mapDatasetToRangeAxis(ix, ix)
    }
  }

  protected def addDetails(plot: XYPlot, selected: Set[Obs], daysShowing: Int): Unit = {
    // add details to plot according to user selection
    details.selected.foreach {
      case Now                => plot.addDomainMarker(timeMarker(System.currentTimeMillis()))
      case Twilights          => twilightMarkers().foreach(plot.addDomainMarker(_, Layer.BACKGROUND))
      case MoonPhases         => moonIcons(plot).foreach(plot.addAnnotation)
      case MoonElevation      => addMoon(plot)
      case SkyBrightnessCurve => findAxis(plot, SkyBrightnessAxis).foreach(ix => skyBrightnessMarkers.foreach(plot.addRangeMarker(ix, _, Layer.FOREGROUND)))
      case InsideMarkers      => observableMarkers(selected, daysShowing > 7).foreach(plot.addDomainMarker(_, Layer.BACKGROUND))
      case OutsideMarkers     => notObservableMarkers(selected, daysShowing > 7).foreach(plot.addDomainMarker(_, Layer.BACKGROUND))
      case Schedule           => scheduleMarker(nights, constraints.selected).foreach(plot.addDomainMarker(_))
      case _                  => // ignore other details
    }
  }

  private def regularSampling(): Vector[Long] = {
    val rate = (end - start) / 250
    val times = for (i <- 0 to 250) yield start + (i * rate)
    times.toVector
  }

  private def midNightTimeSampling(): Vector[Long] = {
    nights.map(_.middleNightTime).toVector
  }

  private def moonDataset(times: Vector[Long]): XYSeriesCollection = {
    val moon = MoonCalculator(site, times)
    val data = new XYSeriesCollection
    val series = new XYSeries("Moon")
    times.foreach(t => {
      series.add(t, moon.valueAt(MoonCalculator.Fields.Elevation.id, t))
    })
    data.addSeries(series)
    data
  }

}

trait VisibilityCategoryChart extends VisibilityChart {

  protected def addDetails(plot: CategoryPlot, observations: Set[Obs]): Unit = {
    // add details to plot according to user selection
    details.selected.foreach {
      case Now        =>
        plot.addRangeMarker(timeMarker(System.currentTimeMillis()))
      case Twilights  =>
        twilightMarkers().
          foreach(plot.addRangeMarker(_, Layer.BACKGROUND))
      case InsideMarkers =>
        observableMarkers(observations, details.isSelected(IgnoreDaytime)).
          foreach(plot.addRangeMarker(_, Layer.BACKGROUND))
      case OutsideMarkers =>
        notObservableMarkers(observations, details.isSelected(IgnoreDaytime)).
          foreach(plot.addRangeMarker(_, Layer.BACKGROUND))
      case MoonPhases => // TODO: how to show icons on a category plot?
      case Schedule =>
        scheduleMarker(nights, constraints.selected).map(plot.addRangeMarker(_, Layer.BACKGROUND))
      case _  => // ignore other details
    }

  }

}

/**
 */
trait VisibilityChart {

  val ctx: QvContext

  val SolidThickStroke = new BasicStroke(5)
  val SolidVeryThickStroke = new BasicStroke(8)
  val SolidThinStroke = new BasicStroke(2)
  val DashedThickStroke = new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, Array(2.0f, 3.0f), 0.0f)
  val DashedThinStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, Array(2.0f, 3.0f), 0.0f)

  val site: Site
  val nights: Seq[Night]
  val details: OptionsSelector
  val constraints: ConstraintsSelector

  val start: Long = nights.headOption.map(_.sunset).getOrElse(System.currentTimeMillis() - 12.hours.toMillis)
  val end: Long = nights.lastOption.map(_.sunrise).getOrElse(System.currentTimeMillis() + 12.hours.toMillis)
  val duration: Interval = Interval(start, end)

  /** Initialise the tick marks on the date axis the way we want them. */
  protected def initDateAxis(dateAxis: DateAxis, nights: Seq[Night], timeZone: TimeZone): Unit = {
    nights.size match {
      case i if i <= 1 =>
        val sdf = new SimpleDateFormat("HH:mm"); sdf.setTimeZone(timeZone)
        dateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.MINUTE, 30, sdf))
        dateAxis.setMinorTickCount(6)
        dateAxis.setMinorTickMarksVisible(true)
      case i if i <= 7 =>
        val sdf = new SimpleDateFormat("MMM dd"); sdf.setTimeZone(timeZone)
        dateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.HOUR, 24, sdf))
        dateAxis.setMinorTickCount(8)
        dateAxis.setMinorTickMarksVisible(true)
      case i if i <= 30 =>
        val sdf = new SimpleDateFormat("MMM dd"); sdf.setTimeZone(timeZone)
        dateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.DAY, 3, sdf))
        dateAxis.setMinorTickCount(3)
        dateAxis.setMinorTickMarksVisible(true)
      case i if i <= 100 =>
        val sdf = new SimpleDateFormat("MMM dd"); sdf.setTimeZone(timeZone)
        dateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.DAY, 28, sdf))
        dateAxis.setMinorTickCount(4)
        dateAxis.setMinorTickMarksVisible(true)
      case _ =>
        val sdf = new SimpleDateFormat("MMM dd"); sdf.setTimeZone(timeZone)
        dateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.DAY, 28, sdf))
        dateAxis.setMinorTickCount(4)
        dateAxis.setMinorTickMarksVisible(true)
    }
  }

  protected def observableMarkers(selected: Set[Obs], allDay: Boolean): Seq[IntervalMarker] = {
    val solution = SolutionProvider(site).solution(nights, constraints.selected, selected)
    val reducedSolution = if (allDay) solution.allDay(site.timezone()) else solution
    reducedSolution.intervals.map(i => {
      val marker = new IntervalMarker(i.start, i.end)
      marker.setPaint(new Color(0, 255, 0, 30))
      marker
    })
  }

  protected def notObservableMarkers(selected: Set[Obs], allDay: Boolean): Seq[IntervalMarker] = {
    val solution = SolutionProvider(site).solution(nights, constraints.selected, selected)
    val reducedSolution = if (allDay) solution.allDay(site.timezone()) else solution
    (Interval(0, nights.head.start) +: reducedSolution.intervals :+ Interval(nights.last.end, Long.MaxValue)).sliding(2).map(i => {
      val marker = new IntervalMarker(i(0).end, i(1).start)
      marker.setPaint(new Color(255, 0, 0, 30))
      marker
    }).toSeq
  }

  protected def timeMarker(time: Long): ValueMarker =
    new ValueMarker(time, Color.orange, new BasicStroke(2.0f))

  protected def twilightMarkers(): Seq[IntervalMarker] = {
    nights.map(night => {
      // nautical twilight
      val marker = new IntervalMarker(night.nauticalTwilightStart, night.nauticalTwilightEnd)
      marker.setPaint(new Color(232, 232, 255, 128))
      marker
    }) ++
    nights.map(night => {
      // sunrise/sunset
      val marker = new IntervalMarker(night.sunset, night.sunrise)
      marker.setPaint(new Color(222, 222, 255, 50))
      marker
    })
  }

  protected val skyBrightnessMarkers: Seq[ValueMarker] = {
    val sb20 = new ValueMarker(Conds.getBrightestMagnitude(20), Color.lightGray, SolidThinStroke)
    sb20.setLabel("SB = 20%")
    sb20.setLabelAnchor(RectangleAnchor.TOP_RIGHT)
    sb20.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT)

    val sb50 = new ValueMarker(Conds.getBrightestMagnitude(50), Color.lightGray, SolidThinStroke)
    sb50.setLabel("SB = 50%")
    sb50.setLabelAnchor(RectangleAnchor.TOP_RIGHT)
    sb50.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT)

    val sb80 = new ValueMarker(Conds.getBrightestMagnitude(80), Color.lightGray, SolidThinStroke)
    sb80.setLabel("SB = 80%")
    sb80.setLabelAnchor(RectangleAnchor.TOP_RIGHT)
    sb80.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT)

    Seq(sb20, sb50, sb80)
  }

  protected def moonIcons(plot: XYPlot): Seq[XYAnnotation] = {

    val width = 18
    val yPos = {
      val a = plot.getRangeAxis
      val d = a.getUpperBound - a.getLowerBound
      if (a.isInverted) a.getLowerBound + d*0.05 else a.getLowerBound + d*0.95
    }

    def moonIcon(time: Long, illumination: Double, waxing: Boolean) = {
      val icon = new MoonIcon(width, illumination, waxing)
      val image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB)
      icon.paintIcon(new JPanel(), image.getGraphics, 0, 0)
      new XYImageAnnotation(time, yPos, image)
    }

    val interval = Interval(start, end)
    interval.duration.milliseconds.toDays match {
      case d if d <= 1 => {
        val moonCalc = MoonCalculator(site, (end + start)/2)
        val illumination = moonCalc.illuminatedFraction
        val waxing = moonCalc.phaseAngle > 180
        val moonIconx = moonIcon(end - 20.minutes.toMillis, illumination, waxing)
        val moonLabel = new XYTextAnnotation(f"${illumination*100}%3.0f%%", moonIconx.getX + 12.minutes.toMillis, yPos)
        Seq(moonIconx, moonLabel)
      }
      case d if d <= 15 => {
        val phases = Interval(start - 15.days.toMillis, end)
        MoonCalculator.newMoons(site, phases).map(t => moonIcon(t, 0, waxing=false)) ++
        MoonCalculator.fullMoons(site, phases).map(t => moonIcon(t, 1, waxing=false)) ++
        MoonCalculator.firstQuarterMoons(site, phases).map(t => moonIcon(t, 0.5, waxing=true)) ++
        MoonCalculator.lastQuarterMoons(site, phases).map(t => moonIcon(t, 0.5, waxing=false))
      }
      case _ => {
        val phases = Interval(start - 15.days.toMillis, end)
        MoonCalculator.newMoons(site, phases).map(t => moonIcon(t, 0, waxing=false)) ++
        MoonCalculator.fullMoons(site, phases).map(t => moonIcon(t, 1, waxing=false))
      }
    }
  }

  protected def title(label: String): String = {
    val zone = site.timezone.toZoneId

    val MMMddyyyyFormatter = DateTimeFormatter.ofPattern("MMM dd yyyy").withZone(zone)
    val MMMddFormatter     = DateTimeFormatter.ofPattern("MMM dd")     .withZone(zone)
    val ddyyyyFormatter    = DateTimeFormatter.ofPattern("dd yyyy")    .withZone(zone)

    val s = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), zone)
    val e = ZonedDateTime.ofInstant(Instant.ofEpochMilli(end),   zone)
    val equalY = s.getYear       == e.getYear
    val equalM = s.getMonth      == e.getMonth
    val equalD = s.getDayOfMonth == e.getDayOfMonth
    val dates = (equalY, equalM, equalD) match {
      case (true,  true,  true ) => MMMddyyyyFormatter.format(s)
      case (true,  true,  false) => MMMddFormatter.format(s) + " - " + ddyyyyFormatter.format(e)
      case (true,  false, _    ) => MMMddFormatter.format(s) + " - " + MMMddyyyyFormatter.format(e)
      case (false, _,     _    ) => MMMddyyyyFormatter.format(s) + " - " + MMMddyyyyFormatter.format(e)
    }
    s"$label $dates"
  }

  protected def addLegend(plot: Plot, selectedObs: Set[Obs], colorCoding: ColorCoding): Unit = {
    val coll = colorCoding.legend(ctx, selectedObs)
    if (details.isSelected(MoonElevation) || details.isSelected(MoonHours) ||  details.isSelected(MoonSetRise)) {
      coll.add(new LegendItem("Moon", QvGui.MoonColor))
    }
    plot match {
      case xyPlot: XYPlot => xyPlot.setFixedLegendItems(coll)
      case catPlot: CategoryPlot => catPlot.setFixedLegendItems(coll)
    }

  }

  protected def scheduleMarker(nights: Seq[Night], constraints: Set[SolutionProvider.ConstraintType]): Seq[IntervalMarker] = {

    val lasColor = new Color(233, 233, 0, 30)     // yellow for laser
    val prgColor = new Color(128, 128, 128, 50)   // gray for program constraints (excl. classical)
    val claColor = new Color(237, 201, 175, 50)   // brown for classical
    val ftpColor = new Color(58, 50, 102, 50)     // violet for fast turnaround
    val redColor = new Color(255, 0, 0, 30)       // red for telescope (engineering etc)

    /** Creates markers with given label and color for a set of intervals. */
    def markers(label: String, color: Paint, intervals: Seq[Interval], labelAtBottom: Boolean = false): Seq[IntervalMarker] =
      intervals.map(i => {
        val m = new IntervalMarker(i.start, i.end)
        m.setPaint(color)                         // note: setting paint in constructor will result in different output
        if (nights.size <= 95) {                  // only show label if we are displaying three months or less
          m.setLabel(label)
          m.setLabelOffset(new RectangleInsets(5, 5, 0, 0))
          if (labelAtBottom) {
            m.setLabelAnchor(RectangleAnchor.BOTTOM_LEFT)
            m.setLabelTextAnchor(TextAnchor.BOTTOM_LEFT)
            m.setLabelFont(m.getLabelFont.deriveFont(m.getLabelFont.getSize * 1.4f)) // make font a bit bigger
          } else {
            m.setLabelAnchor(RectangleAnchor.TOP_LEFT)
            m.setLabelTextAnchor(TextAnchor.TOP_LEFT)
            m.setLabelFont(m.getLabelFont.deriveFont(m.getLabelFont.getSize * 1.2f)) // make font a bit bigger
          }
        }
        m
      })

    val schedule = SolutionProvider(site).telescopeSchedule
    constraints.toSeq.flatMap {
        case InstrumentConstraint     =>
          val paint = new GradientPaint(0, 0, new Color(0, 0, 0), 0, 100, new Color(255, 255, 255, 30))
          schedule.instrumentSchedules.flatMap(is => {
            markers(s"${is.instrument.readableStr}-Off", paint, is.intervals, labelAtBottom = true)
          })
        case ProgramConstraint      =>
          schedule.programSchedules.flatMap(cs => {
            val paint = if (cs.id.ptype == Some(Classical)) claColor else prgColor
            markers(cs.id.toString, paint, cs.intervals)
          })
        case LaserConstraint          => markers(LaserConstraint.label, lasColor, schedule.laserSchedule.intervals)
        case ShutdownConstraint       => markers(ShutdownConstraint.label, redColor, schedule.shutdownSchedule.intervals)
        case EngineeringConstraint    => markers(EngineeringConstraint.label, redColor, schedule.engineeringSchedule.intervals)
        case WeatherConstraint        => markers(EngineeringConstraint.label, redColor, schedule.engineeringSchedule.intervals)
        case _ => Seq()
    }
  }



}
