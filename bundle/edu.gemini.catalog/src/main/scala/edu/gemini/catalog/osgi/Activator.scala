package edu.gemini.catalog.osgi

import edu.gemini.catalog.skycat.binding.skyobj.Votable2SkyCatalogServlet
import jsky.catalog.skycat.SkycatConfigFile
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import java.util.logging.Logger
import edu.gemini.util.osgi.Tracker._
import org.osgi.service.http.HttpService
import org.osgi.util.tracker.ServiceTracker

final class Activator extends BundleActivator {
  @Deprecated
  private val SERVLET_CONTEXT = "/votable"
  @Deprecated
  private val servlet = new Votable2SkyCatalogServlet()
  @Deprecated
  var servletTracker: Option[ServiceTracker[HttpService, HttpService]] = None

  private val LOG = Logger.getLogger(classOf[Activator].getName)

  def start(ctx: BundleContext) {
    LOG.info("start edu.gemini.catalog")
    val url = ctx.getBundle.getEntry("/jsky/catalog/osgi/skycat.cfg")
    SkycatConfigFile.setConfigFile(url)

    servletTracker = Some(track[HttpService, HttpService](ctx) { h =>
      h.registerServlet(SERVLET_CONTEXT, servlet, new java.util.Hashtable[String, String](), null)
      h
    } { h =>
      h.unregister(SERVLET_CONTEXT)
    })
    servletTracker.map(_.open)
  }

  def stop(bundleContext: BundleContext) {
    LOG.info("stop edu.gemini.catalog")
    servletTracker.map(_.close)
  }
}