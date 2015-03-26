package jsky.app.ot.gemini.editor.targetComponent

import javax.swing.BorderFactory._
import javax.swing.border.Border

package object details {

  /** Create a titled border with inner and outer padding. */
  def titleBorder(title: String): Border =
    createCompoundBorder(
      createEmptyBorder(2,2,2,2),
      createCompoundBorder(
        createTitledBorder(title),
        createEmptyBorder(2,2,2,2)))

}
