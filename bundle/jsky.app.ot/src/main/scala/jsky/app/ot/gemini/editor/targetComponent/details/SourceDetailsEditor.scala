package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{Component, GridBagConstraints, Insets}
import javax.swing.JPanel

import edu.gemini.auxfile.client.AuxFileClient
import edu.gemini.auxfile.copier.AuxFileType
import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.core.{LibraryNonStar, LibraryStar, AuxFileSpectrum, UserDefinedSpectrum, EmissionLine, PowerLaw, BlackBody, SpectralDistribution, GaussianSource, UniformSource, PointSource, SpatialProfile, SPProgramID}
import edu.gemini.spModel.core.WavelengthConversions._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target._
import jsky.app.ot.OT
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.app.ot.gemini.editor.targetComponent.details.NumericPropertySheet.Prop
import jsky.app.ot.vcs.VcsOtClient
import squants.motion.VelocityConversions._
import squants.motion.{KilometersPerSecond, Velocity}
import squants.radio.IrradianceConversions._
import squants.radio.SpectralIrradianceConversions._
import squants.radio.{ErgsPerSecondPerSquareCentimeter, ErgsPerSecondPerSquareCentimeterPerAngstrom, Irradiance, SpectralIrradiance, WattsPerSquareMeter, WattsPerSquareMeterPerMicron}

import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.ListView.Renderer
import scala.swing.event.SelectionChanged
import scala.swing.{ComboBox, GridBagPanel, Label, Swing}
import scalaz.Scalaz._


final class SourceDetailsEditor extends GridBagPanel with TelescopePosEditor with ReentrancyHack {

  // ==== The Target

  private[this] var spt: SPTarget = new SPTarget

  private def setDistribution(sd: SpectralDistribution): Unit         = setDistribution(Some(sd))
  private def setDistribution(sd: Option[SpectralDistribution]): Unit = spt.setSpectralDistribution(sd)
  private def setProfile     (sp: SpatialProfile): Unit               = setProfile(Some(sp))
  private def setProfile     (sp: Option[SpatialProfile]): Unit       = spt.setSpatialProfile(sp)

  // ==== Spatial Profile Details

  private val defaultGaussianSource = GaussianSource(0.5)

  private def gaussianOrDefault(t: SPTarget): GaussianSource = t.getTarget.getSpatialProfile.fold(defaultGaussianSource)(_.asInstanceOf[GaussianSource])

  private val pointSourceDetails    = new JPanel()
  private val uniformSourceDetails  = new JPanel()
  private val gaussianSourceDetails = NumericPropertySheet[GaussianSource](None, t => gaussianOrDefault(t),
    Prop("with FWHM",  "arcsec", _.fwhm, (a, v) => setProfile(GaussianSource(v)))
  )

  private case class ProfilePanel(label: String, panel: Component, value: () => Option[SpatialProfile])
  private val profilePanels = List(
    ProfilePanel("«undefined»",              new JPanel(),          () => None),
    ProfilePanel("Point Source",             pointSourceDetails,    () => Some(PointSource)),
    ProfilePanel("Extended Gaussian Source", gaussianSourceDetails, () => Some(defaultGaussianSource)),
    ProfilePanel("Extended Uniform Source",  uniformSourceDetails,  () => Some(UniformSource))
  )

  private val profiles = new ComboBox[ProfilePanel](profilePanels) {
    renderer = Renderer(_.label)
  }

  // === Spectral Distribution Details

  private val defaultBlackBody    = BlackBody(10000)
  private val defaultEmissionLine = EmissionLine(2.2.microns, 500.kps, 5.0e-19.wattsPerSquareMeter, 1.0e-16.wattsPerSquareMeterPerMicron)
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
    Prop("Wavelength",  "μm", _.wavelength.toMicrons, (a, v)                     => setDistribution(a.copy(wavelength = v.microns))),
    Prop("Width",             _.width,                (a, v: Velocity)           => setDistribution(a.copy(width      = v        )), KilometersPerSecond),
    Prop("Flux",              _.flux,                 (a, v: Irradiance)         => setDistribution(a.copy(flux       = v        )), WattsPerSquareMeter, ErgsPerSecondPerSquareCentimeter),
    Prop("Continuum",         _.continuum,            (a, v: SpectralIrradiance) => setDistribution(a.copy(continuum  = v        )), WattsPerSquareMeterPerMicron, ErgsPerSecondPerSquareCentimeterPerAngstrom)
  )
  private val powerLawDetails = NumericPropertySheet[PowerLaw](None, powerLawOrDefault,
    Prop("Index",       "",         _.index,                      (a, v) => setDistribution(PowerLaw(v)))
  )
  private val emptySedFilesModel = ComboBox.newConstantModel(Seq(AuxFileSpectrum.Undefined))
  private val userDefinedDetails = new ComboBox[AuxFileSpectrum](Seq()) {
    peer.setModel(emptySedFilesModel)
    renderer = Renderer(_.name)
    // Selects the given sed file, if it is not available (e.g. because the aux file it represents
    // has been removed from the program) an "Undefined" placeholder is inserted instead.
    def selectItem(s: AuxFileSpectrum) = {
      // try to select the given aux file
      selection.item = s
      // if that fails, the file is not available (anymore)
      if (peer.getModel.getSelectedItem != s) {
        // deal with missing files: replace them with our good friend, the "Undefined" place holder
        val oldModel = peer.getModel
        val items    = Range(0, oldModel.getSize).map(oldModel.getElementAt)
        val newModel = ComboBox.newConstantModel(AuxFileSpectrum.Undefined +: items)
        peer.setModel(newModel)
        peer.setSelectedItem(AuxFileSpectrum.Undefined)
        setDistribution(AuxFileSpectrum.Undefined)
      }
    }
  }

  private case class DistributionPanel(label: String, panel: Component, value: () => Option[SpectralDistribution])
  private val distributionPanels = List(
    DistributionPanel("«undefined»",      new JPanel(),               () => None),
    DistributionPanel("Library Star",     libraryStarDetails.peer,    () => Some(libraryStarDetails.selection.item)),
    DistributionPanel("Library Non-Star", libraryNonStarDetails.peer, () => Some(libraryNonStarDetails.selection.item)),
    DistributionPanel("Black Body",       blackBodyDetails,           () => Some(defaultBlackBody)),
    DistributionPanel("Emission Line",    emissionLineDetails,        () => Some(defaultEmissionLine)),
    DistributionPanel("Power Law",        powerLawDetails,            () => Some(defaultPowerLaw)),
    DistributionPanel("User Defined",     userDefinedDetails.peer,    () => Some(userDefinedDetails.selection.item))
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
    libraryNonStarDetails.selection,
    userDefinedDetails.selection
  )

  listenTo(editElements:_*)
  reactions += {

    case SelectionChanged(`profiles`)  =>
      setProfile(profiles.selection.item.value())

    case SelectionChanged(`distributions`) =>
      setDistribution(distributions.selection.item.value())

    case SelectionChanged(`libraryStarDetails`) =>
      setDistribution(libraryStarDetails.selection.item)

    case SelectionChanged(`libraryNonStarDetails`) =>
      setDistribution(libraryNonStarDetails.selection.item)

    case SelectionChanged(`userDefinedDetails`) =>
        setDistribution(userDefinedDetails.selection.item)

  }

  // react to any kind of target change by updating all UI elements
  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = nonreentrant {

    // update target
    spt = spTarget

    // we only show the source editor for the base/science target, and we also only need to update it if visible
    visible = if (obsContext.isDefined) obsContext.getValue.getTargets.getBase == spTarget else false
    if (visible) {

      deafTo(editElements:_*)

      // update available user spectras (in case program has changed or files have been added/removed)
      updateSedFiles(node.getProgramID, sedFiles(node.getProgramID))

      // update UI elements to reflect the spatial profile
      spt.getTarget.getSpatialProfile match {
        case None                        => profiles.selection.item = profilePanels.head
        case Some(PointSource)           => profiles.selection.item = profilePanels(1)
        case Some(GaussianSource(_))     => profiles.selection.item = profilePanels(2); profilePanels(2).panel.asInstanceOf[NumericPropertySheet[GaussianSource]].edit(obsContext, spTarget, node)
        case Some(UniformSource)         => profiles.selection.item = profilePanels(3)
      }

      // update UI elements to reflect the spectral distribution
      spt.getTarget.getSpectralDistribution match {
        case None                        => distributions.selection.item = distributionPanels.head
        case Some(s: LibraryStar)        => distributions.selection.item = distributionPanels(1); libraryStarDetails.selection.item = s
        case Some(s: LibraryNonStar)     => distributions.selection.item = distributionPanels(2); libraryNonStarDetails.selection.item = s
        case Some(BlackBody(_))          => distributions.selection.item = distributionPanels(3); distributionPanels(3).panel.asInstanceOf[NumericPropertySheet[BlackBody]].edit(obsContext, spTarget, node)
        case Some(EmissionLine(_,_,_,_)) => distributions.selection.item = distributionPanels(4); distributionPanels(4).panel.asInstanceOf[NumericPropertySheet[EmissionLine]].edit(obsContext, spTarget, node)
        case Some(PowerLaw(_))           => distributions.selection.item = distributionPanels(5); distributionPanels(5).panel.asInstanceOf[NumericPropertySheet[PowerLaw]].edit(obsContext, spTarget, node)
        case Some(s: AuxFileSpectrum)    => distributions.selection.item = distributionPanels(6); userDefinedDetails.selectItem(s)
        case Some(s: UserDefinedSpectrum)=> sys.error("not supported") // this is only used in itc web app
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

  // update the sed file combo box
  private def updateSedFiles(programId: SPProgramID, optFiles: Option[Seq[AuxFileSpectrum]]) = {
    optFiles match {
      case Some(files) if files.nonEmpty =>
        userDefinedDetails.peer.setModel(ComboBox.newConstantModel(files))
        userDefinedDetails.enabled = true
      case _                             =>
        userDefinedDetails.peer.setModel(emptySedFilesModel)
        userDefinedDetails.enabled = false
    }

  }

  // get a list of all available sed files (aux files ending in ".sed")
  private def sedFiles(programId: SPProgramID): Option[Seq[AuxFileSpectrum]] = {
    try {
      import scala.collection.JavaConversions._
      VcsOtClient.unsafeGetRegistrar.registration(programId).map { peer =>
        val aux    = new AuxFileClient(OT.getKeyChain, peer.host, peer.port)
        val files  = aux.listAll(programId)
        files.
          filter(AuxFileType.getFileType(_) == AuxFileType.sed).
          map   (f => AuxFileSpectrum(programId.toString, f.getName)).
          toSeq
      }
    } catch {
      case _: Exception => None
    }
  }

}