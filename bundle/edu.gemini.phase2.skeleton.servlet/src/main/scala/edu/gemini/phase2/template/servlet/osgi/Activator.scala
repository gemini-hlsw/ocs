package edu.gemini.phase2.template.servlet.osgi

import edu.gemini.phase2.template.factory.api.TemplateFactory
import edu.gemini.phase2.template.servlet.TemplateServlet
import edu.gemini.util.osgi.Tracker._

import org.osgi.framework.{BundleActivator, BundleContext}
import org.osgi.service.http.HttpService
import org.osgi.util.tracker.ServiceTracker

import java.util.Hashtable
import java.util.logging.Logger

object Activator {
  val LOG         = Logger.getLogger(getClass.getName)
  val APP_CONTEXT = "/template"
}

import Activator._

class Activator extends BundleActivator {
  private var serv: ServiceTracker[_,_] = null

  def start(ctx: BundleContext) {
    LOG.info("Start Template Servlet")
    serv = track[TemplateFactory, HttpService, HttpService](ctx) { (tfactory, http) =>
      http.registerServlet(APP_CONTEXT, new TemplateServlet(tfactory), new Hashtable(), null)
      http
    } { _.unregister(APP_CONTEXT) }
    serv.open()
  }

  def stop(ctx: BundleContext) {
    LOG.info("Stop Template Servlet")
    serv.close()
    serv = null
  }
}
