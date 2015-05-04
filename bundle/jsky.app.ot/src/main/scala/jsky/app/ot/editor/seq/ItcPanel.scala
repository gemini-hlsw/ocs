package jsky.app.ot.editor.seq

import java.awt.Color
import javax.swing.{BorderFactory, ImageIcon}

import edu.gemini.itc.shared.PlottingDetails.PlotLimits
import edu.gemini.itc.shared._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, ImageQuality, SkyBackground, WaterVapor}

import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.ListView.Renderer
import scala.swing.ScrollPane.BarPolicy._
import scala.swing._
import scala.swing.event.{ButtonClicked, KeyEvent, SelectionChanged, TableRowsSelected}
import scalaz.Scalaz._

object ItcPanel {

  /** Creates a panel for ITC imaging results. */
  def forImaging(owner: EdIteratorFolder)       = new ItcImagingPanel(owner)

  /** Creates a panel for ITC spectroscopy results. */
  def forSpectroscopy(owner: EdIteratorFolder)  = new ItcSpectroscopyPanel(owner)

}

/** Input field with a leading label and a tailing units string that turns red if current value is invalid. */
class InputField[A](labelStr: String, unitsStr: String, valueStr: A, convert: String => Option[A], columns: Int = 8)  {
  val label = new Label(labelStr) { horizontalAlignment = Alignment.Left }
  val units = new Label(unitsStr) { horizontalAlignment = Alignment.Left }
  val text  = new TextField(valueStr.toString, columns) {
    horizontalAlignment = Alignment.Right
    listenTo(keys)
    reactions += {
      case e: KeyEvent => foreground = value.fold(Color.RED)(_ => Color.BLACK)
    }
  }

  def keys = text.keys
  def enabled = text.enabled
  def enabled_=(enabled: Boolean) = text.enabled = enabled

  def value: Option[A] = try {
    convert(text.text)
  } catch {
    case _: NumberFormatException => None
  }

}

/** Base trait for different panels which are used to present ITC calculation results to the users. */
sealed trait ItcPanel extends GridBagPanel {
  val owner: EdIteratorFolder
  val table: ItcTable

  protected val currentConditions = new ConditionsPanel
  protected val analysisMethod    = new AnalysisMethodPanel
  protected val sourceDetails     = new SourcePanel

  def visibleFor(t: SPComponentType): Boolean

  def analysis() = analysisMethod.analysisMethod
  def conditions() = currentConditions.conditions
  def spectralDistribution() = sourceDetails.spectralDistribution
  def spatialProfile(mag: Double) = sourceDetails.spatialProfile(mag)

  listenTo(currentConditions, analysisMethod, sourceDetails)
  reactions += {
    case SelectionChanged(`currentConditions`)  => updateInternal()
    case SelectionChanged(`analysisMethod`)     => updateInternal()
    case SelectionChanged(`sourceDetails`)      => updateInternal()
  }

  def update() = {
    currentConditions.update()
    table.update()
  }

  private def updateInternal() = {
// TODO: keep selected table row
//    val selectedRow = table.selection.rows.headOption
    table.update()
    // re-establish previously selected row, this is relevant
    // for charts displayed on spectroscopy panel
//    selectedRow.foreach(r => if (r < table.rowCount) table.selection.rows += r)
  }

  // ==== Source edit TODO: This will migrate to the new source editor once it's ready.

  class SourcePanel extends GridBagPanel {

    /** Details panel with a fixed size and a set of input fields. */
    abstract class DetailsPanel extends GridBagPanel {
      val LabelInsets = new Insets(5, 5, 5, 10)
      val UnitsInsets = new Insets(5, 10, 5, 5)
      val Size = new Dimension(300, 120)
      preferredSize = Size
      minimumSize = Size

      def addField(label: String, value: Double, units: String, row: Int): InputField[Double] =
        addField(label, value, d => Some(d.toDouble), units, row)

      def addField[A](label: String, value: A, convert: String => Option[A], units: String, row: Int): InputField[A] = {
        new InputField[A](label, units, value, convert) <| { f =>
          layout(f.label) = new Constraints { gridx = 0; gridy = row; insets = LabelInsets; weightx = 1; fill = Fill.Horizontal }
          layout(f.text)  = new Constraints { gridx = 1; gridy = row }
          layout(f.units) = new Constraints { gridx = 2; gridy = row; insets = UnitsInsets; anchor = Anchor.West }
        }
      }
    }
    abstract class DistributionDetailsPanel extends DetailsPanel {
      def spectralDistribution: Option[SpectralDistribution]
    }
    abstract class ProfileDetailsPanel extends DetailsPanel {
      def spatialProfile(mag: Double): Option[SpatialProfile]
    }

    private val starDetails = new DistributionDetailsPanel {
      val stars = new ComboBox[LibraryStar](LibraryStar.values) {
        renderer = Renderer(_.sedSpectrum)
      }
      layout(stars) = new Constraints {
        gridx   = 0
        gridy   = 0
        weightx = 1
        fill    = Fill.Horizontal
      }
      def spectralDistribution = Some(stars.selection.item)
    }
    private val nonStarDetails = new DistributionDetailsPanel {
      val nonStars = new ComboBox[LibraryNonStar](LibraryNonStar.values) {
        renderer = Renderer(_.label)
      }
      layout(nonStars) = new Constraints {
        gridx   = 0
        gridy   = 0
        weightx = 1
        fill    = Fill.Horizontal
      }
      def spectralDistribution = Some(nonStars.selection.item)
    }
    private val bbodyDetails = new DistributionDetailsPanel {
      val temperature = addField("Temperature:", 10000.0, "K", 0)
      def spectralDistribution = temperature.value.map(BlackBody)
    }
    private val elineDetails = new DistributionDetailsPanel {
      val wavelength  = addField("Wavelength:", 2.2,      "µm",       0)
      val width       = addField("Width:",      150.0,    "km/s",     1)
      val flux        = addField("Line Flux:",  5.0e-19,  "W/m²",     2)
      val continuum   = addField("Continuum:",  1.0e-16,  "W/m²/µm",  3)
      def spectralDistribution = for {
        wl <- wavelength.value
        wd <- width.value
        fl <- flux.value
        co <- continuum.value
      } yield EmissionLine(wl, wd, fl, "watts_flux", co, "watts_fd_wavelength")
    }
    private val plawDetails = new DistributionDetailsPanel {
      val index = addField("Index:", -1.0, "", 0)
      def spectralDistribution = index.value.map(PowerLaw)
    }

    case class DistributionPanel(label: String, panel: DistributionDetailsPanel)
    private val distributionPanels = List(
      DistributionPanel("Library Star",     starDetails),
      DistributionPanel("Library Non-Star", nonStarDetails),
      DistributionPanel("Black Body",       bbodyDetails),
      DistributionPanel("Emission Line",    elineDetails),
      DistributionPanel("Power Law",        plawDetails)
    )

    private val distributions = new ComboBox[DistributionPanel](distributionPanels) {
      renderer = Renderer(_.label)
      listenTo(selection)
      reactions += {
        case SelectionChanged(_) =>
          distributionPanels.foreach(_.panel.visible = false)
          selection.item.panel.visible = true
      }
    }

    case class ProfilePanel(label: String, panel: ProfileDetailsPanel)
    private val pointSourceDetails = new ProfileDetailsPanel {
      def spatialProfile(mag: Double): Option[SpatialProfile] = Some(PointSource(mag, BrightnessUnit.MAG))
    }
    private val gaussianSourceDetails = new ProfileDetailsPanel {
      val fwhm = addField("FWHM:", 1.0, "arcsec", 0)
      def spatialProfile(mag: Double): Option[SpatialProfile] = fwhm.value.map(GaussianSource(mag, BrightnessUnit.MAG, _))
    }
    private val uniformSourceDetails = new ProfileDetailsPanel {
      def spatialProfile(mag: Double): Option[SpatialProfile] = Some(UniformSource(mag, BrightnessUnit.MAG))
    }
    private val profilePanels = List(
      ProfilePanel("Point Source",             pointSourceDetails),
      ProfilePanel("Extended Gaussian Source", gaussianSourceDetails),
      ProfilePanel("Extended Uniform Source",  uniformSourceDetails)
    )

    private val profiles = new ComboBox[ProfilePanel](profilePanels) {
      renderer = Renderer(_.label)
      listenTo(selection)
      reactions += {
        case SelectionChanged(_) =>
          profilePanels.foreach(_.panel.visible = false)
          selection.item.panel.visible = true
      }
    }

    // magnitude is known by source editor, we need to pass in the value from there
    def spatialProfile(mag: Double) = profiles.selection.item.panel.spatialProfile(mag)

    def spectralDistribution = distributions.selection.item.panel.spectralDistribution

    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

    layout(profiles) = new Constraints {
      gridx   = 0
      gridy   = 0
      weightx = 0.5
      fill    = Fill.Horizontal
    }
    layout(distributions) = new Constraints {
      gridx   = 1
      gridy   = 0
      weightx = 0.5
      fill    = Fill.Horizontal
    }
    // add all distribution panels in the same grid cell,
    // the individual panels will be made visible/invisible as needed
    profilePanels.foreach { p =>
      layout(p.panel) = new Constraints {
        gridx = 0
        gridy = 1
      }
      p.panel.visible = false
    }
    profiles.selection.item.panel.visible = true

    distributionPanels.foreach { p =>
      layout(p.panel) = new Constraints {
        gridx = 1
        gridy = 1
      }
      p.panel.visible = false
    }
    distributions.selection.item.panel.visible = true


    // reactions
    deafTo(this)
    listenTo(distributions.selection, profiles.selection)
    reactions += {
      case SelectionChanged(_) => publish(SelectionChanged(this))
    }

  }

  // ==== Conditions display and edit panels

  class ConditionsPanel extends GridBagPanel {

    private class ConditionCB[A](items: Seq[A], renderFunc: A => String) extends ComboBox[A](items) {
      private var programValue = selection.item
      tooltip  = "Select conditions for ITC calculations. Values different from program conditions are shown in red."
      renderer = Renderer(renderFunc)
      listenTo(selection)
      reactions += {
        case SelectionChanged(_) =>
          foreground = color()
          tooltip    = tt()
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
        tooltip    = tt()
      }

      private def color() = if (inSync()) Color.BLACK else Color.RED

      private def tt() = if (inSync()) "" else s"Program condition is ${renderFunc(programValue)}"

      private def inSync() = programValue == selection.item

    }

    private val t  = new Label("Conditions:")
    private val sb = new ConditionCB[SkyBackground]  (SkyBackground.values,                       _.displayValue())
    private val cc = new ConditionCB[CloudCover]     (CloudCover.values.filterNot(_.isObsolete),  _.displayValue())
    private val iq = new ConditionCB[ImageQuality]   (ImageQuality.values,                        _.displayValue())
    private val wv = new ConditionCB[WaterVapor]     (WaterVapor.values,                          _.displayValue())
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
        // TODO: currently the airmass program value is fixed to 1.5am; get this value from constraints?
        am.sync(1.5)
        // TODO: update necessary??
      }
    }

    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

    layout(t)  = new Constraints {
      gridx   = 0
      gridy   = 0
    }
    layout(sb) = new Constraints {
      gridx   = 1
      gridy   = 0
    }
    layout(cc) = new Constraints {
      gridx   = 2
      gridy   = 0
    }
    layout(iq) = new Constraints {
      gridx   = 3
      gridy   = 0
    }
    layout(wv) = new Constraints {
      gridx   = 4
      gridy   = 0
    }
    layout(am) = new Constraints {
      gridx   = 5
      gridy   = 0
    }

    deafTo(this)
    listenTo(sb.selection, cc.selection, iq.selection, wv.selection, am.selection)
    reactions += {
      case SelectionChanged(_) => publish(new SelectionChanged(this))
    }

  }

}

/** Panel holding the ITC imaging calculation result table. */
class ItcImagingPanel(val owner: EdIteratorFolder) extends ItcPanel {

  val table = new ItcImagingTable(ItcParametersProvider(owner, this))

  border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

  private val scrollPane = new ScrollPane(table) {
    verticalScrollBarPolicy = AsNeeded
    horizontalScrollBarPolicy = AsNeeded
  }

  layout(currentConditions) = new Constraints {
    gridx       = 0
    gridy       = 0
  }
  layout(analysisMethod) = new Constraints {
    gridx       = 0
    gridy       = 1
  }
  layout(sourceDetails) = new Constraints {
    gridx       = 1
    gridy       = 0
    gridheight  = 2
  }
  layout(scrollPane) = new Constraints {
    gridx       = 0
    gridy       = 2
    weightx     = 1
    weighty     = 1
    gridwidth   = 2
    fill        = Fill.Both
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

  val table = new ItcSpectroscopyTable(ItcParametersProvider(owner, this))
  private val charts = new ItcChartsPanel(table)

  border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

  private val tableScrollPane = new ScrollPane(table) {
    preferredSize             = new Dimension(100, 200)
    verticalScrollBarPolicy   = AsNeeded
    horizontalScrollBarPolicy = AsNeeded
  }
  private val chartsScrollPane = new ScrollPane(charts) {
    preferredSize             = new Dimension(100, 400)
    verticalScrollBarPolicy   = AsNeeded
    horizontalScrollBarPolicy = AsNeeded
  }
  private val splitPane = new SplitPane(Orientation.Horizontal, tableScrollPane, chartsScrollPane)

  layout(currentConditions) = new Constraints {
    gridx     = 0
    gridy     = 0
  }
  layout(analysisMethod) = new Constraints {
    gridx     = 0
    gridy     = 1
  }
  layout(sourceDetails) = new Constraints {
    gridx     = 1
    gridy     = 0
    gridheight= 2
  }
  layout(splitPane) = new Constraints {
    gridx     = 0
    gridy     = 2
    weightx   = 1
    weighty   = 1
    gridwidth = 2
    fill      = GridBagPanel.Fill.Both
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
      val chart = ITCChart.forSpcDataSet(ds, limitsPanel.plottingDetails).getBufferedImage(600, 400)
      new Label("", new ImageIcon(chart), Alignment.Center) {
        border = BorderFactory.createEmptyBorder(10, 25, 10, 25)
      }
    }
    layout(limitsPanel) = new Constraints {
      gridx     = 0
      gridy     = 0
      gridwidth = charts.size
      insets    = new Insets(20, 0, 20, 0)
    }
    charts.zipWithIndex.foreach { case (l, x) =>
      layout(l) = new Constraints {
        gridx = x
        gridy = 1
      }
    }
  }
}

protected class AnalysisMethodPanel extends GridBagPanel {

  val autoAperture = new RadioButton("Auto") { focusable = false; selected = true }
  val userAperture = new RadioButton("User") { focusable = false }
  val diameter     = new InputField[Double]("Diameter ",      " arcsec",          0, d => Some(d.toDouble)) { enabled = false }
  val skyAperture  = new InputField[Double]("Sky Aperture ",  " times target",    5, d => Some(d.toDouble))
  new ButtonGroup(autoAperture, userAperture)

  layout(new Label("Analysis Method:")) = new Constraints { gridx = 0; gridy = 0; insets = new Insets(0, 0, 0, 20) }
  layout(autoAperture)                  = new Constraints { gridx = 1; gridy = 0; insets = new Insets(0, 0, 0, 20) }
  layout(userAperture)                  = new Constraints { gridx = 1; gridy = 1; insets = new Insets(0, 0, 0, 20) }
  layout(skyAperture.label)             = new Constraints { gridx = 2; gridy = 0; insets = new Insets(0, 0, 0, 10) }
  layout(skyAperture.text)              = new Constraints { gridx = 3; gridy = 0; insets = new Insets(0, 0, 0, 10) }
  layout(skyAperture.units)             = new Constraints { gridx = 4; gridy = 0 }
  layout(diameter.label)                = new Constraints { gridx = 2; gridy = 1; insets = new Insets(0, 0, 0, 10) }
  layout(diameter.text)                 = new Constraints { gridx = 3; gridy = 1; insets = new Insets(0, 0, 0, 10) }
  layout(diameter.units)                = new Constraints { gridx = 4; gridy = 1 }

  listenTo(autoAperture, userAperture, diameter.keys, skyAperture.keys)
  reactions += {
    case ButtonClicked(`autoAperture`)  => diameter.enabled = false; publish(new SelectionChanged(this))
    case ButtonClicked(`userAperture`)  => diameter.enabled = true;  publish(new SelectionChanged(this))
    case e: KeyEvent                    => publish(new SelectionChanged(this))
  }

  def analysisMethod: AnalysisMethod =
    if (autoAperture.selected) autoApertureValue.getOrElse(default)
    else userApertureValue.getOrElse(default)

  private val default = AutoAperture(5.0)

  private def autoApertureValue: Option[AnalysisMethod] =
    skyAperture.value.map(AutoAperture)

  private def userApertureValue: Option[AnalysisMethod]  =
    for {
      sky <- skyAperture.value
      dia <- diameter.value
    } yield UserAperture(dia, sky)

}

protected class PlotDetailsPanel extends GridBagPanel {

  val autoLimits = new RadioButton("Auto") { focusable = false; background = Color.WHITE; selected = true }
  val userLimits = new RadioButton("User") { focusable = false; background = Color.WHITE }
  val lowLimit   = new InputField[Double]("Low ",  " nm",    0, d => Some(d.toDouble)) { enabled = false }
  val highLimit  = new InputField[Double]("High ", " nm", 2000, d => Some(d.toDouble)) { enabled = false }
  new ButtonGroup(autoLimits, userLimits)

  background = Color.WHITE
  layout(new Label("Limits:"))  = new Constraints { gridx = 0; gridy = 0; insets = new Insets(0, 0, 0, 20) }
  layout(autoLimits)            = new Constraints { gridx = 1; gridy = 0; insets = new Insets(0, 0, 0, 20) }
  layout(userLimits)            = new Constraints { gridx = 2; gridy = 0; insets = new Insets(0, 0, 0, 20) }
  layout(lowLimit.label)        = new Constraints { gridx = 3; gridy = 0 }
  layout(lowLimit.text)         = new Constraints { gridx = 4; gridy = 0 }
  layout(lowLimit.units)        = new Constraints { gridx = 5; gridy = 0; insets = new Insets(0, 0, 0, 20) }
  layout(highLimit.label)       = new Constraints { gridx = 6; gridy = 0 }
  layout(highLimit.text)        = new Constraints { gridx = 7; gridy = 0 }
  layout(highLimit.units)       = new Constraints { gridx = 8; gridy = 0 }

  listenTo(autoLimits, userLimits, lowLimit.keys, highLimit.keys)
  reactions += {
    case ButtonClicked(`autoLimits`)    => lowLimit.enabled = false; highLimit.enabled = false; publish(new SelectionChanged(this))
    case ButtonClicked(`userLimits`)    => lowLimit.enabled = true;  highLimit.enabled = true;  publish(new SelectionChanged(this))
    case e: KeyEvent                    => publish(new SelectionChanged(this))
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

