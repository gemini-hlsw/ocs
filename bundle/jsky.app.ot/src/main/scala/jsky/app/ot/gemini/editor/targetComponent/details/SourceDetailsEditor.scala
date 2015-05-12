package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{Component, GridBagConstraints, Insets}
import javax.swing.JPanel

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.core.Wavelength
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.EmissionLine.{Continuum, Flux}
import edu.gemini.spModel.target._
import jsky.app.ot.OTOptions
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.app.ot.gemini.editor.targetComponent.details.NumericPropertySheet.Prop

import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.ListView.Renderer
import scala.swing.event.{MouseClicked, SelectionChanged}
import scala.swing.{ComboBox, GridBagPanel, Label, Swing}
import scalaz.Scalaz._


final class SourceDetailsEditor extends GridBagPanel with TelescopePosEditor {

  // ==== The Target

  private[this] var spt: SPTarget = new SPTarget
  private[this] var isBase: Boolean = false
  private[this] var node: Option[ISPNode] = None

  private def setDistribution(sd: SpectralDistribution) = spt.getTarget.setSpectralDistribution(Some(sd))
  private def setProfile     (sp: SpatialProfile)       = spt.getTarget.setSpatialProfile(Some(sp))

  // ==== Spatial Profile Details

  private val defaultPointSource    = PointSource()
  private val defaultUniformSource  = UniformSource()
  private val defaultGaussianSource = GaussianSource(0.5)
  private val defaultProfile        = defaultPointSource

  private def gaussianOrDefault(t: SPTarget): GaussianSource = t.getTarget.getSpatialProfile.fold(defaultGaussianSource)(_.asInstanceOf[GaussianSource])

  private val pointSourceDetails    = new JPanel()
  private val uniformSourceDetails  = new JPanel()
  private val gaussianSourceDetails = NumericPropertySheet[GaussianSource](None, t => gaussianOrDefault(t),
    Prop("with FWHM",  "arcsec", _.fwhm, (a, v) => setProfile(GaussianSource(v)))
  )

  private case class ProfilePanel(label: String, panel: Component, default: SpatialProfile)
  private val profilePanels = List(
    ProfilePanel("Point Source",             pointSourceDetails,    defaultPointSource),
    ProfilePanel("Extended Gaussian Source", gaussianSourceDetails, defaultGaussianSource),
    ProfilePanel("Extended Uniform Source",  uniformSourceDetails,  defaultUniformSource)
  )

  private val profiles = new ComboBox[ProfilePanel](profilePanels) {
    renderer = Renderer(_.label)
  }

  // === Spectral Distribution Details

  private val defaultBlackBody    = BlackBody(10000)
  private val defaultEmissionLine = EmissionLine(Wavelength.fromMicrons(2.2), 500, Flux.fromWatts(5.0e-19), Continuum.fromWatts(1.0e-16))
  private val defaultPowerLaw     = PowerLaw(1)
  private val defaultDistribution = LibraryStar.A0V

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
    Prop("Temperature", "Kelvin",   _.temperature,          (a, v) => setDistribution(BlackBody(v)))
  )
  private val emissionLineDetails = NumericPropertySheet[EmissionLine](None, emissionLineOrDefault,
    Prop("Wavelength",  "µm",       _.wavelength.toMicrons, (a, v) => setDistribution(EmissionLine(Wavelength.fromMicrons(v),  a.width,  a.flux,             a.continuum))),
    Prop("Width",       "km/sec",   _.width,                (a, v) => setDistribution(EmissionLine(a.wavelength,               v,        a.flux,             a.continuum))),
    Prop("Flux",        "W/m²",     _.flux.toWatts,         (a, v) => setDistribution(EmissionLine(a.wavelength,               a.width,  Flux.fromWatts(v),  a.continuum))),
    Prop("Continuum",   "W/m²/µm",  _.continuum.toWatts,    (a, v) => setDistribution(EmissionLine(a.wavelength,               a.width,  a.flux,             Continuum.fromWatts(v))))
  )
  private val powerLawDetails = NumericPropertySheet[PowerLaw](None, powerLawOrDefault,
    Prop("Index",       "",         _.index,                (a, v) => setDistribution(PowerLaw(v)))
  )

  private case class DistributionPanel(label: String, panel: Component, default: SpectralDistribution)
  private val distributionPanels = List(
    DistributionPanel("Library Star",     libraryStarDetails.peer,    libraryStarDetails.selection.item),
    DistributionPanel("Library Non-Star", libraryNonStarDetails.peer, libraryNonStarDetails.selection.item),
    DistributionPanel("Black Body",       blackBodyDetails,           defaultBlackBody),
    DistributionPanel("Emission Line",    emissionLineDetails,        defaultEmissionLine),
    DistributionPanel("Power Law",        powerLawDetails,            defaultPowerLaw)
  )

  private val distributions = new ComboBox[DistributionPanel](distributionPanels) {
    renderer = Renderer(_.label)
  }

  // ==== The Layout

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

  // ==== Listeners and Reactions

  val editElements = List(
    profiles.selection,
    distributions.selection,
    libraryStarDetails.selection,
    libraryNonStarDetails.selection,
    mouse.clicks
  )

  listenTo(editElements:_*)
  reactions += {

    case SelectionChanged(`distributions`) =>
      setDistribution(distributions.selection.item.default)
      spt.notifyOfGenericUpdate()

    case SelectionChanged(`libraryStarDetails`) =>
      setDistribution(libraryStarDetails.selection.item)
      spt.notifyOfGenericUpdate()

    case SelectionChanged(`libraryNonStarDetails`) =>
      setDistribution(libraryNonStarDetails.selection.item)
      spt.notifyOfGenericUpdate()

    case SelectionChanged(`profiles`)  =>
      setProfile(profiles.selection.item.default)
      spt.notifyOfGenericUpdate()

    case MouseClicked(_,_,_,_,_)      =>
      if (!enabled && editable) {
        // first click will set profile & distribution to some default values
        // up to that moment both values are set to None
        setProfile(defaultProfile)
        setDistribution(defaultDistribution)
        spt.notifyOfGenericUpdate()
      }
  }

  def editable: Boolean =
    node.exists { n =>
      OTOptions.areRootAndCurrentObsIfAnyEditable(n.getProgram, n.getContextObservation)
    }

  // react to any kind of target change by updating all UI elements
  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {

    deafTo(editElements:_*)

    spt       = spTarget
    isBase    = if (obsContext.isDefined) obsContext.getValue.getTargets.getBase == spTarget else false
    this.node = Option(node)

    spt.getTarget.getSpatialProfile.getOrElse(defaultProfile) match {
      case s: PointSource => profiles.selection.item = profilePanels.head;
      case s: GaussianSource => profiles.selection.item = profilePanels(1); profilePanels(1).panel.asInstanceOf[NumericPropertySheet[GaussianSource]].edit(obsContext, spTarget, node)
      case s: UniformSource => profiles.selection.item = profilePanels(2);
    }

    spt.getTarget.getSpectralDistribution.getOrElse(defaultDistribution) match {
      case s: LibraryStar    => distributions.selection.item = distributionPanels.head; libraryStarDetails.selection.item = s
      case s: LibraryNonStar => distributions.selection.item = distributionPanels(1);   libraryNonStarDetails.selection.item = s
      case s: BlackBody      => distributions.selection.item = distributionPanels(2);   distributionPanels(2).panel.asInstanceOf[NumericPropertySheet[BlackBody]].edit(obsContext, spTarget, node)
      case s: EmissionLine   => distributions.selection.item = distributionPanels(3);   distributionPanels(3).panel.asInstanceOf[NumericPropertySheet[EmissionLine]].edit(obsContext, spTarget, node)
      case s: PowerLaw       => distributions.selection.item = distributionPanels(4);   distributionPanels(4).panel.asInstanceOf[NumericPropertySheet[PowerLaw]].edit(obsContext, spTarget, node)
      case s: UserDefined    => throw new Error("not yet supported") // at a later stage we will add support for aux files user spectras
    }

    update()

    listenTo(editElements:_*)

  }

  // show/hide UI elements as needed
  private def update(): Unit = {
    if (isBase) {
      visible = true

      profilePanels.foreach(_.panel.setVisible(false))
      profiles.selection.item.panel.setVisible(true)

      distributionPanels.foreach(_.panel.setVisible(false))
      distributions.selection.item.panel.setVisible(true)

      updateEnabledState(editable)
    } else {
      visible = false
    }
    revalidate()
    repaint()
  }

  def updateEnabledState(b: Boolean): Unit = {
    val hasProfile = Option(spt).exists(_.getTarget.getSpatialProfile.isDefined)
    enabled = b && hasProfile

    peer.getComponents.foreach(_.setEnabled(enabled))

    if (!hasProfile && editable) {
      tooltip = "Click on title to activate this."
      border = titleBorder("Source [Click to Activate]")
    } else {
      tooltip = ""
      border = titleBorder("Source")
    }
  }

}