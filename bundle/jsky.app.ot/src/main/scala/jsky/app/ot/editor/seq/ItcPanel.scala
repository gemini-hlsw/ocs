package jsky.app.ot.editor.seq

import java.awt.Color
import java.text.ParseException
import javax.swing.BorderFactory

import edu.gemini.itc.shared._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, ImageQuality, SkyBackground, WaterVapor}

import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.ListView.Renderer
import scala.swing.ScrollPane.BarPolicy._
import scala.swing._
import scala.swing.event.SelectionChanged

object ItcPanel {

  /** Creates a panel for ITC imaging results. */
  def forImaging(owner: EdIteratorFolder)       = new ItcImagingPanel(owner, new ItcImagingTable(owner))

  /** Creates a panel for ITC spectroscopy results. */
  def forSpectroscopy(owner: EdIteratorFolder)  = new ItcSpectroscopyPanel(owner, new ItcSpectroscopyTable(owner))

}

/** Base trait for different panels which are used to present ITC calculation results to the users. */
sealed trait ItcPanel extends GridBagPanel {
  val owner: EdIteratorFolder
  val table: ItcTable

  protected val currentConditions = new ConditionsPanel
  protected val sourceDetails     = new SourcePanel

  def visibleFor(t: SPComponentType): Boolean

  def update() = {
    currentConditions.update()
    table.update(currentConditions.conditions)
  }

  // ==== Source edit TODO: This will migrate to the new source editor once it's ready.

  class SourcePanel extends GridBagPanel {

    abstract class DetailsPanel extends GridBagPanel {
      val Insets    = new Insets(5, 5, 5, 10)
      val Size      = new Dimension(300, 120)
      preferredSize = Size
      minimumSize   = Size
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
      val temperature = new TextField(10)
      layout(new Label("Temperature [K]:")) = new Constraints { gridx = 0; gridy = 0; insets = Insets; weightx = 1; fill = Fill.Horizontal }
      layout(temperature)                   = new Constraints { gridx = 1; gridy = 0 }
      def spectralDistribution = try { Some(BlackBody(temperature.text.toDouble)) } catch { case _: ParseException => None }
    }
    private val elineDetails = new DistributionDetailsPanel {
      val wavelength = new TextField(10)
      val width      = new TextField(10)
      val flux       = new TextField(10)
      val continuum  = new TextField(10)
      layout(new Label("Wavelength:"))      = new Constraints { gridx = 0; gridy = 0; insets = Insets; weightx = 1; fill = Fill.Horizontal}
      layout(wavelength)                    = new Constraints { gridx = 1; gridy = 0 }
      layout(new Label("Width:"))           = new Constraints { gridx = 0; gridy = 1; insets = Insets }
      layout(width)                         = new Constraints { gridx = 1; gridy = 1 }
      layout(new Label("Flux:"))            = new Constraints { gridx = 0; gridy = 2; insets = Insets }
      layout(flux)                          = new Constraints { gridx = 1; gridy = 2 }
      layout(new Label("Continuum:"))       = new Constraints { gridx = 0; gridy = 3; insets = Insets }
      layout(continuum)                     = new Constraints { gridx = 1; gridy = 3 }
      def spectralDistribution = try {
        Some(EmissionLine(
          wavelength.text.toDouble,
          width.text.toDouble,
          flux.text.toDouble,
          "watts_flux",
          continuum.text.toDouble,
          "watts_fd_wavelength"
        ))
      } catch { case _: ParseException => None }
    }
    private val plawDetails = new DistributionDetailsPanel {
      val index = new TextField(10)
      layout(new Label("Index:"))     = new Constraints { gridx = 0; gridy = 0; insets = Insets; weightx = 1; fill = Fill.Horizontal }
      layout(index)                   = new Constraints { gridx = 1; gridy = 0 }
      def spectralDistribution = try { Some(PowerLaw(index.text.toDouble)) } catch { case _: ParseException => None }
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
      override def spatialProfile(mag: Double): Option[SpatialProfile] = Some(PointSource(mag, BrightnessUnit.MAG))
    }
    private val gaussianSourceDetails = new ProfileDetailsPanel {
      override def spatialProfile(mag: Double): Option[SpatialProfile] = Some(GaussianSource(mag, BrightnessUnit.MAG, fwhm = 1.0))
    }
    private val uniformSourceDetails = new ProfileDetailsPanel {
      override def spatialProfile(mag: Double): Option[SpatialProfile] = Some(UniformSource(mag, BrightnessUnit.MAG))
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

    listenTo(sb.selection, cc.selection, iq.selection, wv.selection, am.selection)
    reactions += {
      case SelectionChanged(_) => table.update(currentConditions.conditions)
    }

  }

}

/** Panel holding the ITC imaging calculation result table. */
class ItcImagingPanel(val owner: EdIteratorFolder, val table: ItcImagingTable) extends ItcPanel {

  border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

  private val scrollPane = new ScrollPane(table) {
    verticalScrollBarPolicy = AsNeeded
    horizontalScrollBarPolicy = AsNeeded
  }

  layout(currentConditions) = new Constraints {
    gridx = 0
    gridy = 0
  }
  layout(sourceDetails) = new Constraints {
    gridx = 1
    gridy = 0
  }
  layout(scrollPane) = new Constraints {
    gridx = 0
    gridy = 1
    weightx = 1
    weighty = 1
    gridwidth = 2
    fill = GridBagPanel.Fill.Both
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
class ItcSpectroscopyPanel(val owner: EdIteratorFolder, val table: ItcSpectroscopyTable) extends ItcPanel {

  border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

  private val scrollPane = new ScrollPane(table) {
    verticalScrollBarPolicy   = AsNeeded
    horizontalScrollBarPolicy = AsNeeded
  }

  private val charts = new ItcChartsPanel()

  layout(scrollPane) = new Constraints {
    gridx   = 0
    gridy   = 0
    weightx = 1
    weighty = 0.5
    fill    = GridBagPanel.Fill.Both
  }
  layout(charts) = new Constraints {
    gridx   = 0
    gridy   = 1
    weightx = 1
    weighty = 0.5
    fill    = GridBagPanel.Fill.Both
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

/** Panel holding spectroscopy charts. */
private class ItcChartsPanel extends GridBagPanel {

  // TODO...
  private val label = new Label("Spectroscopy charts will go here..")

  layout(label) = new Constraints {
    gridx   = 0
    gridy   = 0
    weightx = 1
    weighty = 1
    fill    = GridBagPanel.Fill.Both
  }
}

