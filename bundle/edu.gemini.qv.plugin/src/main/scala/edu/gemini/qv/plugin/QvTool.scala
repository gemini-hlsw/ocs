package edu.gemini.qv.plugin

import java.io.File

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.qv.plugin.data.{DataChanged, OdbDataSource}
import edu.gemini.qv.plugin.selector.RefreshDialog
import edu.gemini.qv.plugin.ui.QvGui
import edu.gemini.spModel.core.Peer
import edu.gemini.util.security.auth.keychain.KeyChain
import jsky.app.ot.plugin.{OtContext, OtViewerService}
import jsky.util.gui.Resources

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.event.WindowClosing
import scala.swing.{Frame, Swing}

/**
 * Frame containing the QV tool UI elements (main panel and menu bar).
 * @param ctx
 */
class QvTool(ctx: QvContext) extends Frame {

  // -- create UI and register frame
  QvTool.viewerService.foreach(_.registerView(this))
  title = s"Queue Visualization - ${ctx.site.displayName}"
  menuBar = new QvToolMenu(this, ctx)
  contents = new QvToolPanel(ctx)

  // -- reactions
  reactions += {
    case WindowClosing(e) =>
      QvTool.viewerService.foreach(_.unregisterView(this)) // un-register frame when frame is closed
  }

  // -- show the QV tool
  pack
  Resources.setOTFrameIcon(peer)
  visible = true
}

/**
 * Make utility services used in QV available throughout the plugin.
 */
object QvTool {
  var defaultsFile: Option[File] = None
  var viewerService: Option[OtViewerService] = None
  var authClient: Option[KeyChain] = None

  def apply(ctx: OtContext) = {

    // check if we have a peer and either show an error or start QV
    // (there is nothing useful we can do without having a peer (i.e. a site) defined)
    if (ctx.observingPeer.isEmpty)
      QvGui.showError(
        "Could not start QV",
        """There is no peer for the selected observing site defined.
          |Please define the peer for the observing site and try again.""".stripMargin
      )
    else
      startQv(ctx.observingPeer.get, ctx.mt)

  }

  def apply(ctx: QvContext) =  {

    Future {

      Swing.onEDT {
        new QvTool(ctx)
        // after creating and setting up everything push a DataChanged event so that the new window
        // gets updated as if a batch of fresh data had been loaded/updated; this will also update
        // all other windows, which is unneeded, but that's ok for now.
        ctx.mainFilterProvider.publish(DataChanged)
      }

    } onFailure {
      case t =>
        QvGui.showError(
          "Could not open a new QV window",
          "An error occurred", t)
    }
  }

  private def startQv(peer: Peer, mt: MagnitudeTable) {

    Future {

      // -- load defaults, user settings and get a first bunch of data from data source
      val dataSource = new OdbDataSource(peer, mt)
      val qvCtx = QvContext(peer, dataSource, dataSource)
      QvStore.loadDefaults()


      // -- build UI
      Swing.onEDT {
        // -- set up user interface
        val tool = new QvTool(qvCtx)
        // -- let user do a first data load on demand
        new RefreshDialog(tool, qvCtx).open()
      }

    } onFailure {
      case t =>
        QvGui.showError(
          "Could not start QV",
          "An error occurred", t)
    }
  }

}

