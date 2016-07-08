package edu.gemini.shared.gui.bean

import javax.swing._
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.beans.PropertyDescriptor

import scalaz._
import Scalaz._

final class CheckboxEnumPropertyCtrl[B, T](title: String, pd: PropertyDescriptor, trueValue: T, falseValue: T) extends PropertyCtrl[B, T](pd) {
  val check = new JCheckBox(title)
  def this(pd: PropertyDescriptor, trueValue: T, falseValue: T) = this(pd.getDisplayName, pd, trueValue, falseValue)

  private val actionListener: ActionListener = new ActionListener() {
    override def actionPerformed(e: ActionEvent) {
      val oldVal = getVal
      val newVal = check.getModel.isSelected ? trueValue | falseValue
      setVal(newVal)
      fireEditEvent(oldVal, newVal)
    }
  }

  override protected def addComponentChangeListener(): Unit = {
    check.addActionListener(actionListener)
  }

  protected def removeComponentChangeListener(): Unit = {
    check.removeActionListener(actionListener)
  }

  override def getComponent: JComponent = check

  override def updateComponent(): Unit = {
    check.getModel.setSelected(getVal == trueValue)
  }

  override def updateBean(): Unit = {
    setVal(check.isSelected ? trueValue | falseValue)
  }
}
