package jsky.app.ot.editor.seq

import java.awt.Color
import javax.swing._

import edu.gemini.itc.shared.PlottingDetails.PlotLimits
import edu.gemini.itc.shared._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, ImageQuality, SkyBackground, WaterVapor}
import jsky.app.ot.util.OtColor
import jsky.util.gui.{NumberBoxWidget, TextBoxWidget, TextBoxWidgetWatcher}
import org.jfree.chart.{ChartPanel, JFreeChart}

import scala.concurrent.Future
import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.ListView.Renderer
import scala.swing.ScrollPane.BarPolicy._
import scala.swing.event._
import scala.swing.{ButtonGroup, _}
import scala.util.{Failure, Success}

object ItcPanel {

  val ErrorIcon   = new ImageIcon(classOf[ItcTableModel].getResource("/resources/images/error_tsk.gif"))
  val SpinnerIcon = new ImageIcon(classOf[ItcTableModel].getResource("/resources/images/spinner16.gif"))

  /** Creates a panel for ITC imaging results. */
  def forImaging(owner: EdIteratorFolder)       = new ItcImagingPanel(owner)

  /** Creates a panel for ITC spectroscopy results. */
  def forSpectroscopy(owner: EdIteratorFolder)  = new ItcSpectroscopyPanel(owner)

}

/** Base trait for different panels which are used to present ITC calculation results to the users. */
sealed trait ItcPanel extends GridBagPanel {

  def owner: EdIteratorFolder
  def table: ItcTable
  def display: Component
  def visibleFor(t: SPComponentType): Boolean

  private val currentConditions = new ConditionsPanel(owner)
  private val analysisMethod    = new AnalysisMethodPanel
  private val message           = new ItcFeedbackPanel(table)

  border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  layout(currentConditions) = new Constraints {
    gridx     = 0
    gridy     = 0
    weightx   = 0.5
    fill      = GridBagPanel.Fill.Horizontal
    insets    = new Insets(10, 10, 10, 10)
  }
  layout(analysisMethod) = new Constraints {
    gridx     = 1
    gridy     = 0
    weightx   = 0.5
    fill      = GridBagPanel.Fill.Horizontal
    insets    = new Insets(10, 10, 10, 10)
  }
  layout(display) = new Constraints {
    gridx     = 0
    gridy     = 2
    weightx   = 1
    weighty   = 1
    gridwidth = 2
    fill      = GridBagPanel.Fill.Both
    insets    = new Insets(10, 0, 0, 0)
  }
  layout(message) = new Constraints {
    anchor    = Anchor.West
    gridx     = 0
    gridy     = 3
    gridwidth = 2
    weightx   = 1
    fill      = GridBagPanel.Fill.Horizontal
    insets    = new Insets(10, 0, 0, 0)
  }

  listenTo(currentConditions, analysisMethod)
  reactions += {
    case SelectionChanged(`currentConditions`)  => table.update()
    case SelectionChanged(`analysisMethod`)     => table.update()
  }

  def update(): Unit = {
    currentConditions.update()
    table.update()
  }

  def analysis: Option[AnalysisMethod] = analysisMethod.analysisMethod

  def conditions: ObservingConditions = currentConditions.conditions

}

/** Panel holding the ITC imaging calculation result table. */
class ItcImagingPanel(val owner: EdIteratorFolder) extends ItcPanel {

  lazy val table = new ItcImagingTable(ItcParametersProvider(owner, this))

  lazy val display = new ScrollPane(table) {
    verticalScrollBarPolicy = AsNeeded
    horizontalScrollBarPolicy = AsNeeded
  }

  /** True for all instruments which support ITC calculations for imaging. */
  def visibleFor(t: SPComponentType): Boolean = t match {
    case SPComponentType.INSTRUMENT_ACQCAM      => true
    case SPComponentType.INSTRUMENT_FLAMINGOS2  => true
    case SPComponentType.INSTRUMENT_GMOS        => true
    case SPComponentType.INSTRUMENT_GMOSSOUTH   => true
    case SPComponentType.INSTRUMENT_GSAOI       => true
    case SPComponentType.INSTRUMENT_MICHELLE    => true
    case SPComponentType.INSTRUMENT_NIRI        => true
    case SPComponentType.INSTRUMENT_TRECS       => true
    case _                                      => false
  }
}

/** Panel holding the ITC spectroscopy calculation result table and charts. */
class ItcSpectroscopyPanel(val owner: EdIteratorFolder) extends ItcPanel {

  lazy val table = new ItcSpectroscopyTable(ItcParametersProvider(owner, this))

  lazy val display = new SplitPane(Orientation.Horizontal, tableScrollPane, chartsScrollPane) {
    dividerLocation = 150
  }

  private lazy val charts = new ItcChartsPanel(table)
  private lazy val tableScrollPane = new ScrollPane(table) {
    verticalScrollBarPolicy   = AsNeeded
    horizontalScrollBarPolicy = AsNeeded
  }
  private lazy val chartsScrollPane = new ScrollPane(charts) {
    verticalScrollBarPolicy   = AsNeeded
    horizontalScrollBarPolicy = AsNeeded
  }

  /** True for all instruments which support ITC calculations for spectroscopy. */
  def visibleFor(t: SPComponentType): Boolean = t match {
    case SPComponentType.INSTRUMENT_FLAMINGOS2  => true
    case SPComponentType.INSTRUMENT_GMOS        => true
    case SPComponentType.INSTRUMENT_GMOSSOUTH   => true
    case SPComponentType.INSTRUMENT_GNIRS       => true
    case SPComponentType.INSTRUMENT_MICHELLE    => true
    case SPComponentType.INSTRUMENT_NIFS        => true
    case SPComponentType.INSTRUMENT_NIRI        => true
    case SPComponentType.INSTRUMENT_TRECS       => true
    case _                                      => false
  }
}

/** Panel holding some feedback, mainly in case something went wrong with the ITC calculations. */
private class ItcFeedbackPanel(table: ItcTable) extends Label {

  import ItcPanel._
  import OtColor._

  opaque              = true
  foreground          = Color.DARK_GRAY
  border              = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  horizontalAlignment = Alignment.Left

  listenTo(table.selection)
  reactions += {
    case TableRowsSelected(_, _, false)  => update()
  }

  private def update(): Unit = {
    table.selected.flatMap(feedback).fold {
      visible = false                 // no table row selected or no feedback, don't show feedback panel
    } { case (ico, msg, col) =>
      icon        = ico               // feedback available, show it
      text        = msg
      background  = col
      visible     = true
    }
    revalidate()
  }

  private def feedback(f: Future[ItcService.Result]): Option[(Icon, String, Color)] = {
    f.value.fold {
      feedback(SpinnerIcon, "Calculating...", BANANA)
    } {
      case Failure(t)               => feedback(ErrorIcon, t.getMessage, LIGHT_SALMON)
      case Success(s) => s match {
        case scalaz.Failure(errs)   => feedback(ErrorIcon, errs.mkString(", "), LIGHT_SALMON)
        case scalaz.Success(_)      => None
      }
    }
  }

  private def feedback(ico: Icon, msg: String, col: Color): Option[(Icon, String, Color)] = Some((ico, msg, col))

}

/** Panel holding spectroscopy charts.
  * It listens to the results table and updates itself according to the currently selected row. */
private class ItcChartsPanel(table: ItcSpectroscopyTable) extends GridBagPanel {

  private val limitsPanel = new PlotDetailsPanel

  private var charts = Seq[Component]()

  background = Color.WHITE

  listenTo(table.selection, limitsPanel)
  reactions += {
    case TableRowsSelected(_, _, false)  => update()
    case SelectionChanged(`limitsPanel`) => update()
  }

  private def update(): Unit = {
    // remove all current charts and the limits panel
    peer.remove(limitsPanel.peer)
    charts.map(_.peer).foreach(peer.remove)
    // add new ones (if any)
    table.selectedResult().foreach(update)
    // revalidate and repaint everything
    revalidate()
    repaint()
  }

  private def update(result: ItcSpectroscopyResult): Unit = {
    charts = result.charts.map { ds =>
      val chart = ITCChart.forSpcDataSet(ds, limitsPanel.plottingDetails).getChart
      new JFChartComponent(chart)
    }
    layout(limitsPanel) = new Constraints {
      gridx     = 0
      gridy     = 0
      gridwidth = charts.size
      insets    = new Insets(20, 0, 20, 0)
    }
    charts.zipWithIndex.foreach { case (c, x) =>
      layout(c) = new Constraints {
        gridx   = x
        gridy   = 1
        weightx = 1
        weighty = 1
        fill    = Fill.Both
        insets  = new Insets(10, 25, 10, 25)
      }
    }
  }

  // a very simple Scala wrapper for JFreeChart charts
  class JFChartComponent(chart: JFreeChart) extends Component {
    override lazy val peer = new ChartPanel(chart)
    peer.setMaximumDrawHeight(Int.MaxValue)                       // don't limit drawing resolution
    peer.setMaximumDrawWidth(Int.MaxValue)                        // don't limit drawing resolution
    peer.setBackground(Color.white)
  }

}

/** User element that allows to change the conditions taken for the calculations on-the-fly. */
private class ConditionsPanel(owner: EdIteratorFolder) extends GridBagPanel {

  class ConditionCB[A](items: Seq[A], renderFunc: A => String) extends ComboBox[A](items) {
    private var programValue = selection.item
    tooltip  = "Select conditions for ITC calculations. Values different from program conditions are shown in red."
    renderer = new Renderer[A] {
      override def componentFor(list: ListView[_ <: A], isSelected: Boolean, focused: Boolean, a: A, index: Int): Component = {
        new Label(renderFunc(a)) {{ foreground = if (programValue == a) Color.BLACK else Color.RED }}
      }
    }
    listenTo(selection)
    reactions += {
      case SelectionChanged(_) => foreground = color()
    }

    def sync(newValue: A) = {
      if (programValue == selection.item) {
        // if we are "in sync" with program value (i.e. the program value is currently selected), update it
        deafTo(selection)
        selection.item = newValue
        listenTo(selection)
      }
      // set new program value and update coloring
      programValue = newValue
      foreground = color()
    }

    private def color()   = if (inSync()) Color.BLACK else Color.RED

    private def inSync()  = programValue == selection.item

  }

  private val sb = new ConditionCB[SkyBackground]  (SkyBackground.values,                       "SB " + _.displayValue())
  private val cc = new ConditionCB[CloudCover]     (CloudCover.values.filterNot(_.isObsolete),  "CC " + _.displayValue())
  private val iq = new ConditionCB[ImageQuality]   (ImageQuality.values,                        "IQ " + _.displayValue())
  private val wv = new ConditionCB[WaterVapor]     (WaterVapor.values,                          "WV " + _.displayValue())
  private val am = new ConditionCB[Double]         (List(1.0, 1.5, 2.0),                        d => f"Airmass $d%.1f")

  def conditions = new ObservingConditions(
    iq.selection.item,
    cc.selection.item,
    wv.selection.item,
    sb.selection.item,
    am.selection.item)

  def update() = {
    // Note: site quality node can be missing (i.e. null)
    Option(owner.getContextSiteQuality).foreach { qual =>
      sb.sync(qual.getSkyBackground)
      cc.sync(qual.getCloudCover)
      iq.sync(qual.getImageQuality)
      wv.sync(qual.getWaterVapor)
      // TODO: currently the airmass program value is fixed to 1.5 airmass
      // TODO: can we get this value from the airmass constraints?
      am.sync(1.5)
    }
  }

  border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

  layout(new Label("Conditions:"))  = new Constraints {
    gridx   = 0
    gridy   = 0
  }
  layout(sb) = new Constraints {
    gridx   = 1
    gridy   = 0
    insets  = new Insets(0, 10, 0, 0)
  }
  layout(cc) = new Constraints {
    gridx   = 2
    gridy   = 0
    insets  = new Insets(0, 5, 0, 0)
  }
  layout(iq) = new Constraints {
    gridx   = 3
    gridy   = 0
    insets  = new Insets(0, 5, 0, 0)
  }
  layout(wv) = new Constraints {
    gridx   = 4
    gridy   = 0
    insets  = new Insets(0, 5, 0, 0)
  }
  layout(am) = new Constraints {
    gridx   = 5
    gridy   = 0
    insets  = new Insets(0, 5, 0, 0)
  }

  deafTo(this)
  listenTo(sb.selection, cc.selection, iq.selection, wv.selection, am.selection)
  reactions += {
    case SelectionChanged(_) => publish(new SelectionChanged(this))
  }

}
private class AnalysisMethodPanel extends GridBagPanel {

  val autoAperture      = new RadioButton("Auto") { focusable = false; selected = true }
  val userAperture      = new RadioButton("User") { focusable = false }
  val skyApertureLabel  = new Label("Sky Aperture")
  val skyApertureUnits  = new Label("times target")
  val skyAperture       = new NumberEdit(skyApertureLabel, skyApertureUnits, 5)
  val diameterLabel     = new Label("Diameter")
  val diameterUnits     = new Label("arcsec")
  val diameter          = new NumberEdit(diameterLabel, diameterUnits, 2) { enabled = false }
  new ButtonGroup(autoAperture, userAperture)

  layout(new Label("Analysis Method:")) = new Constraints { gridx = 0; gridy = 0; insets = new Insets(0, 0, 0, 20) }
  layout(autoAperture)                  = new Constraints { gridx = 1; gridy = 0; insets = new Insets(0, 0, 0, 20) }
  layout(userAperture)                  = new Constraints { gridx = 1; gridy = 1; insets = new Insets(0, 0, 0, 20) }
  layout(skyApertureLabel)              = new Constraints { gridx = 2; gridy = 0; insets = new Insets(0, 0, 0, 10); fill = Fill.Horizontal }
  layout(skyAperture)                   = new Constraints { gridx = 3; gridy = 0; insets = new Insets(0, 0, 0, 10); fill = Fill.Horizontal }
  layout(skyApertureUnits)              = new Constraints { gridx = 4; gridy = 0; insets = new Insets(0, 5, 0, 0);  fill = Fill.Horizontal }
  layout(diameterLabel)                 = new Constraints { gridx = 2; gridy = 1; insets = new Insets(0, 0, 0, 10); fill = Fill.Horizontal }
  layout(diameter)                      = new Constraints { gridx = 3; gridy = 1; insets = new Insets(0, 0, 0, 10); fill = Fill.Horizontal }
  layout(diameterUnits)                 = new Constraints { gridx = 4; gridy = 1; insets = new Insets(0, 5, 0, 0);  fill = Fill.Horizontal }

  listenTo(autoAperture, userAperture, diameter, skyAperture)
  reactions += {
    case ButtonClicked(`autoAperture`)  => diameter.enabled = false; publish(new SelectionChanged(this))
    case ButtonClicked(`userAperture`)  => diameter.enabled = true;  publish(new SelectionChanged(this))
    case ValueChanged(_)                => publish(new SelectionChanged(this))
  }

  def analysisMethod: Option[AnalysisMethod] =
    if (autoAperture.selected) autoApertureValue else userApertureValue

  private def autoApertureValue: Option[AnalysisMethod] =
    skyAperture.value.map(AutoAperture)

  private def userApertureValue: Option[AnalysisMethod]  =
    for {
      sky <- skyAperture.value
      dia <- diameter.value
    } yield UserAperture(dia, sky)

}

private class PlotDetailsPanel extends GridBagPanel {

  val autoLimits      = new RadioButton("Auto") { focusable = false; background = Color.WHITE; selected = true }
  val userLimits      = new RadioButton("User") { focusable = false; background = Color.WHITE }
  val lowLimitLabel   = new Label("Low")
  val lowLimitUnits   = new Label("nm")
  val lowLimit        = new NumberEdit(lowLimitLabel, lowLimitUnits, 0)       { enabled = false }
  val highLimitLabel  = new Label("High")
  val highLimitUnits  = new Label("nm")
  val highLimit       = new NumberEdit(highLimitLabel, highLimitUnits, 2000)  { enabled = false }
  new ButtonGroup(autoLimits, userLimits)

  background = Color.WHITE
  layout(new Label("Limits:"))  = new Constraints { gridx = 0; gridy = 0; insets = new Insets(0, 0, 0, 20) }
  layout(autoLimits)            = new Constraints { gridx = 1; gridy = 0; insets = new Insets(0, 0, 0, 10) }
  layout(userLimits)            = new Constraints { gridx = 2; gridy = 0; insets = new Insets(0, 0, 0, 20) }
  layout(lowLimitLabel)         = new Constraints { gridx = 3; gridy = 0; insets = new Insets(0, 0, 0, 10) }
  layout(lowLimit)              = new Constraints { gridx = 4; gridy = 0; fill = Fill.Horizontal }
  layout(lowLimitUnits)         = new Constraints { gridx = 5; gridy = 0; insets = new Insets(0, 5, 0, 0) }
  layout(highLimitLabel)        = new Constraints { gridx = 6; gridy = 0; insets = new Insets(0, 20, 0, 10) }
  layout(highLimit)             = new Constraints { gridx = 7; gridy = 0; fill = Fill.Horizontal }
  layout(highLimitUnits)        = new Constraints { gridx = 8; gridy = 0; insets = new Insets(0, 5, 0, 0) }

  listenTo(autoLimits, userLimits, lowLimit, highLimit)
  reactions += {
    case ButtonClicked(`autoLimits`)  => lowLimit.enabled = false; highLimit.enabled = false; publish(new SelectionChanged(this))
    case ButtonClicked(`userLimits`)  => lowLimit.enabled = true;  highLimit.enabled = true;  publish(new SelectionChanged(this))
    case ValueChanged(_)              => publish(new SelectionChanged(this))
  }

  def plottingDetails: PlottingDetails =
    if (autoLimits.selected) PlottingDetails.Auto
    else userPlottingDetails.getOrElse(PlottingDetails.Auto)

  private def userPlottingDetails: Option[PlottingDetails] =
    for {
      low    <- lowLimit.value
      high   <- highLimit.value
      (l, h) <- if (low < high) Some((low, high)) else None
    } yield new PlottingDetails(PlotLimits.USER, l, h)

}

// light weight wrapper to turn NumberBoxWidget into a Scala swing component
private class NumberEdit(label: Label, units: Label, default: Double = 0) extends Component {
  override lazy val peer = new NumberBoxWidget {
    setColumns(8)
    setValue(default)
    setMinimumSize(getPreferredSize)
    addWatcher(new TextBoxWidgetWatcher {
      override def textBoxKeyPress(tbwe: TextBoxWidget): Unit = textBoxAction(tbwe)
      override def textBoxAction(tbwe: TextBoxWidget): Unit =
        try {
          publish(new ValueChanged(NumberEdit.this))
          tbwe.requestFocus()
        } catch {
          case _: NumberFormatException =>
        }
    })
  }

  override def enabled_=(e: Boolean) = {
    label.enabled = e
    peer.setEnabled(e)
    units.enabled = e
  }

  def value: Option[Double] =
    try {
      Some(peer.getValue.toDouble)
    } catch {
      case _: NumberFormatException => None
    }
}


