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
  val ServiceStart = "edu.gemini.services.server.start"
  val Log = Logger.getLogger(classOf[Activator].getName)
}


/**
 * Activator that registers and un-registers the services provided by this bundle.
 */
class Activator extends BundleActivator {

  private var telescopeScheduleService: Option[ServiceRegistration[TelescopeScheduleService]] = None

  private def recordRegistration(reg: ServiceRegistration[TelescopeScheduleService]): Unit =
    synchronized {
      telescopeScheduleService = Some(reg)
    }

  private def unregister(): Unit =
    synchronized {
      telescopeScheduleService.foreach(_.unregister())
      telescopeScheduleService = None
    }


  def start(ctx: BundleContext): Unit = {
    val site = SiteProperty.get(ctx)
    Log.info(s"Starting services bundle for site $site.")

    if ((site != null) && Option(ctx.getProperty(ServiceStart)).forall(_.toLowerCase == "true")) {
      // register the services..
      registerTelescopeSchedule(ctx, site)
    } else {
      Log.warning(s"Skipping services start.  Set '${SiteProperty.NAME}' to 'gn' or 'gs' and '$ServiceStart' to 'true' in bundle properties to enable.")
    }
  }

  def stop(ctx: BundleContext): Unit = {

    Log.info("Stopping services bundle.")

    // unregister the services..
    unregister()
  }

  /** Tries to register the telescope schedule service. */
  private def registerTelescopeSchedule(ctx: BundleContext, site: Site) {
    val registerTask = new Runnable() {
      override def run() {
        Log.info("Registering telescope schedule service.")

        // For some reason the Google authentication sometimes does not work reliably on the first attempt.
        // In order to cover for this we retry several times with an increasing wait time before finally giving up.
        retry("telescope schedule", 10, 5) {
          val cs = CalendarService.calendarService(ctx)                    // calendar service to be used
          val service = CalendarService.telescopeScheduleService(ctx, cs)  // the telescope schedule service

          val properties = new java.util.Hashtable[String, Object]()
          properties.put("trpc", "")                                       // publish as TRPC services

          ctx.registerService(classOf[TelescopeScheduleService], service, properties)
        } match {
          case Success(reg) =>
            Log.info("Successfully registered telescope schedule service.")
            recordRegistration(reg)
          case Failure(t)   =>
            Log.log(SEVERE, "Registration of telescope schedule service failed.", t)
        }
      }
    }

    val t = new Thread(registerTask, "Google Service Registration")
    t.setDaemon(true)
    t.setPriority(Thread.NORM_PRIORITY - 1)
    t.start()
  }

  /** Simple retry mechanism. */
  private def retry[T](name: String, n: Int, wait: Long)(doRegistration: => T): Try[T] =
    Try {

      doRegistration

    } match {

      case Failure(e) if n > 1 =>
        Log.warning(s"Attempt to register $name service failed (${e.getMessage}), retrying ${n-1} more times in $wait seconds.")
        Log.log(FINE, s"Google Services registration failure", e)

        Thread.sleep(wait * 1000)

        // wait for double the time before next attempt, but don't wait for more than 3 minutes
        retry(name, n - 1, if (wait*2 < 180) wait * 2 else 180)(doRegistration)

      case otherResult => otherResult
    }
}
