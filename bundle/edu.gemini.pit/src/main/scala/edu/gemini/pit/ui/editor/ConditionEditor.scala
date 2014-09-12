package edu.gemini.pit.ui.editor


import edu.gemini.model.p1.immutable._
import edu.gemini.pit.ui.util._
import swing._
import event.{ValueChanged, ButtonClicked}

object ConditionEditor {
  
  def open(c:Option[Condition], editable:Boolean, parent:UIElement) =
    new ConditionEditor(c.getOrElse(Condition.empty), editable).open(parent)
  
}

/**
 * Modal editor for an Condition.
 */
class ConditionEditor private (c: Condition, editable:Boolean) extends StdModalEditor[Condition]("Edit Conditions") { dialog =>

  // Editor component
  object Editor extends GridBagPanel with Rows {
    addRow(new Label("Cloud Cover"), CC)
    addRow(new Label("Image Quality"), IQ)
    addRow(new Label("Sky Background"), SB)
    addRow(new Label("Water Vapor"), WV)
  }

  // Editable
  CC.enabled = editable
  IQ.enabled = editable
  SB.enabled = editable
  WV.enabled = editable
  Contents.Footer.OkButton.enabled = editable

  // Validation
  validateEditor()

  object CC extends ComboBox(CloudCover.values) with ValueRenderer[CloudCover] {
    selection.item = c.cc
  }
  
  object IQ extends ComboBox(ImageQuality.values) with ValueRenderer[ImageQuality] {
    selection.item = c.iq
  }
  
  object SB extends ComboBox(SkyBackground.values) with ValueRenderer[SkyBackground]  {
    selection.item = c.sb
  }
  
  object WV extends ComboBox(WaterVapor.values) with ValueRenderer[WaterVapor] {
    selection.item = c.wv
  }

  // Construct our editor
  def editor = Editor

  // Construct a new value
  def value = c.copy(
    maxAirmass = None,
    cc = CC.selection.item,
    iq = IQ.selection.item,
    sb = SB.selection.item,
    wv = WV.selection.item)

}

