package jsky.app.ot.osgi

import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.pot.client.SPDB
import edu.gemini.sp.vcs.reg.VcsRegistrar
import edu.gemini.sp.vcs2.Vcs
import edu.gemini.util.osgi.Tracker._
import edu.gemini.util.osgi.ExternalStorage
import edu.gemini.util.security.auth.keychain.KeyChain
import edu.gemini.util.security.auth.keychain.Action._
import edu.gemini.util.security.auth.ui.PasswordDialog
import jsky.app.ot.OT
import jsky.app.ot.gemini.obscat.OTBrowserPresetsPersistence
import jsky.app.ot.plugin.{OtActionPlugin, OtViewerService}
import jsky.app.ot.vcs.VcsOtClient
import jsky.app.ot.viewer.ViewerService
import org.osgi.framework.{BundleActivator, BundleContext, ServiceRegistration}
import org.osgi.util.tracker.ServiceTracker
import jsky.app.ot.viewer.plugin.PluginRegistry
import java.util.logging.Logger
import javax.swing.{JOptionPane, SwingUtilities}

import edu.gemini.catalog.image.ImageCacheWatcher

import scala.swing.Swing

class Activator extends BundleActivator {
  private val LOG = Logger.getLogger(getClass.getName)

  private var tracker: ServiceTracker[_,_] = null
  private var pluginTracker: ServiceTracker[OtActionPlugin, OtActionPlugin] = null

  override def start(ctx: BundleContext) {
    tracker = track[IDBDatabaseService, Vcs, VcsRegistrar, KeyChain, ServiceRegistration[OtViewerService]](ctx) { (odb, vcs, reg, auth) =>
      SPDB.init(odb)
      VcsOtClient.ref  = Some(VcsOtClient(vcs, reg))

      val storage = ExternalStorage.getExternalDataRoot(ctx)

      SwingUtilities.invokeLater(new Runnable {
        def run() {
          // Initialize the image cache
          ImageCacheWatcher.run()

          // Prompt for password
          if (auth.isLocked.unsafeRunAndThrow) {
            PasswordDialog.unlock(auth, null)
            if (auth.isLocked.unsafeRunAndThrow)
              System.exit(0)
          }

          // Load the magnitude table.
          val magTable = ProbeLimitsTable.load().fold(magTableError, identity)

          val start = System.currentTimeMillis
          LOG.info("Starting the OT as %s".format(auth.subject))
          OT.open(auth, magTable, reg, storage)
          LOG.info("Call to OT.open() took %d ms.".format(System.currentTimeMillis - start))

          OTBrowserPresetsPersistence.dir = Some(ExternalStorage.getExternalDataRoot(ctx))
          OTBrowserPresetsPersistence.load()
        }
      })

      ViewerService.instance = Some(new ViewerService(odb, reg))
      ctx.registerService(classOf[OtViewerService], ViewerService.instance.get, new java.util.Hashtable[String, Any])
    } { viewerReg =>
      viewerReg.unregister()
      SPDB.clear()
      VcsOtClient.ref  = None
    }
    tracker.open()

    implicit def regop(p: OtActionPlugin) = new Object {
      private def onEdt(f: OtActionPlugin => Unit) = Swing.onEDT(f(p))
      def register()   = onEdt(PluginRegistry.add)
      def unRegister() = onEdt(PluginRegistry.remove)
    }

    pluginTracker = track[OtActionPlugin, OtActionPlugin](ctx) { plugin =>
      plugin.register()
      plugin
    } { _.unRegister() }
    pluginTracker.open()
  }

  private def magTableError(msg: String): Nothing = {
    LOG.severe(msg)
    JOptionPane.showMessageDialog(null, "Could not load magnitude table:\n" + msg, "Magnitude Table Load Failure", JOptionPane.ERROR_MESSAGE)
    scala.sys.exit(0)
  }

  override def stop(ctx: BundleContext) {
    LOG.info("Stop jsky.app.ot")
    tracker.close()
    tracker = null
  }
}
