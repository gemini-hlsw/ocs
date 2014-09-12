package edu.gemini.pit.ui.util

import swing.TextComponent
import edu.gemini.model.p1.immutable._

trait EmailText extends NonEmptyText { this:TextComponent =>
  override def valid = EmailRegex.findFirstIn(text).isDefined
}
