package jsky.app.ot.osgi

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.pot.client.SPDB
import edu.gemini.sp.vcs.reg.{VcsRegistrationSubscriber, VcsRegistrar}
import edu.gemini.util.osgi.Tracker._
import edu.gemini.util.osgi.ExternalStorage
import edu.gemini.util.security.auth.keychain.KeyChain
import edu.gemini.util.security.auth.keychain.Action._
import edu.gemini.util.security.auth.ui.PasswordDialog

import jsky.app.ot.OT
import jsky.app.ot.gemini.obscat.CatalogQueryHistory
import jsky.app.ot.plugin.{OtActionPlugin, OtViewerService}
import jsky.app.ot.vcs.VcsGui
import jsky.app.ot.viewer.ViewerService

import org.osgi.framework.{ServiceRegistration, BundleContext, BundleActivator}
import org.osgi.util.tracker.ServiceTracker

import jsky.app.ot.viewer.plugin.PluginRegistry

import java.util.Hashtable
import java.util.logging.Logger
import javax.swing.SwingUtilities

import scala.swing.Swing

class Activator extends BundleActivator {
  private val LOG = Logger.getLogger(getClass.getName)

  private var tracker: ServiceTracker[_,_] = null
  private var vcsSubReg: ServiceRegistration[VcsRegistrationSubscriber] = null
  private var pluginTracker: ServiceTracker[OtActionPlugin, OtActionPlugin] = null

  override def start(ctx: BundleContext) {
    tracker = track[IDBDatabaseService, VcsRegistrar, KeyChain, ServiceRegistration[OtViewerService]](ctx) { (odb, reg, auth) =>
      SPDB.init(odb)
      VcsGui.registrar = Some(reg)

      val storage = ExternalStorage.getExternalDataRoot(ctx)

      SwingUtilities.invokeLater(new Runnable {
        def run() {

          // Prompt for password
          if (auth.isLocked.unsafeRunAndThrow) {
            PasswordDialog.unlock(auth, null)
            if (auth.isLocked.unsafeRunAndThrow)
              System.exit(0)
          }

          val start = System.currentTimeMillis
          LOG.info("Starting the OT as %s".format(auth.subject))
          OT.open(auth, reg, storage)
          LOG.info("Call to OT.open() took %d ms.".format(System.currentTimeMillis - start))

          CatalogQueryHistory.load(ExternalStorage.getExternalDataRoot(ctx))
        }
      })

      ViewerService.instance = Some(new ViewerService(odb, reg))
      ctx.registerService(classOf[OtViewerService], ViewerService.instance.get, new Hashtable[String, Any])

    } { viewerReg =>
      viewerReg.unregister()
      SPDB.clear()
      VcsGui.registrar = None
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

    vcsSubReg = ctx.registerService(classOf[VcsRegistrationSubscriber], VcsGui, new Hashtable[String, Any])
  }

  override def stop(ctx: BundleContext) {
    LOG.info("Stop jsky.app.ot")
    CatalogQueryHistory.save(ExternalStorage.getExternalDataRoot(ctx))
    vcsSubReg = null
    tracker.close()
    tracker = null
  }
}
