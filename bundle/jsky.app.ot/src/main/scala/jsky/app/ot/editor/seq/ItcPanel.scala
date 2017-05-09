package jsky.app.ot.editor.seq

import java.awt.Color
import javax.swing._

import com.jgoodies.forms.factories.DefaultComponentFactory
import edu.gemini.itc.shared._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.core.Target
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.{InstGmosNorth, InstGmosSouth}
import edu.gemini.spModel.gemini.gnirs.InstGNIRS
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.nifs.InstNIFS
import edu.gemini.spModel.gemini.niri.InstNIRI
import jsky.app.ot.itc._
import jsky.app.ot.util.OtColor
import org.jfree.chart.{ChartPanel, JFreeChart}

import scala.concurrent.Future
import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.ScrollPane.BarPolicy._
import scala.swing._
import scala.swing.event._
import scala.util.{Failure, Success}
import scalaz._

object ItcPanel {

  val ErrorIcon   = new ImageIcon(classOf[ItcPanel].getResource("/resources/images/error_tsk.gif"))
  val WarningIcon = new ImageIcon(classOf[ItcPanel].getResource("/resources/images/warn_tsk.gif"))
  val SpinnerIcon = new ImageIcon(classOf[ItcPanel].getResource("/resources/images/spinner16.gif"))

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

  /** TODO:ASTERISM: we need to display a list of asterism members and let the user select one. For now we
    * just select the first target in the asterism.
    */
  def selectedTarget: Option[Target] =
    Option(owner.getContextTargetEnv).map(_.getArbitraryTargetFromAsterism.getTarget)

  private val conditionsPanel        = new ConditionsPanel(owner)
  private val aperturePanel          = new AnalysisApertureMethodPanel(owner)
  private val apertureFixedSkyPanel  = new AnalysisApertureMethodPanel(owner, fixedSkyValue = true)
  private val ifuNifsPanel           = new AnalysisIfuMethodPanel(owner, skyEditable = false)
  private val ifuGmosPanel           = new AnalysisIfuMethodPanel(owner, summedAllowed = false)
  private val messagePanel           = new ItcFeedbackPanel(table)

  private var analysisMethod: AnalysisMethodPanel = aperturePanel

  border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
  layout(separator("Conditions:")) = new Constraints {
    anchor    = Anchor.West
    gridx     = 0
    gridy     = 0
    weightx   = 1
    fill      = Fill.Horizontal
    insets    = new Insets(10, 0, 0, 0)
  }
  layout(conditionsPanel) = new Constraints {
    anchor    = Anchor.West
    gridx     = 0
    gridy     = 1
    insets    = new Insets(5, 20, 0, 0)
  }
  layout(separator("Analysis Method:")) = new Constraints {
    anchor    = Anchor.West
    gridx     = 0
    gridy     = 2
    weightx   = 1
    fill      = Fill.Horizontal
    insets    = new Insets(10, 0, 0, 0)
  }
  layout(display) = new Constraints {
    gridx     = 0
    gridy     = 4
    weightx   = 1
    weighty   = 1
    gridwidth = 2
    fill      = Fill.Both
    insets    = new Insets(10, 0, 0, 0)
  }
  layout(messagePanel) = new Constraints {
    anchor    = Anchor.West
    gridx     = 0
    gridy     = 5
    gridwidth = 2
    weightx   = 1
    fill      = Fill.Horizontal
    insets    = new Insets(10, 0, 0, 0)
  }

  listenTo(conditionsPanel, analysisMethod)
  reactions += {
    case SelectionChanged(_) => table.update()
  }

  def update(): Unit = {
    deafTo(conditionsPanel, analysisMethod)
    updateAnalysisPanel()
    conditionsPanel.update()
    analysisMethod.update()
    table.update()
    listenTo(conditionsPanel, analysisMethod)
  }

  def analysis: Option[AnalysisMethod] = analysisMethod.analysisMethod

  def conditions: ObservingConditions = conditionsPanel.conditions

  private def updateAnalysisPanel() = {
    peer.remove(analysisMethod.peer)
    analysisMethod = owner.getContextInstrumentDataObject match {
      case _: InstNIFS                            => ifuNifsPanel          // NIFS ifu method
      case i: InstGmosNorth if i.getFPUnit.isIFU  => ifuGmosPanel          // GMOS ifu method (without summed)
      case i: InstGmosSouth if i.getFPUnit.isIFU  => ifuGmosPanel
      case _: Flamingos2                          => apertureFixedSkyPanel // for IR instruments always use sky aperture = 1.0
      case _: InstGNIRS                           => apertureFixedSkyPanel
      case _: Gsaoi                               => apertureFixedSkyPanel
      case _: InstNIRI                            => apertureFixedSkyPanel
      case _                                      => aperturePanel         // as default use aperture method
    }
    layout(analysisMethod) = new Constraints {
      anchor    = Anchor.West
      gridx     = 0
      gridy     = 3
      insets    = new Insets(5, 20, 5, 0)
    }
    revalidate()
  }

  private def separator(label: String) = new Component {
    override lazy val peer = DefaultComponentFactory.getInstance.createSeparator(label)
  }

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
    case SPComponentType.INSTRUMENT_MICHELLE    => false // may or may not be supported in OT at some point
    case SPComponentType.INSTRUMENT_NIFS        => false
    case SPComponentType.INSTRUMENT_NIRI        => true
    case SPComponentType.INSTRUMENT_TRECS       => false // may or may not be supported in OT at some point
    case SPComponentType.INSTRUMENT_GNIRS       => true
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
    case SPComponentType.INSTRUMENT_MICHELLE    => false // may or may not be supported in OT at some point
    case SPComponentType.INSTRUMENT_NIFS        => true
    case SPComponentType.INSTRUMENT_NIRI        => true
    case SPComponentType.INSTRUMENT_TRECS       => false // may or may not be supported in OT at some point
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

  private def feedback(f: Future[ItcService.Result]): Option[(Icon, String, Color)] =
    f.value.fold {
      feedback(SpinnerIcon, "Calculating...", BANANA)
    } {
      case Failure(t)                           => feedback(ErrorIcon,    t.getMessage, LIGHT_SALMON)
      case Success(s) => s match {
        case -\/(err)                           => feedback(ErrorIcon,    err.msg,      LIGHT_SALMON)
        case \/-(res) if res.warnings.nonEmpty  => feedback(WarningIcon,  res.warnings.map(_.msg).mkString("<html><body>","<br>","</body></html>"), BANANA)
        case _                                  => None
      }
    }

  private def feedback(ico: Icon, msg: String, col: Color): Option[(Icon, String, Color)] = Some((ico, msg, col))

}

/** Panel holding spectroscopy charts.
  * It listens to the results table and updates itself according to the currently selected row. */
private class ItcChartsPanel(table: ItcSpectroscopyTable) extends GridBagPanel {

  private val limitsPanel = new PlotDetailsPanel

  background = Color.WHITE

  listenTo(table.selection, limitsPanel)
  reactions += {
    case TableRowsSelected(_, _, false)  => update()
    case SelectionChanged(`limitsPanel`) => update()
  }

  private def update(): Unit = {
    // remove all current charts and the limits panel
    peer.getComponents.foreach(peer.remove)
    // add new ones (if any)
    table.selectedResult().foreach(update)
    // revalidate and repaint everything
    revalidate()
    repaint()
  }

  private def update(result: ItcSpectroscopyResult): Unit = {

    // get the count of the largest chart group and use it as the width
    val width = result.chartGroups.map(_.charts.length).max
    layout(limitsPanel) = new Constraints {
      gridx     = 0
      gridy     = 0
      gridwidth = width
      insets    = new Insets(20, 0, 20, 0)
    }

    // iterate on chart groups and their charts and add them
    result.chartGroups.zipWithIndex.foreach { case (g, y) =>
      g.charts.zipWithIndex.foreach { case (c, x) =>
        val chart = ITCChart.forSpcDataSet(c, limitsPanel.plottingDetails).getChart
        layout(new JFChartComponent(chart)) = new Constraints {
          gridx   = x
          gridy   = y + 1 // 0 is used by limits panel
          weightx = 1
          weighty = 1
          fill    = Fill.Both
          insets  = new Insets(10, 25, 10, 25)
        }
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
