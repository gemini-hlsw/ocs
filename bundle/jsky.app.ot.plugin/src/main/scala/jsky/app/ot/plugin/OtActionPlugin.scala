package jsky.app.ot.plugin

import javax.swing.Icon

abstract class OtActionPlugin(val name: String) {
  def toolTip: String = ""

  def icon: Option[Icon] = None

  def enabledFor(ctx: OtContext): Boolean = true

  def apply(ctx: OtContext, window: java.awt.Window): Unit
}
