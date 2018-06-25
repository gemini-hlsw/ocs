package jsky.app.ot.itc

import javax.swing.DefaultComboBoxModel

import edu.gemini.spModel.core.{SiderealTarget, Target}
import jsky.app.ot.editor.seq.EdIteratorFolder

import scala.collection.JavaConverters._
import scala.swing._
import scalaz._
import Scalaz._
import scala.swing.event.SelectionChanged

class TargetsPanel(owner: EdIteratorFolder) extends BoxPanel(Orientation.Horizontal) {

  // Start off empty and populate during update.
  private val targetComboBox = new ComboBox[Target](List(SiderealTarget.empty)) {
    renderer = new ListView.AbstractRenderer[Target, Label](new Label) {
      override def configure(list: ListView[_], isSelected: Boolean, focused: Boolean, a: Target, index: Int): Unit = {
        component.text = a.name
      }
    }
  }

  targetComboBox.tooltip = TargetsPanel.ttMsg
  contents += targetComboBox
  contents += Swing.HGlue
  tooltip = TargetsPanel.ttMsg

  private def targets: Option[List[Target]] =
    Option(owner.getContextTargetEnv).
      map(_.getAsterism.allSpTargets.map(_.getTarget)).
      map(_.toList)

  def update(): Unit = {
    deafTo(targetComboBox.selection)
    val sel = targetComboBox.selection.item

    val tgts = targets
    val nonempty = tgts.nonEmpty

    // We need to pass DefaultComboBoxModel a Java Vector<Target>.
    val tgtsVec = new java.util.Vector(tgts.getOrElse(List(TargetsPanel.noneTarget)).asJavaCollection)
    targetComboBox.peer.setModel(new DefaultComboBoxModel[Target](tgtsVec))
    targetComboBox.enabled = nonempty

    \/.fromTryCatchNonFatal(targetComboBox.selection.item = sel)
    listenTo(targetComboBox.selection)
  }

  def selectedTarget: Option[Target] = {
    val s = targetComboBox.selection.item
    // Yes, we want reference comparison here.
    if (s == TargetsPanel.noneTarget) None else Some(s)
  }

  deafTo(this)
  update()
  reactions += {
    case SelectionChanged(_) => publish(SelectionChanged(this))
  }
}

object TargetsPanel {
  val ttMsg = "Select target for ITC calculations."
  val noneTarget: Target = SiderealTarget.name.set(SiderealTarget.empty, "No target found")
}