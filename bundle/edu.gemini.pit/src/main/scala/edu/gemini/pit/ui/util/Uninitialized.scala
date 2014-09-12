package edu.gemini.pit.ui.util

import javax.swing.JLabel
import swing.{ListView, ComboBox}
import scalaz.Lens

import scala.language.reflectiveCalls

/**
 * Support for uninitialized combo box selections.  JComboBox uses null to
 * indicate no selection.  So render "null" as "Select" and xmap the raw
 * lens with Option value to/from null.
 */
object Uninitialized {

  trait ValueRenderer[A <: {def value():String}] { this:ComboBox[A] =>
    renderer = new ListView.Renderer[A] {
      val delegate = renderer
      def componentFor(list: ListView[_], isSelected: Boolean, focused: Boolean, a: A, index: Int) = {
        val c = delegate.componentFor(list, isSelected, focused, a, index)
        val t = Option(a) map { _.value() } getOrElse "Select"
        c.peer.asInstanceOf[JLabel].setText(t)
        c
      }
    }
  }

  def lens[A, B](optionLens: Lens[A, Option[B]])(implicit ev: Null <:< B): Lens[A, B] =
    optionLens.xmapB(_.orNull)(c => Option(c))

}