package edu.gemini.pit.ui.editor

import edu.gemini.model.p1.immutable.EphemerisElement
import edu.gemini.pit.ui.util._
import edu.gemini.shared.gui.textComponent.NumberField
import edu.gemini.spModel.core.Coordinates

import scala.swing._
import scala.swing.event.ValueChanged
import java.util.{Date, TimeZone}
import javax.swing.{JSpinner, SpinnerDateModel}

import scalaz._
import Scalaz._

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
  object RA extends RATextField(e.coords.ra)
  object Dec extends DecTextField(e.coords.dec)
  object Mag extends NumberField(e.magnitude, allowEmpty = false)

  // Construct our editor
  def editor = Editor

  // Construct a new value
  def value = EphemerisElement(Coordinates(RA.toRightAscension, Dec.value), parseDouble(Mag.text).toOption, Cal.getValue.asInstanceOf[Date].getTime)

}

