package edu.gemini.qv.plugin.osgi

import java.io.File

import edu.gemini.qv.plugin.{QvTool, ShowQvToolAction}
import edu.gemini.shared.util.FileUtil
import edu.gemini.spModel.core.Version
import edu.gemini.util.osgi.ExternalStorage
import edu.gemini.util.osgi.ExternalStorage.MigrationStep
import edu.gemini.util.osgi.Tracker._
import edu.gemini.util.security.auth.keychain.KeyChain
import jsky.app.ot.plugin.{OtActionPlugin, OtViewerService}
import org.osgi.framework.{BundleActivator, BundleContext, ServiceRegistration}
import org.osgi.util.tracker.ServiceTracker

class Activator extends BundleActivator {
  private var reg: Option[ServiceRegistration[OtActionPlugin]] = None

  private var tracker: ServiceTracker[_,_] = null

  def start(ctx: BundleContext): Unit = {

    QvTool.defaultsFile = Option(ExternalStorage.getPermanentDataFile(ctx, Version.current.isTest, "userFilters.v1.xml", migrationSteps))

    reg = Option(ctx.registerService(classOf[OtActionPlugin], new ShowQvToolAction, new java.util.Hashtable[String, Object]()))

    tracker = track[OtViewerService,KeyChain,Unit](ctx) { (vs, authClient) =>
      QvTool.viewerService = Some(vs)
      QvTool.authClient = Some(authClient)
    } { _ =>
      QvTool.viewerService = None
      QvTool.authClient = None
    }
    tracker.open()
  }

  def stop(ctx: BundleContext): Unit = {
    reg.foreach(_.unregister())
    reg = None
    tracker.close()
    tracker = null
  }

  private val migrationSteps = List(

    // Migration from 0.0.1 to 2015A -> move file from 0.0.1 folder into bundle folder
    MigrationStep("0.0.1" + File.separator + "defaults-v4.xml", "userFilters.v1.xml", FileUtil.copy(_, _, false))

    // -- add additional migration steps as needed
  )

}
