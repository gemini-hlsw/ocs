package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt.{GridBagConstraints, Insets, GridBagLayout, Component, Dimension}
import java.awt.event.{ActionEvent, ActionListener}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.text.{NumberFormat, DecimalFormat}

import javax.swing.{SwingUtilities, JScrollPane, BorderFactory, JPanel, DefaultComboBoxModel, JFormattedTextField, JLabel, JList, ListCellRenderer, JButton, JComboBox, JTextField}
import javax.swing.ScrollPaneConstants.{ HORIZONTAL_SCROLLBAR_NEVER, VERTICAL_SCROLLBAR_AS_NEEDED }
import javax.swing.text.DefaultFormatter

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.app.ot.ui.util.FlatButtonUtil

import edu.gemini.spModel.core._
import edu.gemini.spModel.target.{WatchablePos, TelescopePosWatcher, SPTarget}

import scala.collection.immutable.TreeSet
import scalaz.{ Ordering => _, _ }, Scalaz._

import language.implicitConversions

// Ported from Java, so slightly nasty.
class MagnitudeEditor2 extends TelescopePosEditor[SPTarget] {

  val MAG_FORMAT = new DecimalFormat("0.0##")
  def formatBrightness(mag: Magnitude): String = MAG_FORMAT.format(mag.value)

  sealed trait Mode
  object Mode {
    case object Add extends Mode
    case object Edit extends Mode
  }

  sealed trait MagWidgetRow {
    def button: JButton
    def setTarget(target: SPTarget, mode: Mode): Unit
    val magnitudeBand: Option[MagnitudeBand] = None
    val bandCombo: Option[JComboBox[MagnitudeBand]] = None
    val systemCombo: Option[JComboBox[MagnitudeSystem]] = None
    val textField: Option[JTextField] = None
  }

  final class MagEditRow(band: MagnitudeBand) extends MagWidgetRow {

    val rmButtonAction: ActionListener = { (e: ActionEvent) =>
      removeBand(band)
    }

    val changeBandAction: ActionListener = { (e: ActionEvent) =>
      val newBand = cb.getSelectedItem.asInstanceOf[MagnitudeBand]
      if (newBand != null && newBand != band) {
        changeBand(band, newBand)
        changeSystem(newBand, newBand.defaultSystem) // OCSADV-355
      }
    }

    val changeSystemAction: ActionListener = { (e: ActionEvent) =>
      val band = cb.getSelectedItem.asInstanceOf[MagnitudeBand]
      if (band != null) {
        val system = systemCb.getSelectedItem.asInstanceOf[MagnitudeSystem]
        if (system != null)
          changeSystem(band, system)
      }
    }

    val updateMagnitudeListener: PropertyChangeListener =  { (evt: PropertyChangeEvent) =>
      try {
        val d = evt.getNewValue.asInstanceOf[Number].doubleValue
        updateMagnitudeValue(band, d);
      } catch {
        case e: Exception => // do nothing
      }
    }

    val button: JButton =
      FlatButtonUtil.createSmallRemoveButton() <| { b =>
        b.addActionListener(rmButtonAction)
        b.setToolTipText("Remove magnitude value");
      }

    val cb: JComboBox[MagnitudeBand] =
      new JComboBox[MagnitudeBand] {
        setToolTipText("Set magnitude band")
      }

    val systemCb: JComboBox[MagnitudeSystem] =
      new JComboBox[MagnitudeSystem](MagnitudeSystem.allForOTAsJava) {
        setRenderer(new ListCellRenderer[MagnitudeSystem] {
          def getListCellRendererComponent(
            list:  JList[_ <: MagnitudeSystem],
            value: MagnitudeSystem,
            index: Int,
            sel:   Boolean,
            foc:   Boolean
          ): Component =
            new JLabel(value.name)
        })
        setToolTipText("Set magnitude system")
      }

    val nf: NumberFormat =
      NumberFormat.getNumberInstance() <| { nf =>
        nf.setMinimumFractionDigits(1)
        nf.setMinimumIntegerDigits(1)
        nf.setGroupingUsed(false)
      }

    val tf: JFormattedTextField =
      new JFormattedTextField(nf) {
        setColumns(5)
        getFormatter.asInstanceOf[DefaultFormatter].setCommitsOnValidEdit(true)
        setToolTipText("Set brightness value")
        setMinimumSize(getPreferredSize)
      }

    def setTarget(target: SPTarget, mode: Mode): Unit = {

      val magOpt: Option[Magnitude] =
        for {
          t  <- Option(target)
          ms <- Target.magnitudes.get(t.getTarget)
          m  <- ms.find(_.band == band)
        } yield m

      List(button, cb, systemCb, tf).foreach(_.setVisible(magOpt.isDefined))
      magOpt.foreach { mag =>

        val options: Set[MagnitudeBand] = {
          val existingBands = Target.magnitudes.get(target.getTarget).map(_.map(_.band)).orZero
          TreeSet.empty[MagnitudeBand](MagnitudeBand.MagnitudeBandOrdering) ++ MagnitudeBand.all -- existingBands + band
        }

        cb.removeActionListener(changeBandAction)
        cb.setModel(new DefaultComboBoxModel(options.toArray))
        cb.setSelectedItem(band)
        cb.setMaximumRowCount(options.size)
        cb.addActionListener(changeBandAction)

        systemCb.removeActionListener(changeSystemAction)
        systemCb.setSelectedItem(mag.system)
        systemCb.addActionListener(changeSystemAction)

        tf.removePropertyChangeListener("value", updateMagnitudeListener)
        tf.setText(formatBrightness(mag))
        tf.addPropertyChangeListener("value", updateMagnitudeListener)

      }

    }

    override val magnitudeBand = Some(band)
    override val bandCombo     = Some(cb)
    override val systemCombo   = Some(systemCb)
    override val textField     = Some(tf)

  }

  class MagNewRow extends MagWidgetRow {

    val button: JButton =
      FlatButtonUtil.createSmallRemoveButton <| { b =>
        b.addActionListener((e: ActionEvent) => cancelAdd())
        b.setToolTipText("Stop adding a new magnitude value");
      }

    val tf: JTextField =
      new JTextField("Select \u2192") {
        setColumns(5)
        setEnabled(false)
        setMinimumSize(getPreferredSize)
        override def setEnabled(b: Boolean) =
          super.setEnabled(false) // never enable
      }

    val cb: JComboBox[MagnitudeBand] =
      new JComboBox[MagnitudeBand] {
        setToolTipText("Set passband for the new magnitude value")
      }

    val addAction: ActionListener =
      (e: ActionEvent) => Option(cb.getSelectedItem.asInstanceOf[MagnitudeBand]).foreach(addBand)

    def setTarget(target: SPTarget, mode: Mode): Unit = {
      val visible = (target != null) && mode == Mode.Add

      List(button, tf, cb).foreach(_.setVisible(visible))
      if (visible) {

        button.setEnabled(Target.magnitudes.get(target.getTarget).exists(_.nonEmpty))

        val options: Set[MagnitudeBand] = {
          val existingBands = Target.magnitudes.get(target.getTarget).map(_.map(_.band)).orZero
          TreeSet.empty[MagnitudeBand](MagnitudeBand.MagnitudeBandOrdering) ++ MagnitudeBand.all -- existingBands
        }

        cb.setMaximumRowCount(options.size)
        cb.removeActionListener(addAction)
        cb.setModel(new DefaultComboBoxModel(options.toArray))
        cb.setSelectedItem(null)
        cb.addActionListener(addAction)

      }
    }

    override val bandCombo = Some(cb)
    override val textField = Some(tf)

  }

  class MagPlusRow extends MagWidgetRow {

    val button: JButton =
      FlatButtonUtil.createSmallAddButton <| { b =>
        b.addActionListener((e: ActionEvent) => enableAdd())
        b.setToolTipText("Add a new magnitude value")
      }

    def setTarget(target: SPTarget, mode: Mode): Unit = {
      val visible = (target != null) && mode == Mode.Edit
      button.setVisible(visible)
    }

  }

  val watcher: TelescopePosWatcher =
    new TelescopePosWatcher {
      def telescopePosUpdate(tp: WatchablePos): Unit = tp match {
        case t: SPTarget => reinit(t)
        case _           =>
      }
    }

  private var target: SPTarget = null
  private val newRow = new MagNewRow

  private val rows: List[MagWidgetRow] =
    MagnitudeBand.all.sorted(MagnitudeBand.MagnitudeBandOrdering).map(new MagEditRow(_)) :+ newRow :+ new MagPlusRow

  val content: JPanel =
    new JPanel(new GridBagLayout) {
      setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0))
      rows.zipWithIndex.foreach { case (row, y) =>
        add(row.button, new GridBagConstraints {
          gridx = 0
          gridy = y
          insets = new Insets(0, 0, 5, 5)
          fill = GridBagConstraints.VERTICAL
        })
        row.textField.foreach(add(_, new GridBagConstraints {
          gridx = 1
          gridy = y
          insets = new Insets(0, 0, 5, 5)
        }))
        row.bandCombo.foreach(add(_, new GridBagConstraints {
          gridx = 2
          gridy = y
          insets = new Insets(0, 0, 5, 5)
          fill = GridBagConstraints.HORIZONTAL
        }))
        row.systemCombo.foreach(add(_, new GridBagConstraints {
          gridx = 3
          gridy = y
          insets = new Insets(0, 0, 5, 0)
          fill = GridBagConstraints.HORIZONTAL
        }))
      }
      add(new JPanel, new GridBagConstraints {
        gridx = 10
        gridy = rows.length
        weightx = 1.0
        weighty = 1.0
        fill = GridBagConstraints.BOTH
      })
    }

  val scroll: JScrollPane =
    new JScrollPane(content, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER) {
      setMaximumSize(new Dimension(215, 1))
      setPreferredSize(new Dimension(215, 1))
      setMinimumSize(new Dimension(215, 1))
      setBorder(BorderFactory.createEmptyBorder(0,0,0,0))
    }

  val getComponent: JPanel =
    new JPanel(new GridBagLayout) {
      add(scroll, new GridBagConstraints {
        gridx = 0
        gridy = 0
        fill = GridBagConstraints.VERTICAL
        weightx = 0.0
        weighty = 1.0
        anchor = GridBagConstraints.WEST
      })
    }

  def edit(ctx: GOption[ObsContext], target: SPTarget, node: ISPNode): Unit =
    if (this.target != target) {
      if (this.target != null) this.target.deleteWatcher(watcher)
      if (target != null) target.addWatcher(watcher)
      reinit(target)
    }

  private def reinit(target: SPTarget): Unit =
    reinit(target,
      if (target != null && Target.magnitudes.get(target.getTarget).forall(_.isEmpty)) Mode.Add
      else Mode.Edit
    )

  private def reinit(target: SPTarget, mode: Mode): Unit = {
    this.target = target
    rows.foreach(_.setTarget(target, mode))
    if (mode == Mode.Add) {
      // Scroll to the bottom to show the new row in the scroll pane, but don't do it in this event
      // cycle.  Wait until this event has finished executing so that the widgets for adding a new
      // magnitude value are visible.
      SwingUtilities.invokeLater(new Runnable {
        def run(): Unit = {
          val sb = scroll.getVerticalScrollBar
          sb.setValue(sb.getMaximum)
          if (Target.magnitudes.get(target.getTarget).exists(_.nonEmpty))
            newRow.bandCombo.foreach(_.requestFocusInWindow)
        }
      })
    }
    getComponent.getParent.repaint()
  }

  private def focusOn(b: MagnitudeBand): Unit =
    for {
      r <- rows.find(_.magnitudeBand === Some(b)) if getComponent.isVisible
      t <- r.textField
    } t.requestFocusInWindow()

  private def cancelAdd(): Unit =
    reinit(target)

  private def enableAdd(): Unit =
    reinit(target, Mode.Add)

  private def addBand(b: MagnitudeBand): Unit = {
    target.setTarget(Target.magnitudes.mod({ ms =>
      new Magnitude(0.0, b) :: ms.filterNot(_.band == b)
    }, target.getTarget))
    focusOn(b)
  }

  private def removeBand(b: MagnitudeBand): Unit =
    target.setTarget(Target.magnitudes.mod(_.filterNot(_.band == b), target.getTarget))

  private def modifyMagnitudeWithBand(b: MagnitudeBand, f: Magnitude => Magnitude): Unit = {
    target.setTarget(Target.magnitudes.mod(_.map { m =>
      if (m.band == b) f(m) else m
    }, target.getTarget))
    focusOn(b)
  }

  private def changeBand(from: MagnitudeBand, to: MagnitudeBand): Unit =
    modifyMagnitudeWithBand(from, Magnitude.band.set(_, to))

  private def changeSystem(band: MagnitudeBand, system: MagnitudeSystem): Unit =
    modifyMagnitudeWithBand(band, Magnitude.system.set(_, system))

  private def updateMagnitudeValue(b: MagnitudeBand, d: Double): Unit = {
    target.deleteWatcher(watcher)
    modifyMagnitudeWithBand(b, Magnitude.value.set(_, d))
    target.addWatcher(watcher)
  }

}