package edu.gemini.services.server.osgi

import Activator._
import edu.gemini.services.client.TelescopeScheduleService
import edu.gemini.services.server.util.CalendarService
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.core.osgi.SiteProperty
import java.util.logging.{Level, Logger}
import java.util.logging.Level._
import org.osgi.framework.{ServiceRegistration, BundleActivator, BundleContext}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Try, Success, Failure}

object Activator {
  val Log = Logger.getLogger(classOf[Activator].getName)
}


/**
 * Activator that registers and un-registers the services provided by this bundle.
 */
class Activator extends BundleActivator {

  var telescopeScheduleService: Option[ServiceRegistration[TelescopeScheduleService]] = None

  def start(ctx: BundleContext): Unit = {

    val site = SiteProperty.get(ctx)
    Log.info(s"Starting services bundle for site $site.")
    
    // register the services..
    registerTelescopeSchedule(ctx, site)

  }

  def stop(ctx: BundleContext): Unit = {

    Log.info("Stopping services bundle.")

    // unregister the services..
    telescopeScheduleService.foreach(_.unregister())

  }

  /** Tries to register the telescope schedule service. */
  private def registerTelescopeSchedule(ctx: BundleContext, site: Site) {
    
    // only try to create services if we know the site
    if (site != null) {

      Future {

        Log.info("Registering telescope schedule service.")

        // For some reason the Google authentication sometimes does not work reliably on the first attempt.
        // In order to cover for this we retry several times with an increasing wait time before finally giving up.

        retry("telescope schedule", 10, 5) {

          val cs = CalendarService.calendarService(ctx)                    // calendar service to be used
          val service = CalendarService.telescopeScheduleService(ctx, cs)  // the telescope schedule service

          val properties = new java.util.Hashtable[String, Object]()
          properties.put("trpc", "")                                    // publish as TRPC services

          val registeredService = ctx.registerService(classOf[TelescopeScheduleService], service, properties)
          telescopeScheduleService = Some(registeredService)
        }

      } onComplete {
        case Success(_) => Log.info("Successfully registered telescope schedule service.")
        case Failure(t) => Log.log(SEVERE, "Registration of telescope schedule service failed.", t)
      }

    } else {

      Log.log(Level.SEVERE, "Can not register telescope schedule service if target site is unknown.")
    }

  }

  /** Simple retry mechanism. */
  private def retry[T](name: String, n: Int, wait: Long)(fn: => T): T = {
    Try { fn } match {
      case Success(x) => x
      case Failure(e) if n > 1 =>
        Log.log(WARNING, s"Attempt to register $name service failed, retrying ${n-1} more times in $wait seconds.", e)
        Thread.sleep(wait * 1000)
        // wait for double the time before next attempt, but don't wait for more than 3 minutes
        retry(name, n - 1, if (wait*2 < 180) wait * 2 else 180)(fn)
      case Failure(e) =>
        Log.log(WARNING, s"Last attempt registering $name service failed. Giving up!")
        throw e
    }
  }

}
