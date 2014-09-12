package edu.gemini.p1monitor.osgi

import org.osgi.framework.{ServiceReference, BundleContext, BundleActivator}
import org.osgi.util.tracker.ServiceTrackerCustomizer
import org.osgi.service.http.{NamespaceException, HttpService}
import java.util
import edu.gemini.p1monitor.fetch.FetchServlet
import edu.gemini.p1monitor.P1MonitorDirMonitor
import util.Hashtable
import util.logging.{Level, Logger}
import edu.gemini.p1monitor.config.P1MonitorConfig
import javax.servlet.ServletException

class Activator extends BundleActivator {
  val LOG = Logger.getLogger(this.getClass.getName)

  var monitor: P1MonitorDirMonitor = _
  var httpRef: ServiceReference[HttpService] = _

  override def start(context: BundleContext) {
    val config: P1MonitorConfig = new P1MonitorConfig(context)
    monitor = new P1MonitorDirMonitor(config)

    httpRef = context.getServiceReference(classOf[HttpService])
    val http: HttpService = context.getService(httpRef)

    try {
      http.registerServlet("/fetch", new FetchServlet(config), new Hashtable(), null)
    } catch {
      case ex: ServletException => LOG.log(Level.SEVERE, "Trouble setting up web application.", ex)
      case ex: NamespaceException => LOG.log(Level.SEVERE, "Trouble setting up web applicaiton.", ex)
    }

    monitor.startMonitoring()

  }

  override def stop(context: BundleContext) {
    monitor.stopMonitoring()
    context.ungetService(httpRef)
  }

}