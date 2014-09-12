package edu.gemini.spdb.cron.osgi

import org.osgi.framework.{BundleContext, BundleActivator}
import org.osgi.util.tracker.ServiceTracker
import edu.gemini.util.osgi.Tracker._
import org.osgi.service.http.HttpService
import edu.gemini.dbTools.monitor.OdbMonitor
import edu.gemini.dbTools.execHours.ExecHourFunctor
import edu.gemini.dbTools.tigratable.TigraTableCreator
import edu.gemini.util.osgi.ExternalStorage
import edu.gemini.dbTools._
import scalaz._
import Scalaz._
import edu.gemini.dbTools.odbState.OdbStateAgent
import edu.gemini.dbTools.mail.OdbMailAgent
import edu.gemini.dbTools.weather.WeatherUpdater
import edu.gemini.dbTools.archive.Archiver
import edu.gemini.util.security.principal.StaffPrincipal
import java.security.Principal

class Activator extends BundleActivator {

  val alias = "/cron"

  // We will run as Superuser
  val user = java.util.Collections.singleton[Principal](StaffPrincipal.Gemini)

  // See each service's entry point for an example invocation via curl
  def services(c: BundleContext): Map[String, Job] =
     Map("monitor"        -> OdbMonitor.monitor,
         "execHours"      -> ExecHourFunctor.run,
         "tigraTable"     -> new TigraTableCreator(c).run,
         "semesterStatus" -> semesterStatus.Driver.run,
         "odbState"       -> OdbStateAgent.run,
         "odbMail"        -> OdbMailAgent.run,
         "weather"        -> new WeatherUpdater(c).run,
         "archive"        -> Archiver.run(c))

  var tracker: ServiceTracker[HttpService, HttpService] = null

  def start(ctx: BundleContext): Unit = {
    tracker = track[HttpService, HttpService](ctx) { http =>
      val file = ExternalStorage.getExternalDataFile(ctx, "cron") <| (_.mkdirs)
      val servlet = new CronServlet(ctx, services(ctx), file, user)
      http <| (_.registerServlet(alias, servlet, null, null))
    }(_.unregister(alias))
    tracker.open()
  }

  def stop(p1: BundleContext): Unit =
    tracker.close()

}


