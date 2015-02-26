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
class EnableDisableComboBox[E <: Object : ClassTag](initialItems: List[E]) extends ComboBox[E](Nil) {
  private var items        = initialItems
  private var enabledItems = scala.collection.mutable.Set[E](items: _*)
  private var model        = new EnabledDisabledComboBoxModel(None)

  def setItemsAndResetSelectedItem(newItems: List[E]): Unit = {
    items = newItems
    model = new EnabledDisabledComboBoxModel(None)
    peer.setModel(model)
  }

  def setItemsAndPreserveSelectedItem(newItems: List[E]): Unit = {
    val oldItem = selection.item
    items = newItems
    model = new EnabledDisabledComboBoxModel(Some(oldItem))
    peer.setModel(model)
  }

  def resetEnabledItems(): Unit =
    items.foreach(enabledItems.add)

  /**
   * This custom model does not allow multiple selections and does not allow items marked as disabled to be
   * selected. An initial selection is permitted, but only if it is amongst the currently enabled items.
   * If initialSelection is None or not amongst the enabled items, the first option that is an enabled item
   * is selected.
   */
  private class EnabledDisabledComboBoxModel(initialSelection: Option[E]) extends DefaultComboBoxModel[E](items.toArray) {
    var selected = initialSelection.filter(enabledItems.contains).orElse(items.find(enabledItems.contains))

    override
    def getSelectedItem: Object = selected.orNull

    override
    def setSelectedItem(o: Object): Unit = {
      Try {
        val item = o.asInstanceOf[E]
        if (enabledItems.contains(item))
          selected = Some(item)
      }
    }

    def markItemDisabled(item: E): Unit = {
      if (selected.forall(_.equals(item))) {
        val idx = items.indexOf(item)
        selected = items.find(enabledItems.contains)
        fireContentsChanged(this, idx, idx)
        EnabledDisabledComboBoxModel.this.setSelectedItem(selected.orNull)
      }
    }

    def markItemEnabled(item: E): Unit = {
      val idx = items.indexOf(item)
      fireContentsChanged(this, idx, idx)
    }
  }

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


  /**
   * Note that we allow items to be enabled / disabled that don't actually exist in items because they may become
   * added at a later point through adding / setting items.
   */
  def disableItem(item: E): Unit = {
    enabledItems -= item
    model.markItemDisabled(item)
  }

  def enableItem(item: E): Unit = {
    enabledItems += item
    model.markItemEnabled(item)
  }

  def removeItems(badItems: List[E]): Unit = {
    val oldItem = selection.item
    items = items.diff(badItems)
    model = new EnabledDisabledComboBoxModel(Some(oldItem))
    peer.setModel(model)
  }
  def removeItem(item: E): Unit =
    removeItems(List(item))

  def addItems(newItems: List[E]): Unit = {
    val oldItem = selection.item
    items = items ++ newItems
    model = new EnabledDisabledComboBoxModel(Some(oldItem))
    peer.setModel(model)
  }
  def addItem(item: E): Unit =
    addItems(List(item))
}