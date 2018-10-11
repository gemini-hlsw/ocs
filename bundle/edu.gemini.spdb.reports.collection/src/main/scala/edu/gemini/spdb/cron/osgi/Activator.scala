package edu.gemini.spdb.cron.osgi

import edu.gemini.dbTools.ephemeris.{EphemerisPurgeCron, TcsEphemerisCron}
import edu.gemini.dbTools.maskcheck.MaskCheckCron
import edu.gemini.dbTools.timingwindowcheck.TimingWindowCheckCron
import edu.gemini.spModel.core.Version
import edu.gemini.spdb.cron.Storage
import Storage.{Perm, Temp}
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

  // Some old activators that we delegate to
  var oldActivators: List[BundleActivator] = Nil

  // See each service's entry point for an example invocation via curl
  def services(c: BundleContext): Map[String, Job] =
     Map("maskcheck"         -> MaskCheckCron.run(c),
         "monitor"           -> OdbMonitor.monitor,
         "execHours"         -> ExecHourFunctor.run,
         "tigraTable"        -> new TigraTableCreator(c).run,
         "semesterStatus"    -> semesterStatus.Driver.run,
         "odbState"          -> OdbStateAgent.run,
         "odbMail"           -> OdbMailAgent.run,
         "weather"           -> new WeatherUpdater(c).run,
         "archive"           -> Archiver.run(c),
         "ephemeris"         -> TcsEphemerisCron.run(c),
         "ephemerisPurge"    -> EphemerisPurgeCron.run(c),
         "timingWindowCheck" -> TimingWindowCheckCron.run(c))

  var tracker: ServiceTracker[HttpService, HttpService] = null

  def start(ctx: BundleContext): Unit = {

    // The old reports activators
    oldActivators = List(
      new edu.gemini.spdb.reports.osgi.Activator,
      new edu.gemini.weather.impl.Activator,
      new edu.gemini.epics.impl.Activator,
      new edu.gemini.spdb.reports.collection.osgi.Activator
    )
    oldActivators.foreach(_.start(ctx))

    // Cron
    tracker = track[HttpService, HttpService](ctx) { http =>
      val temp = ExternalStorage.getExternalDataFile(ctx, "cron") <| (_.mkdirs)
      val perm = ExternalStorage.getPermanentDataFile(ctx, Version.current.isTest, "cron", Nil) <| (_.mkdirs)
      val servlet = new CronServlet(ctx, services(ctx), Temp(temp), Perm(perm), user)
      http <| (_.registerServlet(alias, servlet, null, null))
    }(_.unregister(alias))
    tracker.open()

  }

  def stop(ctx: BundleContext): Unit = {

    // Cron
    tracker.close()

    // Old activators
    oldActivators.foreach(_.stop(ctx))
    oldActivators = Nil

  }

}


