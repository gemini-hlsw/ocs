package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{Component, GridBagConstraints, Insets}
import javax.swing.JPanel

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.EmissionLine.Continuum
import edu.gemini.spModel.target._
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.app.ot.gemini.editor.targetComponent.details.NumericPropertySheet.Prop

import edu.gemini.spModel.core.WavelengthConversions._
import squants.radio.IrradianceConversions._
import squants.motion.VelocityConversions._

import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.ListView.Renderer
import scala.swing.event.SelectionChanged
import scala.swing.{ComboBox, GridBagPanel, Label, Swing}
import scalaz.Scalaz._


final class SourceDetailsEditor extends GridBagPanel with TelescopePosEditor {

  // ==== The Target

  private[this] var spt: SPTarget = new SPTarget

  private def setDistribution(sd: SpectralDistribution): Unit         = setDistribution(Some(sd))
  private def setDistribution(sd: Option[SpectralDistribution]): Unit = spt.setSpectralDistribution(sd)
  private def setProfile     (sp: SpatialProfile): Unit               = setProfile(Some(sp))
  private def setProfile     (sp: Option[SpatialProfile]): Unit       = spt.setSpatialProfile(sp)

  // ==== Spatial Profile Details

  private val defaultPointSource    = PointSource()
  private val defaultUniformSource  = UniformSource()
  private val defaultGaussianSource = GaussianSource(0.5)

  private def gaussianOrDefault(t: SPTarget): GaussianSource = t.getTarget.getSpatialProfile.fold(defaultGaussianSource)(_.asInstanceOf[GaussianSource])

  private val pointSourceDetails    = new JPanel()
  private val uniformSourceDetails  = new JPanel()
  private val gaussianSourceDetails = NumericPropertySheet[GaussianSource](None, t => gaussianOrDefault(t),
    Prop("with FWHM",  "arcsec", _.fwhm, (a, v) => setProfile(GaussianSource(v)))
  )

  private case class ProfilePanel(label: String, panel: Component, default: Option[SpatialProfile])
  private val profilePanels = List(
    ProfilePanel("«undefined»",              new JPanel(),          None),
    ProfilePanel("Point Source",             pointSourceDetails,    Some(defaultPointSource)),
    ProfilePanel("Extended Gaussian Source", gaussianSourceDetails, Some(defaultGaussianSource)),
    ProfilePanel("Extended Uniform Source",  uniformSourceDetails,  Some(defaultUniformSource))
  )

  private val profiles = new ComboBox[ProfilePanel](profilePanels) {
    renderer = Renderer(_.label)
  }

  // === Spectral Distribution Details

  private val defaultBlackBody    = BlackBody(10000)
  private val defaultEmissionLine = EmissionLine(2.2.microns, 500.kps, 5.0e-19.wattsPerSquareMeter, Continuum.fromWatts(1.0e-16))
  private val defaultPowerLaw     = PowerLaw(1)

  private def blackBodyOrDefault   (t: SPTarget): BlackBody     = t.getTarget.getSpectralDistribution.fold(defaultBlackBody)(_.asInstanceOf[BlackBody])
  private def emissionLineOrDefault(t: SPTarget): EmissionLine  = t.getTarget.getSpectralDistribution.fold(defaultEmissionLine)(_.asInstanceOf[EmissionLine])
  private def powerLawOrDefault    (t: SPTarget): PowerLaw      = t.getTarget.getSpectralDistribution.fold(defaultPowerLaw)(_.asInstanceOf[PowerLaw])

  private val libraryStarDetails     = new ComboBox[LibraryStar](LibraryStar.Values) {
    renderer = Renderer(_.sedSpectrum)
  }
  private val libraryNonStarDetails  = new ComboBox[LibraryNonStar](LibraryNonStar.values) {
    renderer = Renderer(_.label)
  }
  private val blackBodyDetails = NumericPropertySheet[BlackBody](None, blackBodyOrDefault,
    Prop("Temperature", "Kelvin",   _.temperature,                (a, v) => setDistribution(BlackBody(v)))
  )
  private val emissionLineDetails = NumericPropertySheet[EmissionLine](None, emissionLineOrDefault,
    Prop("Wavelength",  "µm",       _.wavelength.toMicrons,       (a, v) => setDistribution(EmissionLine(v.microns,    a.width, a.flux,                a.continuum))),
    Prop("Width",       "km/sec",   _.width.toKilometersPerSecond,(a, v) => setDistribution(EmissionLine(a.wavelength, v.kps,   a.flux,                a.continuum))),
    Prop("Flux",        "W/m²",     _.flux.toWattsPerSquareMeter, (a, v) => setDistribution(EmissionLine(a.wavelength, a.width, v.wattsPerSquareMeter, a.continuum))),
    Prop("Continuum",   "W/m²/µm",  _.continuum.toWatts,          (a, v) => setDistribution(EmissionLine(a.wavelength, a.width, a.flux,                Continuum.fromWatts(v))))
  )
  private val powerLawDetails = NumericPropertySheet[PowerLaw](None, powerLawOrDefault,
    Prop("Index",       "",         _.index,                      (a, v) => setDistribution(PowerLaw(v)))
  )

  private case class DistributionPanel(label: String, panel: Component, default: Option[SpectralDistribution])
  private val distributionPanels = List(
    DistributionPanel("«undefined»",      new JPanel(),               None),
    DistributionPanel("Library Star",     libraryStarDetails.peer,    Some(libraryStarDetails.selection.item)),
    DistributionPanel("Library Non-Star", libraryNonStarDetails.peer, Some(libraryNonStarDetails.selection.item)),
    DistributionPanel("Black Body",       blackBodyDetails,           Some(defaultBlackBody)),
    DistributionPanel("Emission Line",    emissionLineDetails,        Some(defaultEmissionLine)),
    DistributionPanel("Power Law",        powerLawDetails,            Some(defaultPowerLaw))
  )

  private val distributions = new ComboBox[DistributionPanel](distributionPanels) {
    renderer = Renderer(_.label)
  }

  // ==== The Layout

  border = titleBorder("Source")

  layout(new Label("Spatial Profile")) = new Constraints {
    anchor  = Anchor.West
    gridx   = 0
    gridy   = 0
    insets = new Insets(0, 2, 2, 2)
  }

  layout(profiles) = new Constraints {
    gridx   = 0
    gridy   = 1
    weightx = 1
    fill    = Fill.Horizontal
    insets  = new Insets(0, 2, 10, 2)
  }

  // add all profile panels in the same grid cell,
  // the individual panels will be made visible/invisible as needed
  profilePanels.foreach { p =>
    peer.add(p.panel, new GridBagConstraints <| { c =>
      c.anchor  = GridBagConstraints.WEST
      c.gridx   = 0
      c.gridy   = 2
      c.insets  = new Insets(0, 5, 0, 2)
    })
  }

  layout(new Label("Spectral Distribution")) = new Constraints {
    anchor  = Anchor.West
    gridx   = 0
    gridy   = 3
    insets = new Insets(20, 2, 2, 2)
  }

  layout(distributions) = new Constraints {
    gridx   = 0
    gridy   = 4
    weightx = 1
    fill    = Fill.Horizontal
    insets  = new Insets(0, 2, 10, 2)
  }

  // add all distribution panels in the same grid cell,
  // the individual panels will be made visible/invisible as needed
  distributionPanels.foreach { p =>
    peer.add(p.panel, new GridBagConstraints <| { c =>
      c.anchor  = GridBagConstraints.WEST
      c.gridx   = 0
      c.gridy   = 5
      c.insets  = new Insets(0, 2, 0, 2)
    })
    p.panel.setVisible(false)
  }

  // occupy remaining vertical space
  layout(Swing.VGlue) = new Constraints {
    gridx   = 0
    gridy   = 6
    weighty = 1
    fill    = Fill.Vertical
  }

  // ==== hide all the panels to start with
  profilePanels.foreach(_.panel.setVisible(false))
  distributionPanels.foreach(_.panel.setVisible(false))

  // ==== Listeners and Reactions

  private val editElements = List(
    profiles.selection,
    distributions.selection,
    libraryStarDetails.selection,
    libraryNonStarDetails.selection
  )

  listenTo(editElements:_*)
  reactions += {

    case SelectionChanged(`distributions`) =>
      setDistribution(distributions.selection.item.default)

    case SelectionChanged(`libraryStarDetails`) =>
      setDistribution(libraryStarDetails.selection.item)

    case SelectionChanged(`libraryNonStarDetails`) =>
      setDistribution(libraryNonStarDetails.selection.item)

    case SelectionChanged(`profiles`)  =>
      setProfile(profiles.selection.item.default)

  }

  // react to any kind of target change by updating all UI elements
  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {

    spt = spTarget

    // we only show the source editor for the base/science target, and we also only need to update it if visible
    visible = if (obsContext.isDefined) obsContext.getValue.getTargets.getBase == spTarget else false
    if (visible) {

      deafTo(editElements:_*)

      spt.getTarget.getSpatialProfile match {
        case None                     => profiles.selection.item = profilePanels.head
        case Some(s: PointSource)     => profiles.selection.item = profilePanels(1)
        case Some(s: GaussianSource)  => profiles.selection.item = profilePanels(2); profilePanels(2).panel.asInstanceOf[NumericPropertySheet[GaussianSource]].edit(obsContext, spTarget, node)
        case Some(s: UniformSource)   => profiles.selection.item = profilePanels(3)
      }

      spt.getTarget.getSpectralDistribution match {
        case None                     => distributions.selection.item = distributionPanels.head
        case Some(s: LibraryStar)     => distributions.selection.item = distributionPanels(1); libraryStarDetails.selection.item = s
        case Some(s: LibraryNonStar)  => distributions.selection.item = distributionPanels(2); libraryNonStarDetails.selection.item = s
        case Some(s: BlackBody)       => distributions.selection.item = distributionPanels(3); distributionPanels(3).panel.asInstanceOf[NumericPropertySheet[BlackBody]].edit(obsContext, spTarget, node)
        case Some(s: EmissionLine)    => distributions.selection.item = distributionPanels(4); distributionPanels(4).panel.asInstanceOf[NumericPropertySheet[EmissionLine]].edit(obsContext, spTarget, node)
        case Some(s: PowerLaw)        => distributions.selection.item = distributionPanels(5); distributionPanels(5).panel.asInstanceOf[NumericPropertySheet[PowerLaw]].edit(obsContext, spTarget, node)
        case Some(s: UserDefined)     => throw new Error("not yet supported") // at a later stage we will add support for aux file user spectras
      }

      updateUI()

      listenTo(editElements:_*)

    }

  }

  // show/hide UI elements as needed
  private def updateUI(): Unit = {
    if (!profiles.selection.item.panel.isVisible) {
      profilePanels.foreach(_.panel.setVisible(false))
      profiles.selection.item.panel.setVisible(true)
    }
    if (!distributions.selection.item.panel.isVisible) {
      distributionPanels.foreach(_.panel.setVisible(false))
      distributions.selection.item.panel.setVisible(true)
    }
  }

}