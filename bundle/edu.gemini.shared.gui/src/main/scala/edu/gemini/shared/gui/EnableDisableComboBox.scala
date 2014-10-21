package edu.gemini.shared.gui

import java.awt.{Component, Dimension, Color}
import javax.swing.border.EmptyBorder
import javax.swing._

import scala.reflect.ClassTag
import scala.swing.ComboBox
import scala.util.Try


/**
 * A ComboBox where items can be marked as enabled or disabled.
 * Disabled items appear grayed out and cannot be selected.
 */
class EnableDisableComboBox[E <: Object : ClassTag](options: List[E]) extends ComboBox[E](Nil) {
  private var enabledItems = scala.collection.mutable.Set[E](options: _*)

  def reset(): Unit =
    options.foreach(enabledItems.add)

  /**
   * This custom model does not allow multiple selections and does not allow items marked as disabled to be
   * selected.
   */
  private object enabledDisabledComboBoxModel extends DefaultComboBoxModel[E](options.toArray) {
    var selection: Option[E] = options.find(enabledItems.contains)

    override
    def getSelectedItem: Object = selection.orNull

    override
    def setSelectedItem(o: Object): Unit = {
      Try {
        val item = o.asInstanceOf[E]
        if (enabledItems.contains(item))
          selection = Some(item)
      }
    }
  }
  peer.setModel(enabledDisabledComboBoxModel)

  /**
   * This renderer visually configures the combo box items so that the disabled items are evident.
   * Note that it is NOT possible to use a BasicListCellRenderer with JDK 1.7 and Scala because the
   * BasicListCellRenderer relies on omitting parameterized types, which is not permitted in Scala.
   * For this reason, we had to reimplement the logic of a BasicListCellRenderer and add the logic
   * to gray out disabled components.
   */
  private object enabledDisabledComboBoxRenderer extends JLabel with ListCellRenderer[E] {
    private val disabledColor     = Color.lightGray
    private val noFocusBorder     = new EmptyBorder(1, 1, 1, 1)

    setOpaque(true)
    setBorder(noFocusBorder)

    override
    def getPreferredSize: Dimension = {
      if ((this.getText == null) || (this.getText == "")) {
        setText(" ")
        val size = super.getPreferredSize
        setText("")
        size
      }
      else {
        super.getPreferredSize
      }
    }

    override
    def getListCellRendererComponent(list: JList[_ <: E], value: E, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component = {
      def configureDefaults(): Unit = {
        if (isSelected) {
          setBackground(list.getSelectionBackground)
          setForeground(list.getSelectionForeground)
        }
        else {
          setBackground(list.getBackground)
          setForeground(list.getForeground)
        }
        setFont(list.getFont)

        value match {
          case i: Icon => setIcon(i)
          case _       => setText(if (value == null) "" else value.toString)
        }
      }

      configureDefaults()
      if (!enabledItems.contains(value)) {
        setBackground(list.getBackground)
        setForeground(disabledColor)
      }

      this
    }
  }
  peer.setRenderer(enabledDisabledComboBoxRenderer)

  def disable(item: E): Unit =
    enabledItems -= item
  def enable(item: E): Unit =
    enabledItems += item
}