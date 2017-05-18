package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt.{Component, Dimension, GridBagConstraints, Insets}
import javax.swing.JPanel
import javax.swing.event.{PopupMenuEvent, PopupMenuListener}

import edu.gemini.auxfile.client.AuxFileClient
import edu.gemini.auxfile.copier.AuxFileType
import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.core.WavelengthConversions._
import edu.gemini.spModel.core.{Target, AuxFileSpectrum, BlackBody, EmissionLine, GaussianSource, LibraryNonStar, LibraryStar, PointSource, PowerLaw, SPProgramID, SpatialProfile, SpectralDistribution, UniformSource, UserDefinedSpectrum}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target._
import jsky.app.ot.OT
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.app.ot.gemini.editor.targetComponent.details2.NumericPropertySheet.Prop
import jsky.app.ot.vcs.VcsOtClient
import squants.motion.VelocityConversions._
import squants.motion.{KilometersPerSecond, Velocity}
import squants.radio.IrradianceConversions._
import squants.radio.SpectralIrradianceConversions._
import squants.radio.{ErgsPerSecondPerSquareCentimeter, ErgsPerSecondPerSquareCentimeterPerAngstrom, Irradiance, SpectralIrradiance, WattsPerSquareMeter, WattsPerSquareMeterPerMicron}

import scala.collection.immutable.Set
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.ListView.Renderer
import scala.swing.event.SelectionChanged
import scala.swing.{ComboBox, GridBagPanel, Label, Swing}
import scalaz.Scalaz._


final class SourceDetailsEditor extends GridBagPanel with TelescopePosEditor with ReentrancyHack {

  // ==== The current program ID
  private[this] var programId: SPProgramID = SPProgramID.toProgramID("")  // this is needed for getting the SED aux files list

  // ==== The current target
  private[this] var spt: SPTarget = new SPTarget

  private def getDistribution(t: SPTarget): Option[SpectralDistribution]= Target.spectralDistribution.get(t.getTarget).join
  private def getProfile(t: SPTarget): Option[SpatialProfile] = Target.spatialProfile.get(t.getTarget).join

  private def setDistribution(sd: SpectralDistribution): Unit = setDistribution(Some(sd))
  private def setDistribution(sd: Option[SpectralDistribution]): Unit = Target.spectralDistribution.set(spt.getTarget, sd).foreach(spt.setTarget)
  private def setProfile     (sp: SpatialProfile): Unit = setProfile(Some(sp))
  private def setProfile     (sp: Option[SpatialProfile]): Unit = Target.spatialProfile.set(spt.getTarget, sp).foreach(spt.setTarget)

  // ==== Spatial Profile Details

  private val defaultGaussianSource = GaussianSource(0.5)

  private def gaussianOrDefault(t: SPTarget): GaussianSource = getProfile(t).fold(defaultGaussianSource)(_.asInstanceOf[GaussianSource])

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

  private def blackBodyOrDefault   (t: SPTarget): BlackBody     = getDistribution(t).fold(defaultBlackBody)(_.asInstanceOf[BlackBody])
  private def emissionLineOrDefault(t: SPTarget): EmissionLine  = getDistribution(t).fold(defaultEmissionLine)(_.asInstanceOf[EmissionLine])
  private def powerLawOrDefault    (t: SPTarget): PowerLaw      = getDistribution(t).fold(defaultPowerLaw)(_.asInstanceOf[PowerLaw])

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
  private val userDefinedDetails = new AuxFileSelector()

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

    // update target and program id
    spt       = spTarget
    programId = node.getProgramID

    // we only show the source editor for the base/science target, and we also only need to update it if visible
    visible = obsContext.isDefined && obsContext.getValue.getTargets.getArbitraryTargetFromAsterism == spTarget && !spt.isTooTarget
    if (visible) {

      deafTo(editElements:_*)

      // update UI elements to reflect the spatial profile
      Target.spatialProfile.get(spt.getTarget).join match {
        case None                        => profiles.selection.item = profilePanels.head
        case Some(PointSource)           => profiles.selection.item = profilePanels(1)
        case Some(GaussianSource(_))     => profiles.selection.item = profilePanels(2); profilePanels(2).panel.asInstanceOf[NumericPropertySheet[GaussianSource]].edit(obsContext, spTarget, node)
        case Some(UniformSource)         => profiles.selection.item = profilePanels(3)
      }

      // update UI elements to reflect the spectral distribution
      Target.spectralDistribution.get(spt.getTarget).join match  {
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
      profilePanels.filter(_.panel.isVisible).foreach(_.panel.setVisible(false))
      profiles.selection.item.panel.setVisible(true)
    }
    if (!distributions.selection.item.panel.isVisible) {
      distributionPanels.filter(_.panel.isVisible).foreach(_.panel.setVisible(false))
      distributions.selection.item.panel.setVisible(true)
    }
  }

  /** UI element that allows to choose from the currently available set of SED aux files for the current program.
    * Unfortunately there is no simple way to keep in sync with changes to the available aux files. In order to
    * still be able to present the user with an up-to-date list of available aux files the combobox model is updated
    * every time when the user opens it (i.e. displays the popup menu). Ideally there would be a simple way to get
    * notifications when the available set of aux files has changed, but for now this is the best I could come up
    * with.
    */
  final class AuxFileSelector extends ComboBox[AuxFileSpectrum](Seq()) {
    val filesNotAvailable = ComboBox.newConstantModel(Seq(AuxFileSpectrum.Undefined))
    tooltip = "SEDs may be added via the File Attachment tab"
    renderer = Renderer(_.name)
    minimumSize = new Dimension(250, preferredSize.getHeight.toInt)     // avoid resizing of component every time model changes
    preferredSize = new Dimension(250, preferredSize.getHeight.toInt)
    peer.setModel(filesNotAvailable)
    peer.addPopupMenuListener(new PopupMenuListener {
      // If the program id is known and the popup is about to be displayed we need to update the combobox model
      // with all currently available SED aux files.
      override def popupMenuWillBecomeVisible(e: PopupMenuEvent): Unit    = { updateAuxFileModel(programId) }
      override def popupMenuWillBecomeInvisible(e: PopupMenuEvent): Unit  = {}
      override def popupMenuCanceled(e: PopupMenuEvent): Unit             = {}
    })

    /** Selects the given SED aux file.
      * We are taking a short cut here by just simply setting a model with exactly that value. This may seem strange,
      * but this allows to just simply set and display the current value without having to care about corner cases like:
      * Is the aux file listing currently available? Has this particular file been deleted? ...
      */
    def selectItem(s: AuxFileSpectrum) = {
      peer.setModel(ComboBox.newConstantModel(Seq(s)))
    }

    /** Updates the combobox model with the currently available aux files.*/
    private def updateAuxFileModel(programId: SPProgramID): Unit =  {

      // Determines the currently selected SED aux file (if any)
      def selectedAuxFile = getDistribution(spt) match {
        case Some(s: AuxFileSpectrum) => Some(s)
        case _                        => None
      }

      // Update the model and make sure that the currently selected value is one of the available entries.
      // This allows to keep the current selection in case the aux file listing is currently not available, or if the
      // currently selected aux file has been removed (we don't want to simply replace an invalid selection with
      // another value without letting the user know). If the selected file has been removed, calling the ITC will
      // result in a meaningful error message that tells the user that the selected aux file is not available anymore.
      def setModel(available: Set[AuxFileSpectrum], selected: Option[AuxFileSpectrum]) = {
        val all1 = selected.fold(available)(s => available + s)               // add current selection to available files if needed
        val all2 = if (all1.isEmpty) Set(AuxFileSpectrum.Undefined) else all1 // if there isn't anything add a "dummy" element
        userDefinedDetails.peer.setModel(ComboBox.newConstantModel(all2.toSeq.sortBy(_.name))) // combo box model musn't be empty
        selected.foreach(s => userDefinedDetails.selection.item = s)
      }

      SourceDetailsEditor.this.deafTo(editElements:_*)    // DON'T trigger any events!
      getAuxFiles(programId) match {
        case files => setModel(files, selectedAuxFile)
      }
      SourceDetailsEditor.this.listenTo(editElements:_*)  // OK, done with updating UI elements

    }

    /** Gets a set of all available SED aux files for the given program ID.
      * The available aux files are filtered and only the ones ending in ".sed" are returned.
      * This is called in order to update the available aux files in the selection combo box right the moment
      * when the combobox is expanded to show the selection, therefore if the result is not returned from
      * the server in a short time, we simply ignore the result.
      */
    private def getAuxFiles(programId: SPProgramID): Set[AuxFileSpectrum] = {

      try {
        import scala.collection.JavaConversions._

        // block UI for at most 3 seconds, if getting the aux file listing takes longer, the result will be ignored
        Await.result(Future {

          VcsOtClient.unsafeGetRegistrar.registration(programId).map { peer =>

            val aux = new AuxFileClient(OT.getKeyChain, peer.host, peer.port)
            val files = aux.listAll(programId)
            files.
              filter(AuxFileType.getFileType(_) == AuxFileType.sed).
              map(f => AuxFileSpectrum(programId.toString, f.getName)).
              toSet

          }.getOrElse(Set())

        }, 3.seconds)

      } catch {
        // Ignore TimeoutException or communication problems
        case _: Exception => Set()
      }
    }
  }
}