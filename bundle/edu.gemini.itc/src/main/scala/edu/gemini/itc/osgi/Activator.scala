package edu.gemini.itc.osgi

import java.util.logging.Level._
import java.util.logging.Logger

import edu.gemini.itc.osgi.Activator._
import edu.gemini.itc.service.{ItcService, ItcServiceImpl}
import org.osgi.framework.{BundleActivator, BundleContext, ServiceRegistration}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object Activator {
  val Log = Logger.getLogger(classOf[Activator].getName)
}

/**
 * Activator that registers and un-registers the services provided by this bundle.
 */
class Activator extends BundleActivator {

  var itcService: Option[ServiceRegistration[ItcService]] = None

  def start(ctx: BundleContext): Unit = {

    Log.info(s"Starting itc services bundle")
    
    // register the services..
    Future {

      val properties = new java.util.Hashtable[String, Object]() {{ put("trpc", "")}}
      val service    = new ItcServiceImpl()
      ctx.registerService(classOf[ItcService], service, properties)

    } onComplete {
      case Success(registeredService) =>
        itcService = Some(registeredService)
        Log.info("Successfully started itc service.")

      case Failure(t) =>
        Log.log(SEVERE, "Registration of itc service failed.", t)
    }

  }

  def stop(ctx: BundleContext): Unit = {

    Log.info("Stopping itc services bundle.")

    // unregister the services..
    itcService.foreach(_.unregister())
    itcService = None

  }

}
