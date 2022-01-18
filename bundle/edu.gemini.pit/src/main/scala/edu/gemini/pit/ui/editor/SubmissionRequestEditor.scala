package edu.gemini.pit.ui.editor

import edu.gemini.pit.ui.view.partner.PartnersFlags
import edu.gemini.model.p1.immutable._
import edu.gemini.shared.gui.textComponent.NumberField
import swing._
import event.ValueChanged
import Swing._
import edu.gemini.pit.ui.util.{ValueRenderer, Rows, StdModalEditor}
import scalaz._
import Scalaz._
import java.awt.BorderLayout

object SubmissionRequestEditor {

  def open[A](s:SubmissionRequest, partner:Option[A], is:List[Investigator], lead:Option[Investigator], parent:UIElement) = new SubmissionRequestEditor(s, partner, lead, is).open(parent)

  class TimeEditor(tu: Option[TimeAmount]) extends BorderPanel {

    border = null
    add(Time, BorderPanel.Position.Center)
    add(Units, BorderPanel.Position.East)

    object Time extends NumberField(tu.map(_.value), allowEmpty = false, format = NumberField.TimeFormatter) {
      override def valid(d:Double) = d >= 0
    }

    object Units extends ComboBox(TimeUnit.values.toList) with ValueRenderer[TimeUnit] {
      tu.map(_.units).foreach(selection.item_=)
    }

    def value = TimeAmount(Time.text.toDouble, Units.selection.item)

    override def enabled_=(b:Boolean) {
      super.enabled = b
      Time.enabled = b
      Units.enabled = b
    }
    
  }

}

class SubmissionRequestEditor[A] private (s:SubmissionRequest, partner:Option[A], lead:Option[Investigator], is:List[Investigator]) extends StdModalEditor[(SubmissionRequest, Investigator, Boolean)]("Edit Request") {

  import SubmissionRequestEditor._

  // Lenses
  val timeLens = SubmissionRequest.time
  val minTimeLens = SubmissionRequest.minTime

  // Validation
  private val validationComponents = Seq(editor.time, editor.minTime).map(_.Time)
  override def editorValid = remove.selected || validationComponents.forall(_.valid)
  validationComponents.foreach {
    _.reactions += {
      case ValueChanged(_) => validateEditor()
    }
  }

  // Add a "remove" box (we have to use the peer directly because add is protected in Scala)
  partner match {
    case Some(_: NgoPartner) => Contents.Footer.peer.add(remove.peer, BorderLayout.CENTER)
    case _ => ()
  }

  object remove extends CheckBox("Remove Partner") {
    horizontalAlignment = Alignment.Center
    reactions += {
      case _ =>
        Seq(editor.time, editor.minTime, editor.leads) foreach { _.enabled = !selected }
        validateEditor()
    }
  }

  // Editor component
  object editor extends GridBagPanel with Rows {

    if (partner.isDefined) {
      addRow(new Label("Partner:"), partnerLabel)
      if (is.nonEmpty)
        addRow(new Label("Partner Lead:"), leads)
      addSpacer()
    }
    addRow(new Label("Time:"), time)
    addRow(new Label("Min Time:"), minTime)

    object partnerLabel extends Label {
      text = ~partner.map(Partners.name)
      icon = partner.map(PartnersFlags.flag).orNull
      horizontalAlignment = Alignment.Left
    }

    object time extends TimeEditor(s.time.some)

    object minTime extends TimeEditor(s.minTime.some)

    object leads extends ComboBox(is) {
      lead.filter(is.contains).foreach(selection.item = _)
    }

    preferredSize = (300, preferredSize.height) // force width

  }

  def value: (SubmissionRequest, Investigator, Boolean) = {
    val s0 = timeLens.set(s, editor.time.value)
    val s1 = minTimeLens.set(s0, editor.minTime.value)
    // REL-2032 Consider a request with 0 times the same as remove
    val removed = (s1.time == TimeAmount.empty && s1.minTime == TimeAmount.empty) || remove.selected
    (s1, editor.leads.selection.item, removed)
  }

}

object LargeSubmissionRequestEditor {

  def open[A](s:SubmissionRequest, parent:UIElement): Option[SubmissionRequest] = new LargeSubmissionRequestEditor(s).open(parent)
}

class LargeSubmissionRequestEditor[A] private (s:SubmissionRequest) extends StdModalEditor[SubmissionRequest]("Edit Request") {

  import SubmissionRequestEditor._

  // Lenses
  val timeLens = SubmissionRequest.time
  val minTimeLens = SubmissionRequest.minTime
  val totalLPTimeLens = SubmissionRequest.totalLPTime
  val minTotalLPTimeLens = SubmissionRequest.minTotalLPTime

  // Editor component
  object editor extends GridBagPanel with Rows {

    addRow(new Label("Time First Semester:"), time)
    addRow(new Label("Min Time First Semester:"), minTime)
    addRow(new Label("Total Large Program Time:"), totalLPTime)
    addRow(new Label("Min Large Program Time:"), minTotalLPTime)

    object time extends TimeEditor(s.time.some)

    object minTime extends TimeEditor(s.minTime.some)

    object totalLPTime extends TimeEditor(s.totalLPTime)

    object minTotalLPTime extends TimeEditor(s.minTotalLPTime)

    preferredSize = (300, preferredSize.height) // force width

  }

  def value = {

    val s0 = timeLens.set(s, editor.time.value)
    val s1 = minTimeLens.set(s0, editor.minTime.value)
    val s2 = totalLPTimeLens.set(s1, Some(editor.totalLPTime.value))
    val s3 = minTotalLPTimeLens.set(s2, Some(editor.minTotalLPTime.value))
    s3
  }


}