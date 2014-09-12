package edu.gemini.rollover.servlet.osgi

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.rollover.servlet.RolloverServlet
import edu.gemini.spModel.core.Site
import edu.gemini.util.osgi.Tracker._

import org.osgi.framework.{BundleContext, BundleActivator}
import org.osgi.service.http.HttpService
import org.osgi.util.tracker.ServiceTracker

import java.util.Hashtable
import java.util.logging.Logger
import java.security.Principal
import edu.gemini.util.security.principal.StaffPrincipal

object Activator {
  val LOG           = Logger.getLogger(getClass.getName)
  val APP_CONTEXT   = "/rollover"
  val SITE_PROPERTY = "edu.gemini.site"
}

import Activator._

class Activator extends BundleActivator {

  private var serv: ServiceTracker[_,_] = null

  // We will run this as the superuser
  val user = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)

  def start(ctx: BundleContext) {
    LOG.info("Start Rollover Servlet")
    val site = Site.parse(ctx.getProperty(SITE_PROPERTY))
    serv = track[IDBDatabaseService, HttpService, HttpService](ctx) { (odb, http) =>
      http.registerServlet(APP_CONTEXT, new RolloverServlet(site, odb, user), new Hashtable(), null)
      http
    } { _.unregister(APP_CONTEXT) }
    serv.open()
  }

  def stop(ctx: BundleContext) {
    LOG.info("Stop Rollover Servlet")
    serv.close()
    serv = null
  }
}