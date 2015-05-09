package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{Component, Dimension, GridBagConstraints}
import javax.swing.JPanel

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target._
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.app.ot.gemini.editor.targetComponent.details.NumericPropertySheet.Prop

import scala.swing.GridBagPanel.{Anchor, Fill}
import scala.swing.ListView.Renderer
import scala.swing.event.{MouseClicked, SelectionChanged}
import scala.swing.{ComboBox, GridBagPanel}
import scalaz.Scalaz._


final class SpatialProfileEditor extends GridBagPanel with TelescopePosEditor {

  private[this] var spt: SPTarget = new SPTarget

  private val defaultPointSource    = PointSource()
  private val defaultUniformSource  = UniformSource()
  private val defaultGaussianSource = GaussianSource(0.5)

  private def gaussianOrDefault(t: SPTarget): GaussianSource = t.getTarget.getSpatialProfile.fold(defaultGaussianSource)(_.asInstanceOf[GaussianSource])



  lazy val pointSourceDetails    = new JPanel()
  lazy val uniformSourceDetails  = new JPanel()
  lazy val gaussianSourceDetails = NumericPropertySheet[GaussianSource](None, t => gaussianOrDefault(t),
    Prop("FWHM",  "Full Width at Half Max (arcsec)", _.fwhm, (a, v) => spt.getTarget.setSpatialProfile(Some(GaussianSource(v))))
  )

  border = titleBorder("Spatial Profile")
  preferredSize = new Dimension(300, 100)
  minimumSize   = new Dimension(300, 100)


  case class ProfilePanel(label: String, panel: Component, default: SpatialProfile)
  private val profilePanels = List(
    ProfilePanel("Point Source",             pointSourceDetails,    defaultPointSource),
    ProfilePanel("Extended Gaussian Source", gaussianSourceDetails, defaultGaussianSource),
    ProfilePanel("Extended Uniform Source",  uniformSourceDetails,  defaultUniformSource)
  )

  private val profiles = new ComboBox[ProfilePanel](profilePanels) {
    renderer = Renderer(_.label)
  }

  layout(profiles) = new Constraints {
    anchor  = Anchor.North
    gridx   = 0
    gridy   = 0
    weightx = 1
    fill    = Fill.Horizontal
  }
  // add all distribution panels in the same grid cell,
  // the individual panels will be made visible/invisible as needed
  profilePanels.foreach { p =>
    peer.add(p.panel, new GridBagConstraints <| { c =>
      c.gridx   = 0
      c.gridy   = 1
      c.weightx = 1
      c.fill    = GridBagConstraints.HORIZONTAL
//      c.insets = ins
    })
  }
  update()

  listenTo(profiles.selection, profiles.mouse.clicks, mouse.clicks)
  reactions += {
    case SelectionChanged(_)  =>
      spt.getTarget.setSpatialProfile(Some(profiles.selection.item.default))
      spt.notifyOfGenericUpdate()

    case MouseClicked(_,_,_,_,_)      =>
      if (!enabled) enableAll(true)
  }

  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {

    deafTo(profiles.selection)

    spt = spTarget

    spt.getTarget.getSpatialProfile.getOrElse(defaultPointSource) match {
      case s: PointSource => profiles.selection.item = profilePanels.head;
      case s: GaussianSource => profiles.selection.item = profilePanels(1); profilePanels(1).panel.asInstanceOf[NumericPropertySheet[GaussianSource]].edit(obsContext, spTarget, node)
      case s: UniformSource => profiles.selection.item = profilePanels(2);
    }

    update()

    listenTo(profiles.selection)

  }

  private def update(): Unit = {
    profilePanels.foreach(_.panel.setVisible(false))
    profiles.selection.item.panel.setVisible(true)
    enableAll(spt.getTarget.getSpatialProfile.isDefined)
    revalidate()
    repaint()
  }

  private def enableAll(b: Boolean) = {
    enabled = b
    profiles.enabled = b
    profilePanels.map(_.panel).foreach(_.setEnabled(b))
    if (b) {
      tooltip = ""
      border = titleBorder("Spatial Profile")
    } else {
      tooltip = "Click anywhere to activate this."
      border = titleBorder("Spatial Profile [Click to Activate]")
    }
  }

}