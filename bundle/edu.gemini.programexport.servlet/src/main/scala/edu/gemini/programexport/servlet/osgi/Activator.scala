package edu.gemini.programexport.servlet.osgi

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.programexport.servlet.ProgramExportServlet
import edu.gemini.util.osgi.Tracker._
import edu.gemini.util.security.principal.StaffPrincipal

import org.osgi.framework.{BundleActivator, BundleContext}
import org.osgi.service.http.HttpContext
import org.osgi.service.http.HttpService
import org.osgi.util.tracker.ServiceTracker


import java.net.URL
import java.util.logging.Logger
import java.util.{Hashtable => JHashtable, Set => JSet}
import java.security.Principal
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import scala.collection.JavaConversions._
import scalaz.\/


object Activator {
  val Log: Logger = Logger.getLogger(getClass.getName)
  val AppContext = "/programexport"
}

import Activator._

final class Activator extends BundleActivator {

  private var tracker: ServiceTracker[_,_] = null

  // Run as superuser.
  val user: JSet[Principal] = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)

  def start(ctx: BundleContext): Unit = {

    // Extract the HTTP (internal) port number if set in the bundle property.
    val internalPort = \/.fromTryCatchNonFatal {
      Option(ctx.getProperty("org.osgi.service.http.port")).getOrElse("8442").toInt
    }.toOption.getOrElse(8442)

    tracker = track[IDBDatabaseService, HttpService, HttpService](ctx) { (odb, http) =>
      val defaultContext = http.createDefaultHttpContext();
      http.registerServlet(AppContext, ProgramExportServlet(odb, user.toSet), new JHashtable(),
        new HttpContext() {
          override def handleSecurity(req: HttpServletRequest, res: HttpServletResponse): Boolean = {
            val result = defaultContext.handleSecurity(req, res) && (req.getServerPort == internalPort)
            if (!result) res.setStatus(HttpServletResponse.SC_FORBIDDEN)
            result
          }

          override def getResource(s: String): URL =
            defaultContext.getResource(s)

          override def getMimeType(s: String): String =
            defaultContext.getMimeType(s)
        }
      )
      http
    } { http =>
      http.unregister(AppContext)
    }

    tracker.open()
  }

  def stop(ctx: BundleContext): Unit = {
    tracker.close()
    tracker = null
  }
}