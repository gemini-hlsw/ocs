package edu.gemini.catalog.osgi

import jsky.catalog.skycat.SkycatConfigFile
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import java.util.logging.Logger

final class Activator extends BundleActivator {
  private val LOG = Logger.getLogger(classOf[Activator].getName)

  def start(ctx: BundleContext) {
    LOG.info("start edu.gemini.catalog")
    val url = ctx.getBundle.getEntry("/jsky/catalog/osgi/skycat.cfg")
    SkycatConfigFile.setConfigFile(url)
  }

  def stop(bundleContext: BundleContext) {
    LOG.info("stop edu.gemini.catalog")
  }
}