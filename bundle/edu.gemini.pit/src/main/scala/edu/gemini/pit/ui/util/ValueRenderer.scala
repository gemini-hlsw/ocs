package edu.gemini.pit.ui.util

import edu.gemini.shared.gui.textComponent.TextRenderer

import swing.ComboBox

import scala.language.reflectiveCalls

// A mixin for showing the value rather than the toString
trait ValueRenderer[A <: {def value():String}] extends TextRenderer[A] { this:ComboBox[A] =>
  def text(a:A):String = a.value()
}

