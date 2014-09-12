package jsky.app.ot.viewer.plugin

import edu.gemini.pot.sp.ISPProgramNode

import jsky.app.ot.plugin.{OtContext, OtActionPlugin}
import jsky.app.ot.userprefs.observer.ObservingPeer
import jsky.app.ot.viewer.SPViewer
import jsky.app.ot.viewer.action.AbstractViewerAction

import java.awt.event.ActionEvent
import javax.swing.{SwingUtilities, Action}
import edu.gemini.util.security.auth.keychain.KeyChain

// Wraps a OtActionPlugin to adapt it to the AbstractViewerAction
final class PluginViewerAction(keychain: KeyChain, viewer: SPViewer, plugin: OtActionPlugin) extends AbstractViewerAction(viewer, plugin.name, plugin.icon.orNull) {
  putValue(AbstractViewerAction.SHORT_NAME, plugin.name)
  putValue(Action.SHORT_DESCRIPTION, plugin.toolTip)
  setEnabled(true)

  private def currentNode: Option[ISPProgramNode] =
    Option(viewer.getNode).collect { case p: ISPProgramNode => p }

  private def otContext = OtContext(currentNode, ObservingPeer.get, keychain)

  override def computeEnabledState: Boolean = plugin.enabledFor(otContext)

  override def actionPerformed(evt: ActionEvent): Unit =
    plugin(otContext, SwingUtilities.getWindowAncestor(viewer))
}
