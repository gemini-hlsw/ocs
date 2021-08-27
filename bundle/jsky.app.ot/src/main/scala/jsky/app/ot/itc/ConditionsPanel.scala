package jsky.app.ot.itc

import java.awt.{Color, Insets}

import edu.gemini.itc.shared.ObservingConditions
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, ImageQuality, SkyBackground, WaterVapor}
import jsky.app.ot.editor.seq.EdIteratorFolder

import scala.swing.GridBagPanel.Anchor
import scala.swing.ListView.Renderer
import scala.swing._
import scala.swing.event.SelectionChanged

import scalaz._
import Scalaz._

/**
  * User element that allows to change the conditions used for ITC calculations on-the-fly.
  * This gives observers the possibility to quickly adapt the conditions to current conditions during the night
  * (if they differ from the planned conditions) and check how this impacts the exposure times.
  */
final class ConditionsPanel(owner: EdIteratorFolder) extends GridBagPanel {

  private val ttMsg = "Select conditions for ITC calculations. Values different from program conditions are shown in red."

  class ConditionCB[A](items: Seq[A], renderFunc: A => String) extends ComboBox[A](items) {
    private var programValue = selection.item
    tooltip  = ttMsg
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
        selection.item = newValue
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

  def conditions: ObservingConditions =
    ObservingConditions(
      \/-(iq.selection.item),
      \/-(cc.selection.item),
      wv.selection.item,
      sb.selection.item,
      am.selection.item
    )

  def update(): Unit =
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

  tooltip = ttMsg

  layout(sb)  = new Constraints {
    gridx     = 0
    gridy     = 0
    insets    = new Insets(0, 0, 0, 0)
  }
  layout(cc) = new Constraints {
    gridx     = 1
    gridy     = 0
    insets    = new Insets(0, 3, 0, 0)
  }
  layout(iq) = new Constraints {
    gridx     = 2
    gridy     = 0
    insets    = new Insets(0, 3, 0, 0)
  }
  layout(wv) = new Constraints {
    gridx     = 3
    gridy     = 0
    insets    = new Insets(0, 3, 0, 0)
  }
  layout(am) = new Constraints {
    gridx     = 4
    gridy     = 0
    insets    = new Insets(0, 3, 0, 0)
  }

  deafTo(this)
  listenTo(sb.selection, cc.selection, iq.selection, wv.selection, am.selection)
  reactions += {
    case SelectionChanged(_) => publish(new SelectionChanged(this))
  }

}
