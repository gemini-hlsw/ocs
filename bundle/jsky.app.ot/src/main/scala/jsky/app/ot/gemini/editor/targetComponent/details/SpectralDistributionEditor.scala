package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{Component, GridBagConstraints}

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.core.Wavelength
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.EmissionLine.{Continuum, Flux}
import edu.gemini.spModel.target._
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.app.ot.gemini.editor.targetComponent.details.NumericPropertySheet.Prop

import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.ListView.Renderer
import scala.swing.event.{MouseClicked, SelectionChanged}
import scala.swing.{ComboBox, Dimension, GridBagPanel}
import scalaz.Scalaz._


final class SpectralDistributionEditor extends GridBagPanel with TelescopePosEditor {

  private[this] var spt: SPTarget = new SPTarget

  private val defaultBlackBody    = BlackBody(10000)
  private val defaultEmissionLine = EmissionLine(Wavelength.fromMicrons(2.2), 500, Flux.fromWatts(5.0e-19), Continuum.fromWatts(1.0e-16))
  private val defaultPowerLaw     = PowerLaw(1)

  private def blackBodyOrDefault   (t: SPTarget): BlackBody     = t.getTarget.getSpectralDistribution.fold(defaultBlackBody)(_.asInstanceOf[BlackBody])
  private def emissionLineOrDefault(t: SPTarget): EmissionLine  = t.getTarget.getSpectralDistribution.fold(defaultEmissionLine)(_.asInstanceOf[EmissionLine])
  private def powerLawOrDefault    (t: SPTarget): PowerLaw      = t.getTarget.getSpectralDistribution.fold(defaultPowerLaw)(_.asInstanceOf[PowerLaw])


  lazy val libraryStarDetails     = new ComboBox[LibraryStar](LibraryStar.Values) {
    renderer = Renderer(_.sedSpectrum)
  }
  lazy val libraryNonStarDetails  = new ComboBox[LibraryNonStar](LibraryNonStar.values) {
    renderer = Renderer(_.label)
  }
  lazy val blackBodyDetails = NumericPropertySheet[BlackBody](None, blackBodyOrDefault,
    Prop("Temperature", "Kelvin",   _.temperature,          (a, v) => spt.getTarget.setSpectralDistribution(Some(BlackBody(v))))
  )
  lazy val emissionLineDetails = NumericPropertySheet[EmissionLine](None, emissionLineOrDefault,
    Prop("Wavelength",  "µm",       _.wavelength.toMicrons, (a, v) => spt.getTarget.setSpectralDistribution(Some(EmissionLine(Wavelength.fromMicrons(v),  a.width,  a.flux,             a.continuum)))),
    Prop("Width",       "km/s",     _.width,                (a, v) => spt.getTarget.setSpectralDistribution(Some(EmissionLine(a.wavelength,               v,        a.flux,             a.continuum)))),
    Prop("Flux",        "W/m²",     _.flux.toWatts,         (a, v) => spt.getTarget.setSpectralDistribution(Some(EmissionLine(a.wavelength,               a.width,  Flux.fromWatts(v),  a.continuum)))),
    Prop("Continuum",   "W/m²/µm",  _.continuum.toWatts,    (a, v) => spt.getTarget.setSpectralDistribution(Some(EmissionLine(a.wavelength,               a.width,  a.flux,             Continuum.fromWatts(v)))))
  )
  lazy val powerLawDetails = NumericPropertySheet[PowerLaw](None, powerLawOrDefault,
    Prop("Index",       "",         _.index,                (a, v) => spt.getTarget.setSpectralDistribution(Some(PowerLaw(v))))
  )


  border = titleBorder("Spectral Distribution")
  preferredSize = new Dimension(300, 150)
  minimumSize   = new Dimension(300, 150)

  case class DistributionPanel(label: String, panel: Component, default: Option[SpectralDistribution])
  private val distributionPanels = List(
    DistributionPanel("Library Star",     libraryStarDetails.peer,    Some(libraryStarDetails.selection.item)),
    DistributionPanel("Library Non-Star", libraryNonStarDetails.peer, Some(libraryNonStarDetails.selection.item)),
    DistributionPanel("Black Body",       blackBodyDetails,           Some(defaultBlackBody)),
    DistributionPanel("Emission Line",    emissionLineDetails,        Some(defaultEmissionLine)),
    DistributionPanel("Power Law",        powerLawDetails,            Some(defaultPowerLaw))
  )


  private val distributions = new ComboBox[DistributionPanel](distributionPanels) {
    renderer = Renderer(_.label)
  }

  layout(distributions) = new Constraints {
    anchor  = Anchor.North
    gridx   = 0
    gridy   = 0
    weightx = 1
    fill    = Fill.Horizontal
  }
  // add all distribution panels in the same grid cell,
  // the individual panels will be made visible/invisible as needed
  distributionPanels.foreach { p =>
    peer.add(p.panel, new GridBagConstraints <| { c =>
      c.gridx   = 0
      c.gridy   = 1
      c.weightx = 1
      c.fill    = GridBagConstraints.HORIZONTAL
      //      c.insets = ins
    })
    p.panel.setVisible(false)
  }
  distributions.selection.item.panel.setVisible(true)

  listenTo(distributions.selection, libraryStarDetails.selection, libraryNonStarDetails.selection, mouse.clicks, distributions.mouse.clicks)
  reactions += {

    case SelectionChanged(`distributions`) =>
      spt.getTarget.setSpectralDistribution(distributions.selection.item.default)
      spt.notifyOfGenericUpdate()
      //edit(edu.gemini.shared.util.immutable.None.instance(), spt, null)

    case SelectionChanged(`libraryStarDetails`) =>
      spt.getTarget.setSpectralDistribution(Some(libraryStarDetails.selection.item))
      spt.notifyOfGenericUpdate()

    case SelectionChanged(`libraryNonStarDetails`) =>
      spt.getTarget.setSpectralDistribution(Some(libraryNonStarDetails.selection.item))
      spt.notifyOfGenericUpdate()

    case MouseClicked(_,_,_,_,_)      =>
      if (!enabled) enableAll(true)

  }


  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {

    deafTo(distributions.selection, libraryStarDetails.selection, libraryNonStarDetails.selection)

    spt = spTarget
    spt.getTarget.getSpectralDistribution.getOrElse(LibraryStar.A0V) match {
      case s: LibraryStar    => distributions.selection.item = distributionPanels.head; libraryStarDetails.selection.item = s
      case s: LibraryNonStar => distributions.selection.item = distributionPanels(1);   libraryNonStarDetails.selection.item = s
      case s: BlackBody      => distributions.selection.item = distributionPanels(2);   distributionPanels(2).panel.asInstanceOf[NumericPropertySheet[BlackBody]].edit(obsContext, spTarget, node)
      case s: EmissionLine   => distributions.selection.item = distributionPanels(3);   distributionPanels(3).panel.asInstanceOf[NumericPropertySheet[EmissionLine]].edit(obsContext, spTarget, node)
      case s: PowerLaw       => distributions.selection.item = distributionPanels(4);   distributionPanels(4).panel.asInstanceOf[NumericPropertySheet[PowerLaw]].edit(obsContext, spTarget, node)
      case s: UserDefined    => throw new Error("not yet supported")
    }

    update()

    listenTo(distributions.selection, libraryStarDetails.selection, libraryNonStarDetails.selection)
  }

  private def update(): Unit = {
    distributionPanels.foreach(_.panel.setVisible(false))
    distributions.selection.item.panel.setVisible(true)
    enableAll(spt.getTarget.getSpectralDistribution.isDefined)
    revalidate()
    repaint()
  }

  private def enableAll(b: Boolean) = {
    enabled = b
    distributions.enabled = b
    distributionPanels.map(_.panel).foreach(_.setEnabled(b))
    if (b) {
      tooltip = ""
      border = titleBorder("Spectral Distribution")
    } else {
      tooltip = "Click anywhere to activate this."
      border = titleBorder("Spectral Distribution [Click to Activate]")
    }
  }

}