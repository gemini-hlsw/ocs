package edu.gemini.pit.ui.editor


import edu.gemini.model.p1.immutable.DegDeg
import edu.gemini.model.p1.immutable.EphemerisElement

import edu.gemini.pit.ui.util._

import scala.swing._
import scala.swing.event.ValueChanged
import java.util.{TimeZone, Date}
import javax.swing.{JSpinner, SpinnerDateModel}

/**
 * Modal editor for an EphemerisElement.
 */
class EphemerisElementEditor(e: EphemerisElement) extends StdModalEditor[EphemerisElement]("Edit Ephemeris Element") {

  // Editor component
  object Editor extends GridBagPanel with Rows {
    addRow(new Label("UTC"), Component.wrap(Cal))
    addRow(new Label("RA"), RA)
    addRow(new Label("Dec"), Dec)
    addRow(new Label("Magnitude"), Mag)
  }

  // Validation
  override def editorValid = RA.valid && Dec.valid && Mag.valid
  Seq(RA, Dec, Mag) foreach {
    _.reactions += {
      case ValueChanged(_) => validateEditor()
    }
  }

  // Data fields
  object Cal extends JSpinner(new SpinnerDateModel()) { spin =>
    setEditor(new JSpinner.DateEditor(spin, "dd-MMM-yyyy HH:mm:ss") {
      getFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
    })
    setValue(new Date(e.validAt))
  }
  object RA extends RATextField(e.coords.toDegDeg.ra.toDouble)
  object Dec extends DecTextField(e.coords.toDegDeg.dec.toDouble)
  object Mag extends NumberField(e.magnitude)

  // Construct our editor
  def editor = Editor

  implicit def pimpString(s:String) = new Object {
    def toDoubleOption = try {
      Some(s.toDouble)
    } catch {
      case _ : NumberFormatException => None
    }
  }

  // Construct a new value
  def value = EphemerisElement(DegDeg(RA.value, Dec.value), Mag.text.toDoubleOption, Cal.getValue.asInstanceOf[Date].getTime)

}

