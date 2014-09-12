package edu.gemini.qv.plugin

import jsky.app.ot.plugin.OtActionPlugin

import jsky.app.ot.plugin.OtContext

/**
 * Implementation of an OT plugin action that opens a new QV tool.
 */
class ShowQvToolAction extends OtActionPlugin("Queue Visualization") {

  override val toolTip = "Open the queue visualization tool."

  override def enabledFor(ctx: OtContext): Boolean = ctx.observingPeer.isDefined

  override def apply(ctx: OtContext, window: java.awt.Window): Unit = QvTool(ctx)
}