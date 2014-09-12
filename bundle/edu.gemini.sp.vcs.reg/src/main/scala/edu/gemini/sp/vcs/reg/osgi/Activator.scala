package edu.gemini.sp.vcs.reg.osgi

import edu.gemini.sp.vcs.reg.{VcsRegistrationSubscriber, VcsRegistrar}
import edu.gemini.sp.vcs.reg.impl.VcsRegistrarImpl
import edu.gemini.util.osgi.Tracker._

import org.osgi.framework.{ServiceRegistration, BundleActivator, BundleContext}
import org.osgi.util.tracker.ServiceTracker
import java.io.File
import java.util.logging.Logger

class Activator extends BundleActivator {
  private var reg: ServiceRegistration[VcsRegistrar] = null
  private var tracker: ServiceTracker[_,_] = null
  private var vcsReg: VcsRegistrarImpl = null

  override def start(ctx: BundleContext) {
    vcsReg = new VcsRegistrarImpl(Activator.getStorageFile(ctx))
    reg = ctx.registerService(classOf[VcsRegistrar], vcsReg, null)

    tracker = track[VcsRegistrationSubscriber, VcsRegistrationSubscriber](ctx) { sub =>
      vcsReg.subscribe(sub)
      sub
    } { vcsReg.unsubscribe }
    tracker.open()
  }

  override def stop(ctx: BundleContext) {
    reg.unregister()
    reg = null
    tracker.close()
    tracker = null
  }
}

object Activator {
  private val LOG = Logger.getLogger(classOf[Activator].getName)

  private val BUNDLE_PROP_DIR = "edu.gemini.spdb.dir"

  private def defaultRegDir(ctx: BundleContext): File =
    new File(ctx.getDataFile("").getParentFile.getParentFile.getParentFile, "data/edu.gemini.sp.vcs.reg")

  private def defaultRegFile(ctx: BundleContext): File = {
    val dir = defaultRegDir(ctx)
    dir.mkdirs()
    new File(dir, "vcs-reg.xml")
  }

  private def getStorageFile(ctx: BundleContext): File = {
    val file = Option(ctx.getProperty(BUNDLE_PROP_DIR)) map { dirName =>
      new File(dirName, "vcs-reg.xml")
    } filter { f =>
      (f.exists() && f.canWrite) || (f.getParentFile.mkdirs() && f.getParentFile.canWrite)
    } getOrElse {
      defaultRegFile(ctx)
    }
    LOG.info("Using %s for vcs registration storage".format(file.getPath))
    file
  }
}
