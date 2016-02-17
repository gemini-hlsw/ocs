package edu.gemini.sp.vcs.log.osgi

import org.osgi.framework.{BundleActivator, BundleContext}
import edu.gemini.sp.vcs.log.VcsLog
import edu.gemini.util.osgi.ExternalStorage.getExternalDataFile
import java.io.File
import java.util.logging.Logger
import edu.gemini.spModel.core.{OcsVersionUtil, Version}

object Activator {
  val BUNDLE_PROP_DIR = "edu.gemini.spdb.dir" // Same location as the SPDB
  val LOG = Logger.getLogger(classOf[Activator].getName)
}

class Activator extends BundleActivator {

  import Activator._

  def start(ctx: BundleContext) {
    val root:File = Option(ctx.getProperty(BUNDLE_PROP_DIR)).fold(getExternalDataFile(ctx, "spdb"))(new File(_))
    val file:File = new File(OcsVersionUtil.getVersionDir(root, Version.current), "vcs")
    file.mkdirs()
    LOG.info(s"VCS log storage is at ${file.getAbsolutePath}")
    ctx.registerService(classOf[VcsLog], VcsLog(file).unsafePerformIO, null)
  }

  def stop(ctx: BundleContext) {
  }

}

